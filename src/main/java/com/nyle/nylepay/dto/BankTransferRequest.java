package com.nyle.nylepay.dto;

import java.math.BigDecimal;

public class BankTransferRequest {
    private Long userId;
    private String bankDetailId; // Optional if using saved detail
    private BigDecimal amount;
    private String currency; // "KSH"
    private String narration;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBankDetailId() { return bankDetailId; }
    public void setBankDetailId(String bankDetailId) { this.bankDetailId = bankDetailId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
}
