package io.petcare.backend.health;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HealthRecordRequest(
        @NotNull(message = "recordDate is required")
        LocalDate recordDate,

        @NotNull(message = "appetite is required")
        AppetiteLevel appetite,

        @NotNull(message = "waterIntake is required")
        WaterIntake waterIntake,

        @NotNull(message = "energyLevel is required")
        EnergyLevel energyLevel,

        @NotNull(message = "activityLevel is required")
        ActivityLevel activityLevel,

        @NotNull(message = "sleepStatus is required")
        SleepStatus sleepStatus,

        @NotNull(message = "stoolStatus is required")
        StoolStatus stoolStatus,

        @NotNull(message = "urinationStatus is required")
        UrinationStatus urinationStatus,

        Boolean abnormal,

        @Size(max = 500, message = "abnormalDescription must not exceed 500 characters")
        String abnormalDescription,

        @Size(max = 500, message = "remark must not exceed 500 characters")
        String remark) {

    public HealthRecordRequest {
        if (abnormalDescription != null) {
            abnormalDescription = abnormalDescription.trim();
        }
        if (remark != null) {
            remark = remark.trim();
        }
    }
}
