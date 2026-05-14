package com.nyle.nylepay.services.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PesaLinkRoutingService {

    @Value("${pesalink.client-id:}")
    private String clientId;

    @Value("${pesalink.client-secret:}")
    private String clientSecret;

    @Value("${pesalink.base-url:https://sandbox.pesalink.co.ke}")
    private String baseUrl;

    @Value("${pesalink.participant-id:}")
    private String participantId;

    @Value("${pesalink.callback-url:}")
    private String callbackUrl;

    @Value("${pesalink.live-mode:false}")
    private boolean liveMode;

    public Map<String, Object> payout(String accountNumber, String bankCode, String accountName,
            BigDecimal amount, String reference, String currency) {
        if (liveMode) {
            ensureConfigured();
            throw new IllegalStateException("PesaLink live payout adapter is configured but not implemented yet.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "PESALINK_PAYOUT_PROCESSING");
        result.put("provider", "PESALINK");
        result.put("providerReference", "PSL_SANDBOX_" + Math.abs(reference.hashCode()));
        result.put("accountNumber", accountNumber);
        result.put("bankCode", bankCode);
        result.put("accountName", accountName == null ? "" : accountName);
        result.put("amount", amount);
        result.put("currency", currency);
        result.put("participantId", participantId);
        result.put("callbackUrl", callbackUrl);
        result.put("sandbox", true);
        result.put("baseUrl", baseUrl);
        return result;
    }

    private void ensureConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()
                || participantId == null || participantId.isBlank()) {
            throw new IllegalStateException(
                    "PesaLink live mode requires PESALINK_CLIENT_ID, PESALINK_CLIENT_SECRET, and PESALINK_PARTICIPANT_ID.");
        }
    }
}
