package com.nyle.nylepay.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class BinanceApiClient {

    @Value("${binance.api-key}")
    private String apiKey;

    @Value("${binance.secret-key}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BINANCE_API_URL = "https://api.binance.com";

    public BigDecimal getTickerPrice(String symbol) {
        try {
            String url = BINANCE_API_URL + "/api/v3/ticker/price?symbol=" + symbol.toUpperCase();
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("price")) {
                return new BigDecimal((String) body.get("price"));
            }
            throw new RuntimeException("Invalid response from Binance API");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch ticker price for " + symbol, e);
        }
    }

    public Map<String, Object> placeMarketOrder(String symbol, String side, BigDecimal quantity) {
        return placeSignedOrder(symbol, side, "MARKET", quantity, null);
    }

    private Map<String, Object> placeSignedOrder(String symbol, String side, String type, BigDecimal quantity, BigDecimal price) {
        try {
            long timestamp = System.currentTimeMillis();
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("symbol=").append(symbol.toUpperCase());
            queryBuilder.append("&side=").append(side.toUpperCase());
            queryBuilder.append("&type=").append(type.toUpperCase());
            
            if (quantity != null) {
                queryBuilder.append("&quantity=").append(quantity.toPlainString());
            }
            if (price != null) {
                queryBuilder.append("&price=").append(price.toPlainString());
            }
            
            queryBuilder.append("&timestamp=").append(timestamp);
            
            String queryData = queryBuilder.toString();
            String signature = generateHmacSha256(queryData, apiSecret);
            queryBuilder.append("&signature=").append(signature);

            String url = BINANCE_API_URL + "/api/v3/order?" + queryBuilder.toString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            // For production, you'd parse the Binance Error JSON to return a meaningful error.
            throw new RuntimeException("Execution failed on Binance Exchange: " + e.getMessage(), e);
        }
    }

    private String generateHmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] raw = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder(2 * raw.length);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign Binance request", e);
        }
    }
}
