package io.petcare.backend.vaccination;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccinationVO(
        Long id,
        Long petId,
        String vaccineName,
        LocalDate vaccinationDate,
        LocalDate nextDate,
        String hospital,
        String batchNumber,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
