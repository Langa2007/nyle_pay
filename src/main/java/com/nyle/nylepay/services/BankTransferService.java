package com.nyle.nylepay.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Service
public class BankTransferService {

    @Value("${flutterwave.secret-key}")
    private String flutterwaveSecretKey;

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    public Map<String, Object> initiateLocalBankTransfer(
            String country,
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration
    ) {

        switch (country.toUpperCase()) {
            case "KE": // Kenya
                return initiateKenyaBankTransfer(accountNumber, bankCode, amount, narration);

            case "NG": // Nigeria
                return initiatePaystackTransfer(accountNumber, bankCode, amount, currency, narration);

            case "GH": // Ghana
            case "UG": // Uganda
            case "TZ": // Tanzania
            case "ZA": // South Africa
                return initiateFlutterwaveTransfer(country, accountNumber, bankCode, amount, currency, narration);

            default:
                return initiateSWIFTTransfer(country, accountNumber, bankCode, amount, currency, narration);
        }
    }

    /* =========================
       METHOD STUBS (ADDED)
       ========================= */

    private Map<String, Object> initiateKenyaBankTransfer(
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String narration
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("provider", "LOCAL_KE_BANK");
        response.put("status", "PENDING");
        response.put("amount", amount);
        response.put("narration", narration);
        return response;
    }

    private Map<String, Object> initiatePaystackTransfer(
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("provider", "PAYSTACK");
        response.put("status", "PENDING");
        response.put("amount", amount);
        response.put("currency", currency);
        response.put("narration", narration);
        return response;
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

    /* =========================
       EXISTING SWIFT LOGIC
       ========================= */

    private Map<String, Object> initiateSWIFTTransfer(
            String country,
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration
    ) {
        // Implement SWIFT transfers using Wise (TransferWise) API or similar
        Map<String, Object> response = new HashMap<>();
        response.put("provider", "SWIFT");
        response.put("estimated_delivery", "3-5 business days");
        response.put("fees", calculateSWIFTFees(amount, currency));
        response.put("status", "PENDING");
        return response;
    }

    private BigDecimal calculateSWIFTFees(BigDecimal amount, String currency) {
        // Calculate fees based on amount and currency
        return amount.multiply(BigDecimal.valueOf(0.02)); // 2% fee
    }
}
