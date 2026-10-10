package io.petcare.backend.expense;

import java.math.BigDecimal;

public record ExpenseCategoryVO(String category, BigDecimal amount) {
}
