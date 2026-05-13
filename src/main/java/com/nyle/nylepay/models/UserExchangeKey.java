package com.nyle.nylepay.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_exchange_keys")
@Data
public class UserExchangeKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String exchangeName; // e.g. "BINANCE", "BYBIT"

    // These MUST be stored encrypted!
    @Column(nullable = false, length = 1024)
    private String encryptedApiKey;

    @Column(nullable = false, length = 1024)
    private String encryptedApiSecret;

    @Column(nullable = false)
    private LocalDateTime linkedAt;
    
    @PrePersist
    public void setLinkingTime() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getEncryptedApiKey() { return encryptedApiKey; }
    public void setEncryptedApiKey(String encryptedApiKey) { this.encryptedApiKey = encryptedApiKey; }
    public String getEncryptedApiSecret() { return encryptedApiSecret; }
    public void setEncryptedApiSecret(String encryptedApiSecret) { this.encryptedApiSecret = encryptedApiSecret; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
}
