package com.nyle.nylepay.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Data // Generates getters and setters automatically
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;

    // Financial Rails
    private String mpesaNumber; // For Kenya transactions
    private String bankAccountNumber; // For Global Bank transactions
    private String cryptoAddress; // The 0x... address we generated earlier

    // Balances
    private BigDecimal fiatBalance; // Standard currency (KSH/USD)
    private BigDecimal cryptoBalance; // Crypto amount
}
