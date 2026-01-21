package com.nyle.nylepay.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String type;        // "DEPOSIT", "WITHDRAW", "PAYMENT", "CRYPTO_TRANSFER"
    private String provider;    // "MPESA", "BANK", "BLOCKCHAIN"
    private BigDecimal amount;
    private String currency;    // "KSH", "USD", "ETH"
    private String status;      // "PENDING", "COMPLETED", "FAILED"
    private LocalDateTime timestamp;
    private String externalId;  // The M-Pesa Receipt or Blockchain Hash
}
