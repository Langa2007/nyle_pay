package com.nyle.nylepay.services.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyle.nylepay.models.CheckoutSession;
import com.nyle.nylepay.models.Merchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * Delivers payment event webhooks to merchant callback URLs.
 *
 * Security:
 *   Every outbound payload is signed with HMAC-SHA256 using the merchant's
 *   unique webhookSecret. Merchants must verify this before trusting any event.
 *
 * Reliability:
 *   Implements 3 retry attempts with exponential backoff (1s, 2s, 4s).
 *   For production, this should be moved to an async queue (Spring @Async + Redis).
 */
@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Delivers a payment.succeeded event to the merchant's webhookUrl.
     */
    public void deliverPaymentSuccess(Merchant merchant, CheckoutSession session) {
        Map<String, Object> event = Map.of(
            "event",     "payment.succeeded",
            "eventId",   "evt_" + System.currentTimeMillis(),
            "timestamp", LocalDateTime.now().toString(),
            "data", Map.of(
                "reference",   session.getReference(),
                "amount",      session.getAmount(),
                "currency",    session.getCurrency(),
                "status",      "COMPLETED",
                "description", session.getDescription() != null ? session.getDescription() : "",
                "createdAt",   session.getCreatedAt().toString()
            )
        );
        deliver(merchant, event);
    }

    /**
     * Delivers a refund.succeeded event to the merchant's webhookUrl.
     */
    public void deliverRefundSuccess(Merchant merchant, String reference,
                                      String refundId, java.math.BigDecimal amount, String currency) {
        Map<String, Object> event = Map.of(
            "event",     "refund.succeeded",
            "eventId",   "evt_" + System.currentTimeMillis(),
            "timestamp", LocalDateTime.now().toString(),
            "data", Map.of(
                "reference", reference,
                "refundId",  refundId,
                "amount",    amount,
                "currency",  currency,
                "status",    "SUCCEEDED"
            )
        );
        deliver(merchant, event);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Core delivery with exponential backoff retry
    // ─────────────────────────────────────────────────────────────────────

    private void deliver(Merchant merchant, Map<String, Object> event) {
        if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isBlank()) {
            log.warn("Merchant {} has no webhookUrl configured — skipping delivery", merchant.getId());
            return;
        }

        try {
            String payload    = objectMapper.writeValueAsString(event);
            String signature  = sign(payload, merchant.getWebhookSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-NylePay-Signature", signature);
            headers.set("X-NylePay-Event",     (String) event.get("event"));

            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            int maxAttempts = 3;
            long delayMs    = 1000;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                        merchant.getWebhookUrl(), HttpMethod.POST, request, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Webhook delivered: merchantId={} event={} attempt={}",
                                 merchant.getId(), event.get("event"), attempt);
                        return;
                    }
                    log.warn("Webhook HTTP {} for merchantId={} attempt={}",
                             response.getStatusCode(), merchant.getId(), attempt);
                } catch (Exception e) {
                    log.warn("Webhook delivery failed attempt={} merchantId={}: {}",
                             attempt, merchant.getId(), e.getMessage());
                }

                if (attempt < maxAttempts) {
                    Thread.sleep(delayMs);
                    delayMs *= 2; // exponential backoff
                }
            }
            log.error("Webhook delivery exhausted all retries: merchantId={} event={}",
                      merchant.getId(), event.get("event"));
        } catch (Exception e) {
            log.error("Webhook serialization/signing error: {}", e.getMessage());
        }
    }

    /**
     * HMAC-SHA256 signs the webhook payload with the merchant's webhookSecret.
     * Merchants must verify: HMAC-SHA256(secret, rawBody) == X-NylePay-Signature header.
     */
    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Webhook signing failed", e);
        }
    }
}
