package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores a tokenized card reference — the raw PAN (card number) is NEVER stored here.
 *
 * PCI DSS SAQ-A compliance:
 *   Card capture happens entirely in the browser via Paystack.js / Stripe.js.
 *   NylePay only ever receives an opaque token from the provider.
 *   Storing only {provider, token, last4, brand} keeps NylePay in SAQ-A scope.
 */
@Entity
@Table(name = "saved_cards", uniqueConstraints = {
    @UniqueConstraint(name = "uq_saved_card_fingerprint", columnNames = {"userId", "fingerprint", "provider"})
})
public class SavedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    /** PAYSTACK | STRIPE | FLUTTERWAVE */
    private String provider;

    /** Provider's opaque token — never the raw PAN */
    private String tokenId;

    /** Last 4 digits of card — safe to store, not sensitive */
    private String last4;

    /** VISA | MASTERCARD | VERVE | AMEX */
    private String brand;

    private Integer expiryMonth;
    private Integer expiryYear;

    /**
     * Provider-level deduplication fingerprint.
     * Same physical card → same fingerprint across all users (used to detect fraud rings).
     */
    private String fingerprint;

    private boolean isDefault = false;
    private boolean isActive  = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ──────────────────────────────────────────────────
    public Long getId()                { return id; }
    public void setId(Long id)         { this.id = id; }
    public Long getUserId()            { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProvider()              { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTokenId()               { return tokenId; }
    public void setTokenId(String tokenId)   { this.tokenId = tokenId; }
    public String getLast4()                 { return last4; }
    public void setLast4(String last4)       { this.last4 = last4; }
    public String getBrand()                 { return brand; }
    public void setBrand(String brand)       { this.brand = brand; }
    public Integer getExpiryMonth()                { return expiryMonth; }
    public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
    public Integer getExpiryYear()               { return expiryYear; }
    public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
    public String getFingerprint()                   { return fingerprint; }
    public void setFingerprint(String fingerprint)   { this.fingerprint = fingerprint; }
    public boolean isDefault()                       { return isDefault; }
    public void setDefault(boolean isDefault)        { this.isDefault = isDefault; }
    public boolean isActive()                        { return isActive; }
    public void setActive(boolean isActive)          { this.isActive = isActive; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
