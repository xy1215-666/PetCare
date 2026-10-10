package io.petcare.backend.vaccination;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VaccinationRequest(
        @NotBlank(message = "vaccineName is required")
        @Size(max = 120, message = "vaccineName must not exceed 120 characters")
        String vaccineName,
        @NotNull(message = "vaccinationDate is required")
        LocalDate vaccinationDate,
        LocalDate nextDate,
        @Size(max = 160, message = "hospital must not exceed 160 characters")
        String hospital,
        @Size(max = 100, message = "batchNumber must not exceed 100 characters")
        String batchNumber,
        @Size(max = 500, message = "remark must not exceed 500 characters")
        String remark) {

    public VaccinationRequest {
        vaccineName = trim(vaccineName);
        hospital = trimToNull(hospital);
        batchNumber = trimToNull(batchNumber);
        remark = trimToNull(remark);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
