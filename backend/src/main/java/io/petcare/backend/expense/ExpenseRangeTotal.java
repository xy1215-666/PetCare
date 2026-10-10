package io.petcare.backend.expense;

import java.math.BigDecimal;

public class ExpenseRangeTotal {
    private BigDecimal totalAmount;
    private long recordCount;

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public long getRecordCount() { return recordCount; }
    public void setRecordCount(long recordCount) { this.recordCount = recordCount; }
}
