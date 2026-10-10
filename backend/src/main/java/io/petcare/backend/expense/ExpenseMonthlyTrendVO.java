package io.petcare.backend.expense;

import java.math.BigDecimal;

public record ExpenseMonthlyTrendVO(String month, BigDecimal amount) {
}
