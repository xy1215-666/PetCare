package io.petcare.backend.expense;

import java.math.BigDecimal;

public class ExpenseMonthTotal {
    private String month;
    private BigDecimal amount;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
