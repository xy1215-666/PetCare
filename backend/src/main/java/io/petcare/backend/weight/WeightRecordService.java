package io.petcare.backend.weight;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.common.exception.BadRequestException;
import io.petcare.backend.common.exception.NotFoundException;
import io.petcare.backend.pet.Pet;
import io.petcare.backend.pet.PetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class WeightRecordService {

    private final WeightRecordMapper weightRecordMapper;
    private final PetService petService;
    private final Clock clock;

    public WeightRecordService(WeightRecordMapper weightRecordMapper, PetService petService, Clock clock) {
        this.weightRecordMapper = weightRecordMapper;
        this.petService = petService;
        this.clock = clock;
    }

    @Transactional
    public WeightRecordVO create(long currentUserId, long petId, WeightRecordRequest request) {
        petService.requireOwnedPetForUpdate(currentUserId, petId);
        validateRequest(request);

        WeightRecord record = new WeightRecord();
        record.setPetId(petId);
        record.setWeight(request.weight());
        record.setRecordDate(request.recordDate());
        record.setRemark(emptyToNull(request.remark()));
        record.setDeleted(0);
        weightRecordMapper.insert(record);
        synchronizeCurrentWeight(currentUserId, petId);
        return toView(weightRecordMapper.selectById(record.getId()));
    }

    @Transactional(readOnly = true)
    public List<WeightRecordVO> list(long currentUserId, long petId) {
        petService.requireOwnedPet(currentUserId, petId);
        return weightRecordMapper.selectList(new QueryWrapper<WeightRecord>()
                        .eq("pet_id", petId)
                        .eq("deleted", 0)
                        .orderByAsc("record_date")
                        .orderByAsc("id"))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public WeightRecordVO update(long currentUserId, long petId, long recordId, WeightRecordRequest request) {
        petService.requireOwnedPetForUpdate(currentUserId, petId);
        findRecord(petId, recordId);
        validateRequest(request);

        UpdateWrapper<WeightRecord> update = new UpdateWrapper<WeightRecord>()
                .eq("id", recordId)
                .eq("pet_id", petId)
                .eq("deleted", 0)
                .set("weight", request.weight())
                .set("record_date", request.recordDate())
                .set("remark", emptyToNull(request.remark()));
        if (weightRecordMapper.update(null, update) != 1) {
            throw new NotFoundException("weight record was not found");
        }
        synchronizeCurrentWeight(currentUserId, petId);
        return toView(findRecord(petId, recordId));
    }

    @Transactional
    public void delete(long currentUserId, long petId, long recordId) {
        petService.requireOwnedPetForUpdate(currentUserId, petId);
        findRecord(petId, recordId);
        int updated = weightRecordMapper.update(null, new UpdateWrapper<WeightRecord>()
                .eq("id", recordId)
                .eq("pet_id", petId)
                .eq("deleted", 0)
                .set("deleted", 1));
        if (updated != 1) {
            throw new NotFoundException("weight record was not found");
        }
        synchronizeCurrentWeight(currentUserId, petId);
    }

    private void validateRequest(WeightRecordRequest request) {
        if (BusinessDate.isFuture(request.recordDate(), clock)) {
            throw new BadRequestException("recordDate cannot be in the future");
        }
    }

    private void synchronizeCurrentWeight(long currentUserId, long petId) {
        WeightRecord latest = weightRecordMapper.selectOne(new QueryWrapper<WeightRecord>()
                .eq("pet_id", petId)
                .eq("deleted", 0)
                .orderByDesc("record_date")
                .orderByDesc("update_time")
                .orderByDesc("id")
                .last("LIMIT 1"));
        petService.updateCurrentWeight(currentUserId, petId, latest == null ? null : latest.getWeight());
    }

    private WeightRecord findRecord(long petId, long recordId) {
        WeightRecord record = weightRecordMapper.selectOne(new QueryWrapper<WeightRecord>()
                .eq("id", recordId)
                .eq("pet_id", petId)
                .eq("deleted", 0));
        if (record == null) {
            throw new NotFoundException("weight record was not found");
        }
        return record;
    }

    private WeightRecordVO toView(WeightRecord record) {
        return new WeightRecordVO(record.getId(), record.getPetId(), record.getWeight(), record.getRecordDate(),
                record.getRemark(), record.getCreateTime(), record.getUpdateTime());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
