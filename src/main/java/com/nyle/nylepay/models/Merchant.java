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
    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)        { this.createdAt = createdAt; }
}
