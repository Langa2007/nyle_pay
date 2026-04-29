package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A NylePay merchant account.
 *
 * Merchants use NylePay as a gateway to accept payments from their customers
 * via card (Visa/Mastercard), M-Pesa, bank, or crypto.
 *
 * Security:
 *   - secretKey is AES-256-GCM encrypted at rest (same pattern as CEX keys).
 *   - publicKey is safe to expose on the merchant's frontend.
 *   - webhookSecret is used to sign outbound webhook deliveries (HMAC-SHA256).
 */
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NylePay user who owns this merchant account */
    private Long userId;

    private String businessName;
    private String businessEmail;

    /**
     * URL NylePay delivers payment events to.
     * Must be HTTPS in production.
     */
    private String webhookUrl;

    /** Secret used to HMAC-sign outbound webhook payloads (stored in plaintext — only we see it) */
    private String webhookSecret;

    /** Safe to expose on merchant's checkout page */
    @Column(unique = true)
    private String publicKey;

    /** AES-256-GCM encrypted secret key — never returned in API responses */
    private String encryptedSecretKey;

    /** PENDING | ACTIVE | SUSPENDED */
    private String status = "PENDING";

    /** NONE | PENDING | VERIFIED | REJECTED */
    private String kycStatus = "NONE";

    /**
     * Linked bank/M-Pesa account for settlement payouts.
     * References UserBankDetail.id or a special MPESA settlement record.
     */
    private Long settlementAccountId;

    /** Settlement currency preference */
    private String settlementCurrency = "KES";

    /** NylePay fee percentage charged on each transaction */
    private java.math.BigDecimal feePercent = new java.math.BigDecimal("1.5");

    /** Accumulated balance awaiting daily settlement payout to merchant's bank/M-Pesa */
    @Column(columnDefinition = "numeric(19,4) default 0")
    private java.math.BigDecimal pendingSettlement = java.math.BigDecimal.ZERO;

    /** M-Pesa number or bank account reference for payout */
    private String settlementPhone;
    private String settlementBankAccount;
    private String settlementBankName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ──────────────────────────────────────────────────
    public Long getId()                                  { return id; }
    public void setId(Long id)                           { this.id = id; }
    public Long getUserId()                              { return userId; }
    public void setUserId(Long userId)                   { this.userId = userId; }
    public String getBusinessName()                          { return businessName; }
    public void setBusinessName(String businessName)         { this.businessName = businessName; }
    public String getBusinessEmail()                         { return businessEmail; }
    public void setBusinessEmail(String businessEmail)       { this.businessEmail = businessEmail; }
    public String getWebhookUrl()                            { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl)             { this.webhookUrl = webhookUrl; }
    public String getWebhookSecret()                         { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret)       { this.webhookSecret = webhookSecret; }
    public String getPublicKey()                             { return publicKey; }
    public void setPublicKey(String publicKey)               { this.publicKey = publicKey; }
    public String getEncryptedSecretKey()                              { return encryptedSecretKey; }
    public void setEncryptedSecretKey(String encryptedSecretKey)       { this.encryptedSecretKey = encryptedSecretKey; }
    public String getStatus()                                { return status; }
    public void setStatus(String status)                     { this.status = status; }
    public String getKycStatus()                             { return kycStatus; }
    public void setKycStatus(String kycStatus)               { this.kycStatus = kycStatus; }
    public Long getSettlementAccountId()                               { return settlementAccountId; }
    public void setSettlementAccountId(Long settlementAccountId)       { this.settlementAccountId = settlementAccountId; }
    public String getSettlementCurrency()                              { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency)       { this.settlementCurrency = settlementCurrency; }
    public java.math.BigDecimal getFeePercent()                        { return feePercent; }
    public void setFeePercent(java.math.BigDecimal feePercent)         { this.feePercent = feePercent; }
    public java.math.BigDecimal getPendingSettlement()                              { return pendingSettlement; }
    public void setPendingSettlement(java.math.BigDecimal pendingSettlement)        { this.pendingSettlement = pendingSettlement; }
    public String getSettlementPhone()                                 { return settlementPhone; }
    public void setSettlementPhone(String settlementPhone)             { this.settlementPhone = settlementPhone; }
    public String getSettlementBankAccount()                                   { return settlementBankAccount; }
    public void setSettlementBankAccount(String settlementBankAccount)         { this.settlementBankAccount = settlementBankAccount; }
    public String getSettlementBankName()                              { return settlementBankName; }
    public void setSettlementBankName(String settlementBankName)       { this.settlementBankName = settlementBankName; }
    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)        { this.createdAt = createdAt; }
}
