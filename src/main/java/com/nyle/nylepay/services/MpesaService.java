package com.nyle.nylepay.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    @Value("${mpesa.callback-url:https://yourdomain.com/api/payments/webhook/mpesa}")
    private String callbackUrl;

    @Value("${mpesa.result-url:https://yourdomain.com/api/payments/webhook/mpesa/result}")
    private String resultUrl;

    @Value("${mpesa.timeout-url:https://yourdomain.com/api/payments/webhook/mpesa/timeout}")
    private String timeoutUrl;

    @Value("${mpesa.confirmation-url:https://yourdomain.com/api/payments/webhook/mpesa/confirmation}")
    private String confirmationUrl;

    @Value("${mpesa.validation-url:https://yourdomain.com/api/payments/webhook/mpesa/validation}")
    private String validationUrl;

    @Value("${mpesa.stk-transaction-type:CustomerPayBillOnline}")
    private String stkTransactionType;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String getBaseUrl() {
        return "production".equals(environment) 
            ? "https://api.safaricom.co.ke" 
            : "https://sandbox.safaricom.co.ke";
    }
    
    public Map<String, Object> stkPush(String phoneNumber, BigDecimal amount, String reference) {
        try {
            String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
            String accessToken = getAccessToken();
            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                (shortCode + passkey + timestamp).getBytes()
            );
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("BusinessShortCode", shortCode);
            requestBody.put("Password", password);
            requestBody.put("Timestamp", timestamp);
            requestBody.put("TransactionType", stkTransactionType);
            requestBody.put("Amount", amount.intValue());
            requestBody.put("PartyA", normalizedPhoneNumber);
            requestBody.put("PartyB", shortCode);
            requestBody.put("PhoneNumber", normalizedPhoneNumber);
            requestBody.put("CallBackURL", callbackUrl);
            requestBody.put("AccountReference", reference);
            requestBody.put("TransactionDesc", "NylePay Deposit");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/stkpush/v1/processrequest";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("MPesa STK Push failed: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> initiateB2C(String phoneNumber, BigDecimal amount, String remarks) {
        try {
            String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
            String accessToken = getAccessToken();
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("InitiatorName", initiatorName);
            requestBody.put("SecurityCredential", securityCredential);
            requestBody.put("CommandID", "BusinessPayment");
            requestBody.put("Amount", amount.toString());
            requestBody.put("PartyA", shortCode);
            requestBody.put("PartyB", normalizedPhoneNumber);
            requestBody.put("Remarks", remarks);
            requestBody.put("QueueTimeOutURL", timeoutUrl);
            requestBody.put("ResultURL", resultUrl);
            requestBody.put("Occasion", "NylePay Withdrawal");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/b2c/v1/paymentrequest";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> result = response.getBody();
            if (result == null) {
                result = new HashMap<>();
            }
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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
            
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
            requestBody.put("ConfirmationURL", confirmationUrl);
            requestBody.put("ValidationURL", validationUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/c2b/v1/registerurl";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to register URLs: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> simulateC2B(String phoneNumber, BigDecimal amount, String billRefNumber) {
        try {
            String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
            String accessToken = getAccessToken();
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("ShortCode", shortCode);
            requestBody.put("CommandID", "CustomerPayBillOnline");
            requestBody.put("Amount", amount.toString());
            requestBody.put("Msisdn", normalizedPhoneNumber);
            requestBody.put("BillRefNumber", billRefNumber);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/c2b/v1/simulate";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
            
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
            requestBody.put("QueueTimeOutURL", timeoutUrl);
            requestBody.put("ResultURL", resultUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/accountbalance/v1/query";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get account balance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Pay to a Till number (Buy Goods) using Safaricom B2B API.
     * CommandID: BusinessBuyGoods
     *
     * @param tillNumber the merchant till number
     * @param amount     amount in KES
     * @param remarks    transaction description
     */
    public Map<String, Object> payToTill(String tillNumber, BigDecimal amount, String remarks) {
        try {
            String accessToken = getAccessToken();

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("Initiator", initiatorName);
            requestBody.put("SecurityCredential", securityCredential);
            requestBody.put("CommandID", "BusinessBuyGoods");
            requestBody.put("SenderIdentifierType", "4");
            requestBody.put("RecieverIdentifierType", "2");
            requestBody.put("Amount", amount.intValue());
            requestBody.put("PartyA", shortCode);
            requestBody.put("PartyB", tillNumber);
            requestBody.put("Remarks", remarks != null ? remarks : "NylePay Till Payment");
            requestBody.put("QueueTimeOutURL", timeoutUrl);
            requestBody.put("ResultURL", resultUrl);
            requestBody.put("AccountReference", "NPY_TILL_" + System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/b2b/v1/paymentrequest";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> result = response.getBody();
            if (result == null) result = new HashMap<>();
            result.put("transactionType", "B2B_TILL");
            result.put("tillNumber", tillNumber);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Till payment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Pay to a Paybill number using Safaricom B2B API.
     * CommandID: BusinessPayBill
     *
     * @param paybillNumber the business shortcode
     * @param accountNumber account reference for the paybill
     * @param amount        amount in KES
     * @param remarks       transaction description
     */
    public Map<String, Object> payToPaybill(String paybillNumber, String accountNumber,
                                             BigDecimal amount, String remarks) {
        try {
            String accessToken = getAccessToken();

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("Initiator", initiatorName);
            requestBody.put("SecurityCredential", securityCredential);
            requestBody.put("CommandID", "BusinessPayBill");
            requestBody.put("SenderIdentifierType", "4");
            requestBody.put("RecieverIdentifierType", "4");
            requestBody.put("Amount", amount.intValue());
            requestBody.put("PartyA", shortCode);
            requestBody.put("PartyB", paybillNumber);
            requestBody.put("Remarks", remarks != null ? remarks : "NylePay Paybill Payment");
            requestBody.put("QueueTimeOutURL", timeoutUrl);
            requestBody.put("ResultURL", resultUrl);
            requestBody.put("AccountReference", accountNumber != null ? accountNumber : "NPY_PBL_" + System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = getBaseUrl() + "/mpesa/b2b/v1/paymentrequest";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> result = response.getBody();
            if (result == null) result = new HashMap<>();
            result.put("transactionType", "B2B_PAYBILL");
            result.put("paybillNumber", paybillNumber);
            result.put("accountReference", accountNumber);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Paybill payment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Pay to Pochi la Biashara (mini business wallet).
     * Uses the same B2B API with BusinessPayBill command.
     * Pochi recipients use the standard Safaricom Pochi shortcode (440000)
     * with the recipient phone as the account reference.
     *
     * @param recipientPhone recipient's M-Pesa phone number
     * @param amount         amount in KES
     * @param remarks        transaction description
     */
    public Map<String, Object> payToPochi(String recipientPhone, BigDecimal amount, String remarks) {
        String normalizedPhone = normalizePhoneNumber(recipientPhone);
        // Pochi la Biashara uses Safaricom's Pochi shortcode
        return payToPaybill("440000", normalizedPhone, amount,
                remarks != null ? remarks : "NylePay Pochi Payment");
    }

    private String getAccessToken() {
        try {
            String auth = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + auth);
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            String url = getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("access_token")) {
                return (String) body.get("access_token");
            }
            throw new RuntimeException("Failed to get access token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get MPesa access token: " + e.getMessage(), e);
        }
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public boolean validateCallback(Map<String, Object> callbackData) {
        try {
            Map<String, Object> details = extractTransactionDetails(callbackData);
            return "0".equals(String.valueOf(details.get("ResultCode")));
        } catch (Exception e) {
            return false;
        }
    }
    
    public Map<String, Object> extractTransactionDetails(Map<String, Object> callbackData) {
        Map<String, Object> details = new HashMap<>();
        try {
            Map<String, Object> body = getNestedMap(callbackData, "Body");
            Map<String, Object> stkCallback = getNestedMap(body, "stkCallback");
            if (stkCallback.isEmpty()) {
                stkCallback = getNestedMap(callbackData, "stkCallback");
            }

            if (!stkCallback.isEmpty()) {
                details.put("ResultCode", stkCallback.get("ResultCode"));
                details.put("ResultDesc", stkCallback.get("ResultDesc"));
                details.put("MerchantRequestID", stkCallback.get("MerchantRequestID"));
                details.put("CheckoutRequestID", stkCallback.get("CheckoutRequestID"));
                details.put("Body", body);

                for (Map<String, Object> item : extractMetadataItems(stkCallback)) {
                    Object name = item.get("Name");
                    Object value = item.get("Value");
                    if (name != null && value != null) {
                        details.put(String.valueOf(name), value);
                    }
                }
            }
        } catch (Exception e) {
            details.put("parseError", e.getMessage());
        }
        return details;
    }

    public Map<String, Object> extractDisbursementDetails(Map<String, Object> callbackData) {
        Map<String, Object> details = new HashMap<>();
        try {
            Map<String, Object> result = getNestedMap(callbackData, "Result");
            if (result.isEmpty()) {
                result = callbackData;
            }

            copyIfPresent(result, details, "ResultType", "ResultCode", "ResultDesc",
                    "OriginatorConversationID", "ConversationID", "TransactionID");

            Map<String, Object> parameters = getNestedMap(result, "ResultParameters");
            Object parameterItems = parameters.get("ResultParameter");
            for (Map<String, Object> item : coerceToMapList(parameterItems)) {
                Object key = firstNonBlank(item.get("Key"), item.get("Name"));
                Object value = item.get("Value");
                if (key != null && value != null) {
                    details.put(String.valueOf(key), value);
                }
            }
        } catch (Exception e) {
            details.put("parseError", e.getMessage());
        }
        return details;
    }

    public String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String normalized = rawPhoneNumber.trim().replace(" ", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }

        if (normalized.matches("^07\\d{8}$")) {
            return "254" + normalized.substring(1);
        }
        if (normalized.matches("^7\\d{8}$")) {
            return "254" + normalized;
        }
        if (normalized.matches("^2547\\d{8}$")) {
            return normalized;
        }

        throw new IllegalArgumentException("Invalid MPesa phone number. Use 2547XXXXXXXX");
    }

    private Map<String, Object> getNestedMap(Map<String, Object> source, String key) {
        if (source == null) {
            return Collections.emptyMap();
        }
        Object value = source.get(key);
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> result = new HashMap<>();
            mapValue.forEach((nestedKey, nestedValue) -> result.put(String.valueOf(nestedKey), nestedValue));
            return result;
        }
        return Collections.emptyMap();
    }

    private List<Map<String, Object>> extractMetadataItems(Map<String, Object> stkCallback) {
        Map<String, Object> callbackMetadata = getNestedMap(stkCallback, "CallbackMetadata");
        Object rawItems = callbackMetadata.get("Item");
        return coerceToMapList(rawItems);
    }

    private List<Map<String, Object>> coerceToMapList(Object value) {
        if (value instanceof List<?> listValue) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : listValue) {
                if (item instanceof Map<?, ?> mapValue) {
                    Map<String, Object> entry = new HashMap<>();
                    mapValue.forEach((nestedKey, nestedValue) -> entry.put(String.valueOf(nestedKey), nestedValue));
                    result.add(entry);
                }
            }
            return result;
        }

        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> single = new HashMap<>();
            mapValue.forEach((nestedKey, nestedValue) -> single.put(String.valueOf(nestedKey), nestedValue));
            return List.of(single);
        }

        return Collections.emptyList();
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> destination, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                destination.put(key, source.get(key));
            }
        }
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String stringValue = String.valueOf(value).trim();
            if (!stringValue.isEmpty() && !"null".equalsIgnoreCase(stringValue)) {
                return value;
            }
        }
        return null;
    }
}
