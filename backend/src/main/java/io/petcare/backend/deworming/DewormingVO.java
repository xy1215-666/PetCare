package io.petcare.backend.deworming;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DewormingVO(
        Long id,
        Long petId,
        DewormingType type,
        String medicineName,
        LocalDate dewormingDate,
        LocalDate nextDate,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
