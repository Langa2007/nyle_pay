package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit trail for all security-sensitive operations.
 *
 * Every entry captures WHO did WHAT, WHEN, from WHERE (IP), and the OUTCOME.
 * This table is append-only — rows are never updated or deleted.
 * CBK AML/CFT regulation requires financial institutions to maintain
 * audit logs for a minimum of 7 years.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "userId"),
        @Index(name = "idx_audit_event_type", columnList = "eventType"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_ip", columnList = "ipAddress")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who performed the action (null for system events) */
    private Long userId;

    /**
     * Event type — standardized category:
     * AUTH_LOGIN_SUCCESS, AUTH_LOGIN_FAILED, AUTH_LOCKOUT,
     * AUTH_PASSWORD_RESET, AUTH_OTP_REQUEST, AUTH_OTP_VERIFY,
     * PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED,
     * WITHDRAWAL_INITIATED, WITHDRAWAL_COMPLETED,
     * TRANSFER_SENT, TRANSFER_RECEIVED,
     * KYC_SUBMITTED, KYC_VERIFIED, KYC_REJECTED,
     * PROFILE_UPDATED, ADMIN_ACTION,
     * FRAUD_ALERT, FRAUD_BLOCKED,
     * ACCOUNT_LOCKED, ACCOUNT_UNLOCKED
     */
    @Column(nullable = false, length = 64)
    private String eventType;

    /** Human-readable description of the event */
    @Column(length = 1024)
    private String description;

    /** IP address of the request */
    @Column(length = 64)
    private String ipAddress;

    /** User-Agent header (browser/client identification) */
    @Column(length = 512)
    private String userAgent;

    /** Outcome: SUCCESS, FAILED, BLOCKED, DENIED */
    @Column(length = 16)
    private String outcome;

    /** Optional JSON metadata (transaction ID, amount, provider, etc.) */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** Immutable timestamp — set once at creation */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String ua) {
        this.userAgent = ua;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime t) {
        this.timestamp = t;
    }
}
