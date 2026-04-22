package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;

    // Role-Based Access Control
    @Column(nullable = false)
    private String role = "USER"; // "USER" or "ADMIN"

    // Financial Rails
    private String mpesaNumber;
    private String bankAccountNumber;
    private String cryptoAddress;

    // KYC — Central Bank of Kenya requirement
    /** NONE | PENDING | VERIFIED | REJECTED */
    private String kycStatus = "NONE";
    private String kycProvider;     // "SMILE_IDENTITY" | "MANUAL"
    private String kycReference;    // Provider job ID
    private java.time.LocalDateTime kycVerifiedAt;

    // Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMpesaNumber() { return mpesaNumber; }
    public void setMpesaNumber(String mpesaNumber) { this.mpesaNumber = mpesaNumber; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getCryptoAddress() { return cryptoAddress; }
    public void setCryptoAddress(String cryptoAddress) { this.cryptoAddress = cryptoAddress; }
    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)          { this.createdAt = createdAt; }
    public String getKycStatus()                               { return kycStatus; }
    public void setKycStatus(String kycStatus)                 { this.kycStatus = kycStatus; }
    public String getKycProvider()                             { return kycProvider; }
    public void setKycProvider(String kycProvider)             { this.kycProvider = kycProvider; }
    public String getKycReference()                            { return kycReference; }
    public void setKycReference(String kycReference)           { this.kycReference = kycReference; }
    public java.time.LocalDateTime getKycVerifiedAt()                        { return kycVerifiedAt; }
    public void setKycVerifiedAt(java.time.LocalDateTime kycVerifiedAt)      { this.kycVerifiedAt = kycVerifiedAt; }
}
