package io.petcare.backend.health;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthRecordVO(
        long id,
        long petId,
        LocalDate recordDate,
        AppetiteLevel appetite,
        WaterIntake waterIntake,
        EnergyLevel energyLevel,
        ActivityLevel activityLevel,
        SleepStatus sleepStatus,
        StoolStatus stoolStatus,
        UrinationStatus urinationStatus,
        boolean abnormal,
        String abnormalDescription,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
