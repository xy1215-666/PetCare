package io.petcare.backend.expense;

import java.math.BigDecimal;

public record ExpenseSummaryVO(
        String month,
        BigDecimal totalAmount,
        long recordCount,
        BigDecimal previousMonthAmount,
        BigDecimal changeRate) {
}
