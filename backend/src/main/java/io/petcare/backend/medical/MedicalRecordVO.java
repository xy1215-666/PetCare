package io.petcare.backend.medical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalRecordVO(
        Long id,
        Long petId,
        LocalDate visitDate,
        String hospital,
        String reason,
        String examination,
        String result,
        String medicine,
        String doctorAdvice,
        BigDecimal cost,
        LocalDate nextVisitDate,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
