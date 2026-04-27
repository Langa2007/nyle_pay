package com.nyle.nylepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for local Kenyan payments: Till, Paybill, Pochi la Biashara, Send Money.
 *
 * paymentType values:
 *   TILL     — Buy Goods (Safaricom B2B)
 *   PAYBILL  — Pay Bill (Safaricom B2B)
 *   POCHI    — Pochi la Biashara (B2B to Pochi shortcode)
 *   SEND     — Send Money to M-Pesa (B2C)
 */
public class LocalPaymentRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "paymentType is required (TILL, PAYBILL, POCHI, SEND)")
    private String paymentType;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "1", message = "Amount must be at least KES 1")
    private BigDecimal amount;

    /** Till number for BuyGoods, or shortcode for Paybill/Pochi */
    private String tillNumber;

    /** Paybill number (shortcode) */
    private String paybillNumber;

    /** Account number for Paybill payments */
    private String accountNumber;

    /** Recipient M-Pesa phone number for SEND payments */
    private String recipientPhone;

    /** Optional description / narration */
    private String description;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTillNumber() { return tillNumber; }
    public void setTillNumber(String tillNumber) { this.tillNumber = tillNumber; }

    public String getPaybillNumber() { return paybillNumber; }
    public void setPaybillNumber(String paybillNumber) { this.paybillNumber = paybillNumber; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
