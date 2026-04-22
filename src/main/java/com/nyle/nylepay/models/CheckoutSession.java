package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A merchant-initiated payment session (checkout).
 *
 * Flow:
 *   1. Merchant calls POST /api/merchant/payment-link → creates this record (PENDING)
 *   2. Customer follows the link and pays (card / M-Pesa / crypto)
 *   3. NylePay receives provider webhook → sets status COMPLETED, credits merchant wallet
 *   4. NylePay delivers webhook to merchant's webhookUrl
 *
 * The word "CheckoutSession" is used instead of "PaymentIntent" to avoid conflict
 * with com.stripe.model.PaymentIntent which is imported in the card services.
 */
@Entity
@Table(name = "checkout_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_checkout_reference", columnNames = {"reference"})
})
public class CheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long merchantId;

    /** Customer's NylePay userId — null if customer is not a registered user */
    private Long customerId;

    private BigDecimal amount;
    private String currency;
    private String description;

    /** PAYSTACK | STRIPE | FLUTTERWAVE | MPESA | CRYPTO */
    private String provider;

    /** Provider's payment intent / charge ID */
    private String providerIntentId;

    /** NylePay internal reference — matches provider's tx_ref / metadata.reference */
    @Column(unique = true)
    private String reference;

    /**
     * PENDING     — awaiting customer payment
     * COMPLETED   — payment confirmed, merchant credited
     * FAILED      — payment failed
     * EXPIRED     — session expired without payment
     * REFUNDED    — full refund issued
     * PARTIAL_REF — partial refund issued
     */
    private String status = "PENDING";

    /** URL customer is redirected to after payment */
    private String redirectUrl;

    /** URL NylePay posts events to (copied from merchant at session creation) */
    private String callbackUrl;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private LocalDateTime createdAt  = LocalDateTime.now();
    private LocalDateTime expiresAt;

    // ── Getters & Setters ──────────────────────────────────────────────────
    public Long getId()                   { return id; }
    public void setId(Long id)            { this.id = id; }
    public Long getMerchantId()           { return merchantId; }
    public void setMerchantId(Long mid)   { this.merchantId = mid; }
    public Long getCustomerId()           { return customerId; }
    public void setCustomerId(Long cid)   { this.customerId = cid; }
    public BigDecimal getAmount()             { return amount; }
    public void setAmount(BigDecimal amount)  { this.amount = amount; }
    public String getCurrency()               { return currency; }
    public void setCurrency(String currency)  { this.currency = currency; }
    public String getDescription()                { return description; }
    public void setDescription(String desc)       { this.description = desc; }
    public String getProvider()               { return provider; }
    public void setProvider(String provider)  { this.provider = provider; }
    public String getProviderIntentId()                       { return providerIntentId; }
    public void setProviderIntentId(String providerIntentId)  { this.providerIntentId = providerIntentId; }
    public String getReference()              { return reference; }
    public void setReference(String ref)      { this.reference = ref; }
    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }
    public String getRedirectUrl()                { return redirectUrl; }
    public void setRedirectUrl(String url)        { this.redirectUrl = url; }
    public String getCallbackUrl()                { return callbackUrl; }
    public void setCallbackUrl(String url)        { this.callbackUrl = url; }
    public String getMetadata()                   { return metadata; }
    public void setMetadata(String metadata)      { this.metadata = metadata; }
    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime dt)    { this.createdAt = dt; }
    public LocalDateTime getExpiresAt()           { return expiresAt; }
    public void setExpiresAt(LocalDateTime dt)    { this.expiresAt = dt; }
}
