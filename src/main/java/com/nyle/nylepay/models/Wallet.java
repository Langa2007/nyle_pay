package com.nyle.nylepay.models;

import jakarta.persistence.*;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "wallets")
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
    public static class Balance {
        private BigDecimal amount = BigDecimal.ZERO;

        public Balance() {}
        public Balance(BigDecimal amount) { this.amount = amount; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Map<String, Balance> getBalances() { return balances; }
    public void setBalances(Map<String, Balance> balances) { this.balances = balances; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
}
