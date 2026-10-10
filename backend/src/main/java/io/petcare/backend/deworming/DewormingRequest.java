package io.petcare.backend.deworming;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DewormingRequest(
        @NotNull(message = "type is required")
        DewormingType type,
        @Size(max = 160, message = "medicineName must not exceed 160 characters")
        String medicineName,
        @NotNull(message = "dewormingDate is required")
        LocalDate dewormingDate,
        LocalDate nextDate,
        @Size(max = 500, message = "remark must not exceed 500 characters")
        String remark) {

    public DewormingRequest {
        medicineName = trimToNull(medicineName);
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
