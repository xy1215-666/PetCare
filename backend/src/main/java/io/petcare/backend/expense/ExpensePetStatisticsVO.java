package io.petcare.backend.expense;

import java.math.BigDecimal;

public record ExpensePetStatisticsVO(Long petId, String petName, BigDecimal amount) {
}
