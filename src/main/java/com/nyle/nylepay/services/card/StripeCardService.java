package com.nyle.nylepay.services.card;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Stripe card payment integration (international, USD/EUR merchants).
 *
 * Flow:
 *   1. Backend creates a PaymentIntent → returns client_secret to frontend
 *   2. Frontend uses Stripe.js to collect card + confirm the PaymentIntent
 *   3. Stripe sends webhook to /api/card/webhook/stripe on completion
 *   4. NylePay verifies Stripe-Signature + credits wallet/merchant
 *
 * PCI DSS SAQ-A: Stripe Elements / Stripe.js handles card capture
 *   entirely in the browser. NylePay never sees the raw card number.
 *
 * API docs: https://stripe.com/docs/api
 */
@Service
public class StripeCardService {

    private static final Logger log = LoggerFactory.getLogger(StripeCardService.class);

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.live-mode:false}")
    private boolean liveMode;

    /**
     * Creates a Stripe PaymentIntent.
     *
     * @param amountCents  amount in the smallest currency unit (cents/pence/sentti)
     * @param currency     ISO-4217 lowercase, e.g. "usd", "kes", "eur"
     * @param description  shown on Stripe dashboard
     * @param idempotencyKey  prevents duplicate charges on retry
     * @return Map containing at minimum: id, client_secret, status
     */
    public Map<String, Object> createPaymentIntent(
            long amountCents,
            String currency,
            String description,
            String idempotencyKey) {

        if (!liveMode) {
            log.warn("[SANDBOX] Stripe PaymentIntent simulated: {} {}", amountCents, currency);
            return Map.of(
                "id",            "pi_SANDBOX_" + idempotencyKey,
                "client_secret", "pi_SANDBOX_secret_test",
                "status",        "requires_payment_method",
                "amount",        amountCents,
                "currency",      currency
            );
        }

        try {
            Stripe.apiKey = secretKey;
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency.toLowerCase())
                .setDescription(description)
                .addPaymentMethodType("card")
                .putMetadata("idempotency_key", idempotencyKey)
                .build();

            PaymentIntent intent = PaymentIntent.create(
                params,
                com.stripe.net.RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build()
            );

            return Map.of(
                "id",            intent.getId(),
                "client_secret", intent.getClientSecret(),
                "status",        intent.getStatus(),
                "amount",        intent.getAmount(),
                "currency",      intent.getCurrency()
            );
        } catch (Exception e) {
            throw new RuntimeException("Stripe PaymentIntent creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Issues a refund for a Stripe charge.
     *
     * @param paymentIntentId  the PaymentIntent ID (pi_...)
     * @param amountCents      null = full refund
     * @param reason           duplicate | fraudulent | requested_by_customer
     */
    public Map<String, Object> refund(String paymentIntentId, Long amountCents, String reason) {
        if (!liveMode) {
            return Map.of("id", "re_SANDBOX_" + paymentIntentId, "status", "succeeded");
        }
        try {
            Stripe.apiKey = secretKey;
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);
            if (amountCents != null) paramsBuilder.setAmount(amountCents);
            if (reason != null) {
                String enumStr = reason.toUpperCase().replace(' ', '_');
                RefundCreateParams.Reason stripeReason = switch (enumStr) {
                    case "DUPLICATE" -> RefundCreateParams.Reason.DUPLICATE;
                    case "FRAUDULENT" -> RefundCreateParams.Reason.FRAUDULENT;
                    case "REQUESTED_BY_CUSTOMER" -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
                    default -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
                };
                paramsBuilder.setReason(stripeReason);
            }

            Refund refund = Refund.create(paramsBuilder.build());
            return Map.of(
                "id",     refund.getId(),
                "status", refund.getStatus(),
                "amount", refund.getAmount()
            );
        } catch (Exception e) {
            throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the Stripe-Signature header using the Stripe SDK's own
     * constant-time HMAC-SHA256 implementation.
     *
     * @param rawPayload   raw request body as string
     * @param sigHeader    value of Stripe-Signature header
     * @return the verified Stripe Event object
     * @throws SignatureVerificationException if the signature is invalid
     */
    public com.stripe.model.Event verifyWebhook(String rawPayload, String sigHeader)
            throws SignatureVerificationException {
        return Webhook.constructEvent(rawPayload, sigHeader, webhookSecret);
    }
}
