package com.nyle.nylepay.services.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class PayPalRoutingService {

    @Value("${paypal.live-mode:false}")
    private boolean liveMode;

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    public Map<String, Object> createCheckoutOrder(BigDecimal amount, String currency,
                                                   String reference, String returnUrl, String cancelUrl) {
        if (!liveMode) {
            String orderId = "PAYPAL_SANDBOX_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
            return Map.of(
                    "provider", "PAYPAL",
                    "status", "PAYPAL_ORDER_CREATED",
                    "providerReference", orderId,
                    "amount", amount,
                    "currency", currency,
                    "approvalUrl", "https://www.sandbox.paypal.com/checkoutnow?token=" + orderId,
                    "returnUrl", returnUrl != null ? returnUrl : "",
                    "cancelUrl", cancelUrl != null ? cancelUrl : "",
                    "message", "Sandbox PayPal order created. No real money moves until live mode is enabled.");
        }
        ensureLiveCredentials();
        throw new IllegalStateException("PayPal live checkout adapter needs Orders API HTTP implementation before production use.");
    }

    public Map<String, Object> payout(String recipientEmail, BigDecimal amount, String currency, String reference) {
        if (!liveMode) {
            return Map.of(
                    "provider", "PAYPAL",
                    "status", "PAYPAL_PAYOUT_PROCESSING",
                    "providerReference", "PAYOUT_SANDBOX_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
                    "recipientEmail", recipientEmail,
                    "amount", amount,
                    "currency", currency,
                    "message", "Sandbox PayPal payout simulated. Production requires PayPal Payouts access.");
        }
        ensureLiveCredentials();
        throw new IllegalStateException("PayPal live payout adapter needs Payouts API HTTP implementation and account approval before production use.");
    }

    private void ensureLiveCredentials() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("PayPal live mode requires PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("PayPal base URL is required.");
        }
    }
}
