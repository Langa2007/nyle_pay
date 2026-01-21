// WithdrawalRequest.java
package com.nyle.nylepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100", message = "Minimum withdrawal is 100")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotBlank(message = "Method is required")
    private String method; // "MPESA", "BANK", "CRYPTO"
    
    @Pattern(regexp = "^2547[0-9]{8}$", message = "Invalid MPesa number format")
    private String mpesaNumber;
    
    private String bankAccount;
    private String bankName;
    private String swiftCode;
    private String cryptoAddress;
}
