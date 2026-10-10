package io.petcare.backend.medical;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicalRecordRequest(
        @NotNull(message = "visitDate is required")
        LocalDate visitDate,
        @Size(max = 160, message = "hospital must not exceed 160 characters")
        String hospital,
        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason,
        @Size(max = 2000, message = "examination must not exceed 2000 characters")
        String examination,
        @Size(max = 2000, message = "result must not exceed 2000 characters")
        String result,
        @Size(max = 1000, message = "medicine must not exceed 1000 characters")
        String medicine,
        @Size(max = 2000, message = "doctorAdvice must not exceed 2000 characters")
        String doctorAdvice,
        @NotNull(message = "cost is required")
        @DecimalMin(value = "0.00", message = "cost cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "cost supports up to 2 decimal places")
        BigDecimal cost,
        LocalDate nextVisitDate,
        @Size(max = 500, message = "remark must not exceed 500 characters")
        String remark) {

    public MedicalRecordRequest {
        hospital = trimToNull(hospital);
        reason = reason == null ? null : reason.trim();
        examination = trimToNull(examination);
        result = trimToNull(result);
        medicine = trimToNull(medicine);
        doctorAdvice = trimToNull(doctorAdvice);
        remark = trimToNull(remark);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
