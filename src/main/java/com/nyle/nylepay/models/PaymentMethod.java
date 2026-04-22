package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores a saved payment method for a user.
 * type: MPESA | BANK | CRYPTO | CARD
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    /** MPESA | BANK | CRYPTO | CARD */
    private String type;

    /** JSON string with method details (masked — no raw card numbers) */
    @Column(columnDefinition = "TEXT")
    private String details;

    private boolean isDefault = false;
    private boolean isVerified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters
    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public Long getUserId()             { return userId; }
    public void setUserId(Long userId)  { this.userId = userId; }
    public String getType()             { return type; }
    public void setType(String type)    { this.type = type; }
    public String getDetails()              { return details; }
    public void setDetails(String details)  { this.details = details; }
    public boolean isDefault()              { return isDefault; }
    public void setDefault(boolean def)     { this.isDefault = def; }
    public boolean isVerified()             { return isVerified; }
    public void setVerified(boolean v)      { this.isVerified = v; }
    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }
}
