package com.nyle.nylepay.services;

import com.nyle.nylepay.services.providers.FlutterwaveService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class BankTransferService {

    private final FlutterwaveService flutterwaveService;

    public BankTransferService(FlutterwaveService flutterwaveService) {
        this.flutterwaveService = flutterwaveService;
    }

    public Map<String, Object> initiateLocalBankTransfer(
            String country,
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration
    ) {

        if ("KE".equalsIgnoreCase(country)) {
            return initiateKenyaBankTransfer(accountNumber, bankCode, amount, currency, narration);
        }
        
        // Fallback for others
        return initiateFlutterwaveTransfer(country, accountNumber, bankCode, amount, currency, narration);
    }

    private Map<String, Object> initiateKenyaBankTransfer(
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration
    ) {
        return flutterwaveService.initiateTransfer(accountNumber, bankCode, amount, currency, narration);
    }

    private Map<String, Object> initiateFlutterwaveTransfer(
            String country,
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("provider", "FLUTTERWAVE");
        response.put("country", country);
        response.put("status", "PENDING");
        response.put("amount", amount);
        response.put("currency", currency);
        response.put("narration", narration);
        return response;
    }
}
