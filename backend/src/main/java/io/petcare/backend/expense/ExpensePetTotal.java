package io.petcare.backend.expense;

import java.math.BigDecimal;

public class ExpensePetTotal {
    private Long petId;
    private String petName;
    private BigDecimal amount;

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
