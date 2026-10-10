package io.petcare.backend.health;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.common.exception.BadRequestException;
import io.petcare.backend.common.exception.ConflictException;
import io.petcare.backend.common.exception.NotFoundException;
import io.petcare.backend.pet.PetService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;
    private final PetService petService;
    private final Clock clock;

    public HealthRecordService(HealthRecordMapper healthRecordMapper, PetService petService, Clock clock) {
        this.healthRecordMapper = healthRecordMapper;
        this.petService = petService;
        this.clock = clock;
    }

    @Transactional
    public HealthRecordVO create(long currentUserId, long petId, HealthRecordRequest request) {
        petService.requireOwnedPet(currentUserId, petId);
        validateRequest(request);
        ensureDateAvailable(petId, request.recordDate(), null);

        HealthRecord record = new HealthRecord();
        record.setPetId(petId);
        apply(record, request);
        record.setDeleted(0);
        try {
            healthRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            throw duplicateDateConflict();
        }
        return toView(healthRecordMapper.selectById(record.getId()));
    }

    @Transactional(readOnly = true)
    public List<HealthRecordVO> list(long currentUserId, long petId) {
        petService.requireOwnedPet(currentUserId, petId);
        return healthRecordMapper.selectList(new QueryWrapper<HealthRecord>()
                        .eq("pet_id", petId)
                        .eq("deleted", 0)
                        .orderByDesc("record_date")
                        .orderByDesc("id"))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public HealthRecordVO get(long currentUserId, long petId, long recordId) {
        petService.requireOwnedPet(currentUserId, petId);
        return toView(findRecord(petId, recordId));
    }

    @Transactional
    public HealthRecordVO update(long currentUserId, long petId, long recordId, HealthRecordRequest request) {
        petService.requireOwnedPet(currentUserId, petId);
        HealthRecord current = findRecord(petId, recordId);
        validateRequest(request);
        ensureDateAvailable(petId, request.recordDate(), recordId);

        UpdateWrapper<HealthRecord> update = new UpdateWrapper<HealthRecord>()
                .eq("id", recordId)
                .eq("pet_id", petId)
                .eq("deleted", 0)
                .set("record_date", request.recordDate())
                .set("appetite", request.appetite())
                .set("water_intake", request.waterIntake())
                .set("energy_level", request.energyLevel())
                .set("activity_level", request.activityLevel())
                .set("sleep_status", request.sleepStatus())
                .set("stool_status", request.stoolStatus())
                .set("urination_status", request.urinationStatus())
                .set("abnormal", Boolean.TRUE.equals(request.abnormal()))
                .set("abnormal_description", emptyToNull(request.abnormalDescription()))
                .set("remark", emptyToNull(request.remark()));
        try {
            if (healthRecordMapper.update(null, update) != 1) {
                throw new NotFoundException("health record was not found");
            }
        } catch (DuplicateKeyException exception) {
            throw duplicateDateConflict();
        }
        return toView(findRecord(petId, current.getId()));
    }

    @Transactional
    public void delete(long currentUserId, long petId, long recordId) {
        petService.requireOwnedPet(currentUserId, petId);
        findRecord(petId, recordId);
        int updated = healthRecordMapper.update(null, new UpdateWrapper<HealthRecord>()
                .eq("id", recordId)
                .eq("pet_id", petId)
                .eq("deleted", 0)
                .set("deleted", 1));
        if (updated != 1) {
            throw new NotFoundException("health record was not found");
        }
    }

    private void validateRequest(HealthRecordRequest request) {
        if (BusinessDate.isFuture(request.recordDate(), clock)) {
            throw new BadRequestException("recordDate cannot be in the future");
        }
        if (Boolean.TRUE.equals(request.abnormal())
                && (request.abnormalDescription() == null || request.abnormalDescription().isBlank())) {
            throw new BadRequestException("abnormalDescription is required when abnormal is true");
        }
    }

    private void ensureDateAvailable(long petId, java.time.LocalDate date, Long exceptRecordId) {
        QueryWrapper<HealthRecord> query = new QueryWrapper<HealthRecord>()
                .eq("pet_id", petId)
                .eq("record_date", date)
                .eq("deleted", 0);
        if (exceptRecordId != null) {
            query.ne("id", exceptRecordId);
        }
        if (healthRecordMapper.selectCount(query) > 0) {
            throw duplicateDateConflict();
        }
    }

    private ConflictException duplicateDateConflict() {
        return new ConflictException("a health record already exists for this pet on this date");
    }

    private HealthRecord findRecord(long petId, long recordId) {
        HealthRecord record = healthRecordMapper.selectOne(new QueryWrapper<HealthRecord>()
                .eq("id", recordId)
                .eq("pet_id", petId)
                .eq("deleted", 0));
        if (record == null) {
            throw new NotFoundException("health record was not found");
        }
        return record;
    }

    private void apply(HealthRecord record, HealthRecordRequest request) {
        record.setRecordDate(request.recordDate());
        record.setAppetite(request.appetite());
        record.setWaterIntake(request.waterIntake());
        record.setEnergyLevel(request.energyLevel());
        record.setActivityLevel(request.activityLevel());
        record.setSleepStatus(request.sleepStatus());
        record.setStoolStatus(request.stoolStatus());
        record.setUrinationStatus(request.urinationStatus());
        record.setAbnormal(Boolean.TRUE.equals(request.abnormal()));
        record.setAbnormalDescription(emptyToNull(request.abnormalDescription()));
        record.setRemark(emptyToNull(request.remark()));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private HealthRecordVO toView(HealthRecord record) {
        return new HealthRecordVO(
                record.getId(),
                record.getPetId(),
                record.getRecordDate(),
                record.getAppetite(),
                record.getWaterIntake(),
                record.getEnergyLevel(),
                record.getActivityLevel(),
                record.getSleepStatus(),
                record.getStoolStatus(),
                record.getUrinationStatus(),
                Boolean.TRUE.equals(record.getAbnormal()),
                record.getAbnormalDescription(),
                record.getRemark(),
                record.getCreateTime(),
                record.getUpdateTime());
    }
}
