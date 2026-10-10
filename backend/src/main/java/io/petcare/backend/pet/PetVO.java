package io.petcare.backend.pet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetVO(
        long id,
        String name,
        PetType type,
        String breed,
        PetGender gender,
        LocalDate birthday,
        BigDecimal weight,
        Boolean neutered,
        LocalDate homeDate,
        String remark,
        LocalDateTime createTime) {
}
