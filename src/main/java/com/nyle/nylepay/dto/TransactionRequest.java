// TransactionRequest.java
package com.nyle.nylepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Transaction type is required")
    private String type; // "DEPOSIT", "WITHDRAW", "TRANSFER", "CONVERSION"
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String destination; // For transfers/withdrawals
    private String provider; // "MPESA", "BANK", "CRYPTO"
    private String description;
    private String toCurrency; // For conversions
}
