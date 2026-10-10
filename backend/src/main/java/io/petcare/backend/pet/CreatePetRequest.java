package io.petcare.backend.pet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePetRequest(
        @NotBlank(message = "name is required")
        @Size(max = 50, message = "name must not exceed 50 characters")
        String name,

        @NotNull(message = "type is required")
        PetType type,

        @Size(max = 80, message = "breed must not exceed 80 characters")
        String breed,

        PetGender gender,

        @PastOrPresent(message = "birthday cannot be in the future")
        LocalDate birthday,

        @DecimalMin(value = "0.001", message = "weight must be at least 0.001 kg")
        @DecimalMax(value = "500.000", message = "weight must not exceed 500 kg")
        @Digits(integer = 3, fraction = 3, message = "weight supports up to 3 decimal places")
        BigDecimal weight,

        Boolean neutered,

        LocalDate homeDate,

        @Size(max = 500, message = "remark must not exceed 500 characters")
        String remark) {

    public CreatePetRequest {
        if (name != null) {
            name = name.trim();
        }
        if (breed != null) {
            breed = breed.trim();
        }
        if (remark != null) {
            remark = remark.trim();
        }
    }
}
