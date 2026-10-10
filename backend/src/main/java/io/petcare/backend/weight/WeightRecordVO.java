package io.petcare.backend.weight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeightRecordVO(
        long id,
        long petId,
        BigDecimal weight,
        LocalDate recordDate,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
