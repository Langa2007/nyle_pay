package com.nyle.nylepay.services.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AirtelMoneyRoutingService {

    @Value("${airtel-money.client-id:}")
    private String clientId;

    @Value("${airtel-money.client-secret:}")
    private String clientSecret;

    @Value("${airtel-money.base-url:https://openapi.airtel.africa}")
    private String baseUrl;

    @Value("${airtel-money.country:KE}")
    private String country;

    @Value("${airtel-money.currency:KES}")
    private String currency;

    @Value("${airtel-money.callback-url:}")
    private String callbackUrl;

    @Value("${airtel-money.live-mode:false}")
    private boolean liveMode;

    public Map<String, Object> collect(String phone, BigDecimal amount, String reference, String routeCurrency) {
        if (liveMode) {
            ensureConfigured();
            throw new IllegalStateException("Airtel Money live collection adapter is configured but not implemented yet.");
        }
        return sandboxResult("AIRTEL_COLLECTION_INITIATED", phone, amount, reference, routeCurrency);
    }

    public Map<String, Object> payout(String phone, BigDecimal amount, String reference, String routeCurrency) {
        if (liveMode) {
            ensureConfigured();
            throw new IllegalStateException("Airtel Money live payout adapter is configured but not implemented yet.");
        }
        return sandboxResult("AIRTEL_PAYOUT_PROCESSING", phone, amount, reference, routeCurrency);
    }

    private Map<String, Object> sandboxResult(String status, String phone, BigDecimal amount,
            String reference, String routeCurrency) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("provider", "AIRTEL_MONEY");
        result.put("providerReference", "AIR_SANDBOX_" + Math.abs(reference.hashCode()));
        result.put("phone", phone);
        result.put("amount", amount);
        result.put("currency", routeCurrency == null || routeCurrency.isBlank() ? currency : routeCurrency);
        result.put("country", country);
        result.put("callbackUrl", callbackUrl);
        result.put("sandbox", true);
        result.put("baseUrl", baseUrl);
        return result;
    }

    private void ensureConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Airtel Money live mode requires AIRTEL_MONEY_CLIENT_ID and AIRTEL_MONEY_CLIENT_SECRET.");
        }
    }
}
