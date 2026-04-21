// DepositRequest.java
package com.nyle.nylepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


import java.math.BigDecimal;

public class DepositRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10", message = "Minimum deposit is 10")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency; // "KSH", "USD"
    
    @Pattern(regexp = "^2547[0-9]{8}$", message = "Invalid MPesa number format")
    private String mpesaNumber;
    
    private String method; // "MPESA", "BANK"
    private String bankReference; // For bank deposits

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMpesaNumber() { return mpesaNumber; }
    public void setMpesaNumber(String mpesaNumber) { this.mpesaNumber = mpesaNumber; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
}
