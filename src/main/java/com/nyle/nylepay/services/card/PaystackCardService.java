package com.nyle.nylepay.services.card;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Paystack card payment integration.
 *
 * Supports Visa, Mastercard, and Verve (Africa-native).
 * Primary card acquirer for Kenya and the rest of Africa.
 *
 * PCI DSS SAQ-A: card capture is handled entirely by Paystack's
 * hosted checkout page — NylePay never sees the raw card number.
 *
 * API docs: https://paystack.com/docs/api/
 */
@Service
public class PaystackCardService {

    private static final Logger log = LoggerFactory.getLogger(PaystackCardService.class);
    private static final String BASE_URL = "https://api.paystack.co";

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.webhook-secret:}")
    private String webhookSecret;

    @Value("${paystack.live-mode:false}")
    private boolean liveMode;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes a Paystack transaction and returns an authorization_url.
     * Redirect your customer to the authorization_url to complete payment.
     *
     * @param email       customer email (required by Paystack)
     * @param amount      amount IN KOBO (KES) or smallest currency unit — e.g. KES 100 = 10000 kobo
     * @param currency    ISO-4217 e.g. "KES", "USD", "NGN"
     * @param reference   your unique idempotency reference
     * @param callbackUrl URL Paystack redirects customer to after payment
     * @param metadata    extra data stored with the transaction (JSON string)
     */
    public Map<String, Object> initializeTransaction(
            String email,
            BigDecimal amount,
            String currency,
            String reference,
            String callbackUrl,
            String metadata) {

        if (!liveMode) {
            log.warn("[SANDBOX] Paystack TX simulated: {} {} ref={}", amount, currency, reference);
            return Map.of(
                "status",            true,
                "message",           "Authorization URL created (SANDBOX)",
                "data", Map.of(
                    "authorization_url", "https://checkout.paystack.com/sandbox",
                    "access_code",       "SANDBOX_ACCESS_CODE",
                    "reference",         reference
                )
            );
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("email",        email);
            // Paystack expects amount in the smallest currency unit (kobo/cents)
            body.put("amount",       amount.multiply(BigDecimal.valueOf(100)).longValue());
            body.put("currency",     currency);
            body.put("reference",    reference);
            body.put("callback_url", callbackUrl);
            if (metadata != null) body.put("metadata", metadata);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(secretKey);

            ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/transaction/initialize",
                HttpMethod.POST,
                new HttpEntity<>(body.toString(), headers),
                String.class
            );
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Paystack initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies a Paystack transaction after webhook or callback.
     * Always verify server-side — never trust a client-side completed status.
     */
    public Map<String, Object> verifyTransaction(String reference) {
        if (!liveMode) {
            return Map.of(
                "status", true,
                "data", Map.of(
                    "status",    "success",
                    "reference", reference,
                    "amount",    10000,
                    "currency",  "KES"
                )
            );
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/transaction/verify/" + reference,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Paystack verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Issues a full or partial refund.
     */
    public Map<String, Object> refund(String transactionRef, BigDecimal amount) {
        if (!liveMode) {
            return Map.of("status", true, "data", Map.of("id", "SANDBOX_REFUND_" + UUID.randomUUID()));
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("transaction", transactionRef);
            if (amount != null) {
                body.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue());
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(secretKey);
            ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/refund",
                HttpMethod.POST,
                new HttpEntity<>(body.toString(), headers),
                String.class
            );
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Paystack refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the X-Paystack-Signature HMAC-SHA512 header.
     * Must be validated BEFORE processing any webhook payload.
     *
     * @param rawBody   the raw request body bytes (do NOT parse JSON first)
     * @param signature value of X-Paystack-Signature header
     */
    public boolean verifyWebhookSignature(byte[] rawBody, String signature) {
        if (secretKey == null || secretKey.isBlank()) return false;
        if (signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] computed = mac.doFinal(rawBody);
            String computedHex = HexFormat.of().formatHex(computed);
            return java.security.MessageDigest.isEqual(
                computedHex.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Paystack HMAC verification error: {}", e.getMessage());
            return false;
        }
    }
}
