package com.nyle.nylepay.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "wallets")
@Data
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ElementCollection
    @CollectionTable(name = "wallet_balances", joinColumns = @JoinColumn(name = "wallet_id"))
    @MapKeyColumn(name = "currency_code")
    private Map<String, Balance> balances = new HashMap<>();

    private String defaultCurrency = "KSH";

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Balance {
        private BigDecimal amount = BigDecimal.ZERO;
    }
}
