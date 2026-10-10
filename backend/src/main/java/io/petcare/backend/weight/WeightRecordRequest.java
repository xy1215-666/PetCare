package io.petcare.backend.weight;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightRecordRequest(
        @NotNull(message = "weight is required")
        @DecimalMin(value = "0.001", message = "weight must be at least 0.001 kg")
        @DecimalMax(value = "500.000", message = "weight must not exceed 500 kg")
        @Digits(integer = 3, fraction = 3, message = "weight supports up to 3 decimal places")
        BigDecimal weight,

        @NotNull(message = "recordDate is required")
        LocalDate recordDate,

        @Size(max = 500, message = "remark must not exceed 500 characters")
        String remark) {

    public WeightRecordRequest {
        if (remark != null) {
            remark = remark.trim();
        }
    }
}
