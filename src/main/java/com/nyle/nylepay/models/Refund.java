package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a refund operation against a completed transaction or checkout session.
 *
 * ACID:
 *   Refund is processed inside a single @Transactional method that:
 *   1. Debits the merchant's settlement wallet balance
 *   2. Credits the customer's NylePay wallet (if they are a NylePay user)
 *   3. Calls the provider refund API
 *   4. Saves this record
 *   Any failure rolls back all four steps atomically.
 */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The NylePay Transaction that is being refunded */
    private Long transactionId;

    /** The CheckoutSession that is being refunded (may be null for direct charges) */
    private Long checkoutSessionId;

    private Long merchantId;

    /** Customer userId — null if customer is not a NylePay user */
    private Long customerId;

    private BigDecimal amount;
    private String currency;

    /** DUPLICATE | FRAUDULENT | CUSTOMER_REQUEST | OTHER */
    private String reason;

    /** PENDING | SUCCEEDED | FAILED */
    private String status = "PENDING";

    /** Provider's refund ID for correlation */
    private String providerRefundId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    public Long getTransactionId()         { return transactionId; }
    public void setTransactionId(Long tid) { this.transactionId = tid; }
    public Long getCheckoutSessionId()                    { return checkoutSessionId; }
    public void setCheckoutSessionId(Long csid)           { this.checkoutSessionId = csid; }
    public Long getMerchantId()            { return merchantId; }
    public void setMerchantId(Long mid)    { this.merchantId = mid; }
    public Long getCustomerId()            { return customerId; }
    public void setCustomerId(Long cid)    { this.customerId = cid; }
    public BigDecimal getAmount()              { return amount; }
    public void setAmount(BigDecimal amount)   { this.amount = amount; }
    public String getCurrency()                { return currency; }
    public void setCurrency(String currency)   { this.currency = currency; }
    public String getReason()                  { return reason; }
    public void setReason(String reason)       { this.reason = reason; }
    public String getStatus()                  { return status; }
    public void setStatus(String status)       { this.status = status; }
    public String getProviderRefundId()                      { return providerRefundId; }
    public void setProviderRefundId(String providerRefundId) { this.providerRefundId = providerRefundId; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime dt) { this.createdAt = dt; }
}
