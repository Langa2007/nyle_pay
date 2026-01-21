// DepositRequest.java
package com.nyle.nylepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10", message = "Minimum deposit is 10")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency; // "KSH", "USD"
    
    @Pattern(regexp = "^2547[0-9]{8}$", message = "Invalid MPesa number format")
    private String mpesaNumber;
    
    private String method; // "MPESA", "BANK"
    private String bankReference; // For bank deposits
}
