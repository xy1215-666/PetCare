package io.petcare.backend.expense;

import java.util.List;

public record ExpensePageVO(List<ExpenseView> records, long total, int page, int size, long totalPages) {
}
