// MpesaService.java - UPDATED
package com.nyle.nylepay.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class MpesaService {
    
    @Value("${mpesa.consumer-key:}")
    private String consumerKey;
    
    @Value("${mpesa.consumer-secret:}")
    private String consumerSecret;
    
    @Value("${mpesa.shortcode:}")
    private String shortCode;
    
    @Value("${mpesa.passkey:}")
    private String passkey;
    
    @Value("${mpesa.initiator-name:}")
    private String initiatorName;
    
    @Value("${mpesa.security-credential:}")
    private String securityCredential;
    
    @Value("${mpesa.environment:sandbox}")
    private String environment;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String getBaseUrl() {
        return environment.equals("production") 
            ? "https://api.safaricom.co.ke" 
            : "https://sandbox.safaricom.co.ke";
    }
    
    public Map<String, Object> stkPush(String phoneNumber, BigDecimal amount, String reference) {
        try {
            // Get access token
            String accessToken = getAccessToken();
            
            // Prepare STK Push request
            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                (shortCode + passkey + timestamp).getBytes()
            );
            
            // Create request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("BusinessShortCode", shortCode);
            requestBody.put("Password", password);
            requestBody.put("Timestamp", timestamp);
            requestBody.put("TransactionType", "CustomerPayBillOnline");
            requestBody.put("Amount", amount.intValue());
            requestBody.put("PartyA", phoneNumber);
            requestBody.put("PartyB", shortCode);
            requestBody.put("PhoneNumber", phoneNumber);
            requestBody.put("CallBackURL", "https://yourdomain.com/api/payments/webhook/mpesa");
            requestBody.put("AccountReference", reference);
            requestBody.put("TransactionDesc", "NylePay Deposit");
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            // Make API call
            String url = getBaseUrl() + "/mpesa/stkpush/v1/processrequest";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            throw new RuntimeException("MPesa STK Push failed: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> initiateB2C(String phoneNumber, BigDecimal amount, String remarks) {
        try {
            // Get access token
            String accessToken = getAccessToken();
            
            // Create request body for B2C (Business to Customer)
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("InitiatorName", initiatorName);
            requestBody.put("SecurityCredential", securityCredential);
            requestBody.put("CommandID", "BusinessPayment"); // or "SalaryPayment", "PromotionPayment"
            requestBody.put("Amount", amount.toString());
            requestBody.put("PartyA", shortCode);
            requestBody.put("PartyB", phoneNumber);
            requestBody.put("Remarks", remarks);
            requestBody.put("QueueTimeOutURL", "https://yourdomain.com/api/payments/webhook/mpesa-timeout");
            requestBody.put("ResultURL", "https://yourdomain.com/api/payments/webhook/mpesa-result");
            requestBody.put("Occasion", "NylePay Withdrawal");
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            // Make API call
            String url = getBaseUrl() + "/mpesa/b2c/v1/paymentrequest";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
            
            // Add additional info
            result.put("transactionType", "B2C");
            result.put("timestamp", LocalDateTime.now().toString());
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("MPesa B2C payment failed: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> checkTransactionStatus(String checkoutRequestId) {
        try {
            String accessToken = getAccessToken();
            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                (shortCode + passkey + timestamp).getBytes()
            );
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("BusinessShortCode", shortCode);
            requestBody.put("Password", password);
            requestBody.put("Timestamp", timestamp);
            requestBody.put("CheckoutRequestID", checkoutRequestId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            String url = getBaseUrl() + "/mpesa/stkpushquery/v1/query";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to check transaction status: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> registerURLs() {
        try {
            String accessToken = getAccessToken();
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("ShortCode", shortCode);
            requestBody.put("ResponseType", "Completed");
            requestBody.put("ConfirmationURL", "https://yourdomain.com/api/payments/webhook/mpesa-confirmation");
            requestBody.put("ValidationURL", "https://yourdomain.com/api/payments/webhook/mpesa-validation");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            String url = getBaseUrl() + "/mpesa/c2b/v1/registerurl";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to register URLs: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> simulateC2B(String phoneNumber, BigDecimal amount, String billRefNumber) {
        try {
            String accessToken = getAccessToken();
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("ShortCode", shortCode);
            requestBody.put("CommandID", "CustomerPayBillOnline");
            requestBody.put("Amount", amount.toString());
            requestBody.put("Msisdn", phoneNumber);
            requestBody.put("BillRefNumber", billRefNumber);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            String url = getBaseUrl() + "/mpesa/c2b/v1/simulate";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to simulate C2B: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getAccountBalance() {
        try {
            String accessToken = getAccessToken();
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("Initiator", initiatorName);
            requestBody.put("SecurityCredential", securityCredential);
            requestBody.put("CommandID", "AccountBalance");
            requestBody.put("PartyA", shortCode);
            requestBody.put("IdentifierType", "4");
            requestBody.put("Remarks", "Balance check");
            requestBody.put("QueueTimeOutURL", "https://yourdomain.com/api/payments/webhook/mpesa-timeout");
            requestBody.put("ResultURL", "https://yourdomain.com/api/payments/webhook/mpesa-result");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            String url = getBaseUrl() + "/mpesa/accountbalance/v1/query";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get account balance: " + e.getMessage(), e);
        }
    }
    
    private String getAccessToken() {
        try {
            String auth = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + auth);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            String url = getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
            
            throw new RuntimeException("Failed to get access token");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get MPesa access token: " + e.getMessage(), e);
        }
    }
    
    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    
    // Helper method to validate MPesa response
    public boolean validateCallback(Map<String, Object> callbackData) {
        try {
            if (!callbackData.containsKey("Body") || !callbackData.containsKey("stkCallback")) {
                return false;
            }
            
            Map<String, Object> stkCallback = (Map<String, Object>) callbackData.get("stkCallback");
            if (!stkCallback.containsKey("ResultCode")) {
                return false;
            }
            
            String resultCode = stkCallback.get("ResultCode").toString();
            return "0".equals(resultCode);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    // Helper method to extract transaction details from callback
    public Map<String, Object> extractTransactionDetails(Map<String, Object> callbackData) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            if (callbackData.containsKey("Body") && callbackData.containsKey("stkCallback")) {
                Map<String, Object> stkCallback = (Map<String, Object>) callbackData.get("stkCallback");
                
                if (stkCallback.containsKey("CallbackMetadata")) {
                    Map<String, Object> metadata = (Map<String, Object>) stkCallback.get("CallbackMetadata");
                    Map<String, Object> items = (Map<String, Object>) metadata.get("Item");
                    
                    for (Map.Entry<String, Object> entry : items.entrySet()) {
                        details.put(entry.getKey(), entry.getValue());
                    }
                }
                
                details.put("ResultCode", stkCallback.get("ResultCode"));
                details.put("ResultDesc", stkCallback.get("ResultDesc"));
                details.put("MerchantRequestID", stkCallback.get("MerchantRequestID"));
                details.put("CheckoutRequestID", stkCallback.get("CheckoutRequestID"));
            }
            
        } catch (Exception e) {
            // Log error but return empty details
        }
        
        return details;
    }
}