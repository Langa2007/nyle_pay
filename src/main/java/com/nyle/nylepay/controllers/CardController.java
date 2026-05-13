package com.nyle.nylepay.controllers;

import com.nyle.nylepay.services.card.CardPaymentService;
import com.nyle.nylepay.services.card.PaystackCardService;
import com.nyle.nylepay.services.card.StripeCardService;
import com.nyle.nylepay.services.kyc.AmlScreeningService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Card payment endpoints (Visa / Mastercard via Paystack and Stripe).
 *
 * Security:
 *   - All card initiation endpoints require JWT authentication.
 *   - Webhook endpoints are permit-all (no JWT) but enforce HMAC signatures.
 *   - Raw body is read BEFORE any JSON parsing (HMAC must cover raw bytes).
 */
@RestController
@RequestMapping("/api/card")
public class CardController {

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    private final CardPaymentService  cardPaymentService;
    private final PaystackCardService paystackCardService;
    private final StripeCardService   stripeCardService;
    private final AmlScreeningService amlScreeningService;
    private final ObjectMapper        objectMapper = new ObjectMapper();

    public CardController(CardPaymentService cardPaymentService,
                          PaystackCardService paystackCardService,
                          StripeCardService stripeCardService,
                          AmlScreeningService amlScreeningService) {
        this.cardPaymentService  = cardPaymentService;
        this.paystackCardService = paystackCardService;
        this.stripeCardService   = stripeCardService;
        this.amlScreeningService = amlScreeningService;
    }

    // Paystack — initiate card top-up

    /**
     * POST /api/card/paystack/initiate
     * Initializes a Paystack card transaction.
     * Returns an authorization_url — redirect your customer there.
     *
     * Body: { "amount": 1000, "currency": "KES", "callbackUrl": "https://..." }
     */
    @PostMapping("/paystack/initiate")
    public ResponseEntity<Map<String, Object>> initiatePaystack(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId   = getUserId(auth);
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency   = (String) body.getOrDefault("currency", "KES");
        String callbackUrl = (String) body.getOrDefault("callbackUrl", "");

        AmlScreeningService.AmlResult aml = amlScreeningService.screenTransaction(
            userId, amount, currency, "CARD_DEPOSIT", "DEPOSIT");
        if (aml.isBlocked()) {
            return ResponseEntity.status(403).body(Map.of("error", "Transaction blocked by AML screening"));
        }

        String email = body.containsKey("email") ? (String) body.get("email") : auth.getName();

        Map<String, Object> result = cardPaymentService.initiatePaystackDeposit(
            userId, email, amount, currency, callbackUrl);
        return ResponseEntity.ok(result);
    }

    // Stripe — initiate card top-up

    /**
     * POST /api/card/stripe/initiate
     * Creates a Stripe PaymentIntent. Returns client_secret for Stripe.js.
     *
     * Body: { "amount": 10.00, "currency": "usd" }
     */
    @PostMapping("/stripe/initiate")
    public ResponseEntity<Map<String, Object>> initiateStripe(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = getUserId(auth);
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency   = (String) body.getOrDefault("currency", "usd");

        AmlScreeningService.AmlResult aml = amlScreeningService.screenTransaction(
            userId, amount, currency, "CARD_DEPOSIT", "DEPOSIT");
        if (aml.isBlocked()) {
            return ResponseEntity.status(403).body(Map.of("error", "Transaction blocked by AML screening"));
        }

        Map<String, Object> result = cardPaymentService.initiateStripeDeposit(userId, amount, currency);
        return ResponseEntity.ok(result);
    }

    // Paystack Webhook — HMAC-SHA512 verified

    /**
     * POST /api/card/webhook/paystack
     * Receives Paystack payment events.
     * Configure this URL in Paystack Dashboard → Settings → Webhooks.
     */
    @PostMapping("/webhook/paystack")
    public ResponseEntity<String> paystackWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody byte[] rawBody) {

        // Security: verify HMAC-SHA512 before any processing
        if (!paystackCardService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Paystack webhook rejected: invalid signature");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
            String event = (String) payload.getOrDefault("event", "");

            if ("charge.success".equals(event)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                String reference    = (String) data.get("reference");
                Object amountObj    = data.get("amount");
                String currency     = (String) data.getOrDefault("currency", "KES");
                BigDecimal amount   = new BigDecimal(amountObj.toString())
                                        .divide(BigDecimal.valueOf(100));

                cardPaymentService.processPaystackPaymentSuccess(reference, amount, currency);
                log.info("Paystack charge.success processed: ref={}", reference);
            } else {
                log.info("Paystack event={} received but not handled", event);
            }
        } catch (IOException e) {
            log.error("Paystack webhook parse error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Parse error");
        }
        return ResponseEntity.ok("OK");
    }

    // Stripe Webhook — Stripe-Signature verified

    /**
     * POST /api/card/webhook/stripe
     * Receives Stripe payment events.
     * Configure this URL in Stripe Dashboard → Developers → Webhooks.
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> stripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
            @RequestBody String rawPayload) {

        Event event;
        try {
            event = stripeCardService.verifyWebhook(rawPayload, sigHeader);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook rejected: invalid signature");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject().orElse(null);
            if (stripeObject instanceof PaymentIntent pi) {
                cardPaymentService.processStripePaymentSuccess(
                    pi.getId(), pi.getAmount(), pi.getCurrency());
                log.info("Stripe payment_intent.succeeded processed: {}", pi.getId());
            }
        } else {
            log.info("Stripe event={} received but not handled", event.getType());
        }
        return ResponseEntity.ok("OK");
    }

    // Verify a Paystack payment callback (for frontend redirect verification)

    /**
     * GET /api/card/paystack/verify/{reference}
     * Verifies a Paystack transaction by reference.
     * Call this after the customer is redirected back from Paystack checkout.
     */
    @GetMapping("/paystack/verify/{reference}")
    public ResponseEntity<Map<String, Object>> verifyPaystack(
            @PathVariable String reference,
            Authentication auth) {

        Map<String, Object> result = paystackCardService.verifyTransaction(reference);
        return ResponseEntity.ok(result);
    }


    private Long getUserId(Authentication auth) {
        if (auth == null) throw new RuntimeException("Not authenticated");
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // In NylePay the username IS the user's email — look up ID
            // For now return from name (JWT should carry userId as claim in production)
            return Long.parseLong(auth.getName().replaceAll("[^0-9]", "").isBlank()
                ? "0" : auth.getName().replaceAll("[^0-9]", ""));
        }
        throw new RuntimeException("Cannot resolve userId from auth principal");
    }
}
