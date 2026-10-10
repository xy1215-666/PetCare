package io.petcare.backend.vaccination;

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
public class VaccinationService {

    private static final long MAX_FUTURE_RECORD_DAYS = 365;

    private final VaccinationMapper vaccinationMapper;
    private final PetService petService;
    private final Clock clock;

    public VaccinationService(VaccinationMapper vaccinationMapper, PetService petService, Clock clock) {
        this.vaccinationMapper = vaccinationMapper;
        this.petService = petService;
        this.clock = clock;
    }

    @Transactional
    public VaccinationVO create(long userId, long petId, VaccinationRequest request) {
        petService.requireOwnedPet(userId, petId);
        validate(request);
        VaccinationRecord record = new VaccinationRecord();
        record.setPetId(petId);
        apply(record, request);
        record.setDeleted(0);
        vaccinationMapper.insert(record);
        return toView(vaccinationMapper.selectById(record.getId()));
    }

    @Transactional(readOnly = true)
    public List<VaccinationVO> list(long userId, long petId) {
        petService.requireOwnedPet(userId, petId);
        return vaccinationMapper.selectList(new QueryWrapper<VaccinationRecord>()
                        .eq("pet_id", petId)
                        .eq("deleted", 0)
                        .orderByDesc("vaccination_date")
                        .orderByDesc("id"))
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public VaccinationVO get(long userId, long petId, long recordId) {
        petService.requireOwnedPet(userId, petId);
        return toView(findRecord(petId, recordId));
    }

    @Transactional
    public VaccinationVO update(long userId, long petId, long recordId, VaccinationRequest request) {
        petService.requireOwnedPet(userId, petId);
        validate(request);
        VaccinationRecord record = findRecord(petId, recordId);
        apply(record, request);
        vaccinationMapper.updateById(record);
        return toView(vaccinationMapper.selectById(recordId));
    }

    @Transactional
    public void delete(long userId, long petId, long recordId) {
        petService.requireOwnedPet(userId, petId);
        findRecord(petId, recordId);
        vaccinationMapper.update(null, new UpdateWrapper<VaccinationRecord>()
                .eq("id", recordId).eq("pet_id", petId).eq("deleted", 0).set("deleted", 1));
    }

    private void validate(VaccinationRequest request) {
        LocalDate today = BusinessDate.today(clock);
        if (request.vaccinationDate().isAfter(today.plusDays(MAX_FUTURE_RECORD_DAYS))) {
            throw new BadRequestException("vaccinationDate is too far in the future");
        }
        if (request.nextDate() != null && request.nextDate().isBefore(request.vaccinationDate())) {
            throw new BadRequestException("nextDate cannot be before vaccinationDate");
        }
    }

    private VaccinationRecord findRecord(long petId, long recordId) {
        VaccinationRecord record = vaccinationMapper.selectOne(new QueryWrapper<VaccinationRecord>()
                .eq("id", recordId).eq("pet_id", petId).eq("deleted", 0));
        if (record == null) {
            throw new NotFoundException("vaccination record was not found");
        }
        return record;
    }

    private void apply(VaccinationRecord record, VaccinationRequest request) {
        record.setVaccineName(request.vaccineName());
        record.setVaccinationDate(request.vaccinationDate());
        record.setNextDate(request.nextDate());
        record.setHospital(request.hospital());
        record.setBatchNumber(request.batchNumber());
        record.setRemark(request.remark());
    }

    private VaccinationVO toView(VaccinationRecord record) {
        return new VaccinationVO(record.getId(), record.getPetId(), record.getVaccineName(),
                record.getVaccinationDate(), record.getNextDate(), record.getHospital(), record.getBatchNumber(),
                record.getRemark(), record.getCreateTime(), record.getUpdateTime());
    }
}
