package com.nyle.nylepay.services.cex;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Bybit V5 API client.
 *
 * Authentication: HMAC-SHA256 over:
 *   timestamp + apiKey + recvWindow + queryString (GET) or requestBody (POST)
 * Headers: X-BAPI-API-KEY, X-BAPI-TIMESTAMP, X-BAPI-SIGN, X-BAPI-RECV-WINDOW
 *
 * All signing happens here; providers call the typed methods.
 */
@Component
public class BybitApiClient {

    private static final String BYBIT_API_URL = "https://api.bybit.com";
    private static final String RECV_WINDOW   = "5000";

    @Value("${bybit.api-key:}")
    private String defaultApiKey;

    @Value("${bybit.secret-key:}")
    private String defaultApiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    //  Account

    /**
     * Fetches unified account wallet balance.
     * GET /v5/account/wallet-balance?accountType=UNIFIED
     */
    public Map<String, Object> getWalletBalance(String apiKey, String apiSecret) {
        String path   = "/v5/account/wallet-balance";
        String params = "accountType=UNIFIED";
        return signedGet(path, params, apiKey, apiSecret);
    }

    //  Trading

    /**
     * Places a market order.
     * POST /v5/order/create
     */
    public Map<String, Object> placeMarketOrder(String symbol, String side, BigDecimal qty,
                                                 String apiKey, String apiSecret) {
        String body = String.format(
            "{\"category\":\"spot\",\"symbol\":\"%s\",\"side\":\"%s\",\"orderType\":\"Market\",\"qty\":\"%s\"}",
            symbol.toUpperCase(), side.toUpperCase(), qty.toPlainString()
        );
        return signedPost("/v5/order/create", body, apiKey, apiSecret);
    }

    //  Asset / Withdrawal

    /**
     * Initiates an on-chain withdrawal.
     * POST /v5/asset/withdraw/create
     */
    public Map<String, Object> createWithdrawal(String coin, String chain, String address,
                                                  BigDecimal amount, String apiKey, String apiSecret) {
        String body = String.format(
            "{\"coin\":\"%s\",\"chain\":\"%s\",\"address\":\"%s\",\"amount\":\"%s\",\"accountType\":\"UNIFIED\"}",
            coin.toUpperCase(), chain.toUpperCase(), address, amount.toPlainString()
        );
        return signedPost("/v5/asset/withdraw/create", body, apiKey, apiSecret);
    }

    /**
     * Verifies API key is valid.
     * GET /v5/user/query-api
     */
    public Map<String, Object> queryApiKey(String apiKey, String apiSecret) {
        return signedGet("/v5/user/query-api", "", apiKey, apiSecret);
    }

    //  HTTP helpers

    private Map<String, Object> signedGet(String path, String queryString, String apiKey, String apiSecret) {
        long timestamp = System.currentTimeMillis();
        String signPayload = timestamp + apiKey + RECV_WINDOW + queryString;
        String signature   = hmacSha256(signPayload, apiSecret);

        HttpHeaders headers = buildHeaders(apiKey, timestamp, signature);
        String url = BYBIT_API_URL + path + (queryString.isEmpty() ? "" : "?" + queryString);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );
        return resp.getBody();
    }

    private Map<String, Object> signedPost(String path, String body, String apiKey, String apiSecret) {
        long timestamp = System.currentTimeMillis();
        String signPayload = timestamp + apiKey + RECV_WINDOW + body;
        String signature   = hmacSha256(signPayload, apiSecret);

        HttpHeaders headers = buildHeaders(apiKey, timestamp, signature);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
            BYBIT_API_URL + path, HttpMethod.POST, new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<>() {}
        );
        return resp.getBody();
    }

    private HttpHeaders buildHeaders(String apiKey, long timestamp, String signature) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-BAPI-API-KEY",      apiKey);
        headers.set("X-BAPI-TIMESTAMP",     String.valueOf(timestamp));
        headers.set("X-BAPI-SIGN",          signature);
        headers.set("X-BAPI-RECV-WINDOW",   RECV_WINDOW);
        return headers;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign Bybit request", e);
        }
    }
}
