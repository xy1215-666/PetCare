package io.petcare.backend.deworming;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.common.exception.BadRequestException;
import io.petcare.backend.common.exception.NotFoundException;
import io.petcare.backend.pet.PetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class DewormingService {

    private static final long MAX_FUTURE_RECORD_DAYS = 365;

    private final DewormingMapper dewormingMapper;
    private final PetService petService;
    private final Clock clock;

    public DewormingService(DewormingMapper dewormingMapper, PetService petService, Clock clock) {
        this.dewormingMapper = dewormingMapper;
        this.petService = petService;
        this.clock = clock;
    }

    @Transactional
    public DewormingVO create(long userId, long petId, DewormingRequest request) {
        petService.requireOwnedPet(userId, petId);
        validate(request);
        DewormingRecord record = new DewormingRecord();
        record.setPetId(petId);
        apply(record, request);
        record.setDeleted(0);
        dewormingMapper.insert(record);
        return toView(dewormingMapper.selectById(record.getId()));
    }

    @Transactional(readOnly = true)
    public List<DewormingVO> list(long userId, long petId) {
        petService.requireOwnedPet(userId, petId);
        return dewormingMapper.selectList(new QueryWrapper<DewormingRecord>()
                        .eq("pet_id", petId)
                        .eq("deleted", 0)
                        .orderByDesc("deworming_date")
                        .orderByDesc("id"))
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public DewormingVO get(long userId, long petId, long recordId) {
        petService.requireOwnedPet(userId, petId);
        return toView(findRecord(petId, recordId));
    }

    @Transactional
    public DewormingVO update(long userId, long petId, long recordId, DewormingRequest request) {
        petService.requireOwnedPet(userId, petId);
        validate(request);
        DewormingRecord record = findRecord(petId, recordId);
        apply(record, request);
        dewormingMapper.updateById(record);
        return toView(dewormingMapper.selectById(recordId));
    }

    @Transactional
    public void delete(long userId, long petId, long recordId) {
        petService.requireOwnedPet(userId, petId);
        findRecord(petId, recordId);
        dewormingMapper.update(null, new UpdateWrapper<DewormingRecord>()
                .eq("id", recordId).eq("pet_id", petId).eq("deleted", 0).set("deleted", 1));
    }

    private void validate(DewormingRequest request) {
        LocalDate today = BusinessDate.today(clock);
        if (request.dewormingDate().isAfter(today.plusDays(MAX_FUTURE_RECORD_DAYS))) {
            throw new BadRequestException("dewormingDate is too far in the future");
        }
        if (request.nextDate() != null && request.nextDate().isBefore(request.dewormingDate())) {
            throw new BadRequestException("nextDate cannot be before dewormingDate");
        }
    }

    private DewormingRecord findRecord(long petId, long recordId) {
        DewormingRecord record = dewormingMapper.selectOne(new QueryWrapper<DewormingRecord>()
                .eq("id", recordId).eq("pet_id", petId).eq("deleted", 0));
        if (record == null) {
            throw new NotFoundException("deworming record was not found");
        }
        return record;
    }

    private void apply(DewormingRecord record, DewormingRequest request) {
        record.setType(request.type().name());
        record.setMedicineName(request.medicineName());
        record.setDewormingDate(request.dewormingDate());
        record.setNextDate(request.nextDate());
        record.setRemark(request.remark());
    }

    private DewormingVO toView(DewormingRecord record) {
        return new DewormingVO(record.getId(), record.getPetId(), DewormingType.valueOf(record.getType()),
                record.getMedicineName(), record.getDewormingDate(), record.getNextDate(), record.getRemark(),
                record.getCreateTime(), record.getUpdateTime());
    }
}
