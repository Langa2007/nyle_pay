package com.nyle.nylepay.services.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class FlutterwaveService {

    @Value("${flutterwave.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String BASE_URL = "https://api.flutterwave.com/v3";

    /**
     * Initiate a payout to a Kenyan bank account.
     */
    public Map<String, Object> initiateTransfer(
            String accountNumber, 
            String bankCode, 
            BigDecimal amount, 
            String currency, 
            String narration) {
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("account_bank", bankCode);
            requestBody.put("account_number", accountNumber);
            requestBody.put("amount", amount.toString());
            requestBody.put("currency", currency);
            requestBody.put("narration", narration);
            requestBody.put("reference", "TRF-KE-BANK-" + UUID.randomUUID().toString());
            requestBody.put("callback_url", "https://yourdomain.com/api/payments/webhook/bank");
            requestBody.put("debit_currency", "KSH");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(secretKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/transfers", request, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            
        } catch (Exception e) {
            throw new RuntimeException("Flutterwave Transfer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve account name (Verification).
     */
    public Map<String, Object> resolveAccount(String accountNumber, String bankCode) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("account_number", accountNumber);
            requestBody.put("account_bank", bankCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(secretKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/accounts/resolve", request, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            
        } catch (Exception e) {
            throw new RuntimeException("Account resolution failed: " + e.getMessage(), e);
        }
    }
}
