package io.petcare.backend.expense;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @Positive(message = "petId must be positive") Long petId,
        @NotNull(message = "category is required") ExpenseCategory category,
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @DecimalMax(value = "999999.99", message = "amount must not exceed 999999.99")
        @Digits(integer = 6, fraction = 2, message = "amount must have at most 2 decimal places") BigDecimal amount,
        @NotNull(message = "expenseDate is required") LocalDate expenseDate,
        @Size(max = 160, message = "merchant must be at most 160 characters") String merchant,
        @Size(max = 500, message = "description must be at most 500 characters") String description,
        @Size(max = 500, message = "remark must be at most 500 characters") String remark) {
}
