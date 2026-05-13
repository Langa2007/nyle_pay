package com.nyle.nylepay.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores one NylePay custody wallet per user per chain.
 * The private key is AES-256-GCM encrypted via EncryptionUtils before persisting.
 *
 * Security model:
 *   - Private key NEVER leaves the encrypted column in plaintext
 *   - Decrypted only transiently in memory during signing, then GC'd
 *   - Address (public, derived) stored plaintext for monitoring / deposit receipts
 */
@Entity
@Table(
    name = "crypto_wallets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_crypto_wallet_user_chain", columnNames = {"user_id", "chain"})
    }
)
public class CryptoWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Chain identifier — one of: ETHEREUM, POLYGON, ARBITRUM, BASE
     * EVM compatible; same key pair, different RPC.
     */
    @Column(nullable = false, length = 20)
    private String chain;

    /** The 0x... Ethereum-compatible address derived from the private key. */
    @Column(nullable = false, length = 42)
    private String address;

    /**
     * AES-256-GCM encrypted hex private key.
     * Length 1024 to safely hold IV + ciphertext in Base64.
     */
    @Column(name = "encrypted_private_key", nullable = false, length = 1024)
    private String encryptedPrivateKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEncryptedPrivateKey() { return encryptedPrivateKey; }
    public void setEncryptedPrivateKey(String encryptedPrivateKey) { this.encryptedPrivateKey = encryptedPrivateKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
