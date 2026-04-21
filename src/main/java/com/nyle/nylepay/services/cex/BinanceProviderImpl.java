package com.nyle.nylepay.services.cex;

import com.nyle.nylepay.services.BinanceApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Binance provider implementation — real signed API calls.
 *
 * Uses the existing BinanceApiClient for HMAC-SHA256 signing on trading endpoints.
 * Balance and withdrawal endpoints are implemented here with the same signing approach.
 *
 * Live vs. sandbox: controlled by cex.live-mode property.
 * When live-mode=false, methods simulate successful responses so the rest of the
 * system (routing, ACID, transaction recording) can be tested end-to-end.
 */
@Service
public class BinanceProviderImpl implements ICexProvider {

    private static final Logger log = LoggerFactory.getLogger(BinanceProviderImpl.class);
    private static final String BINANCE_API = "https://api.binance.com";

    @Value("${cex.live-mode:false}")
    private boolean liveMode;

    private final BinanceApiClient binanceApiClient;
    private final RestTemplate restTemplate = new RestTemplate();

    public BinanceProviderImpl(BinanceApiClient binanceApiClient) {
        this.binanceApiClient = binanceApiClient;
    }

    @Override
    public String getProviderName() {
        return "BINANCE";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Connection verification
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean verifyConnection(String apiKey, String apiSecret) {
        if (!liveMode) {
            return apiKey != null && apiKey.length() > 10;
        }
        try {
            long ts   = System.currentTimeMillis();
            String qs = "timestamp=" + ts;
            String sig = hmacSha256(qs, apiSecret);
            String url = BINANCE_API + "/sapi/v1/account/apiTradingStatus?" + qs + "&signature=" + sig;
            HttpHeaders h = new HttpHeaders();
            h.set("X-MBX-APIKEY", apiKey);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(h), new ParameterizedTypeReference<>() {});
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Binance connection verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Balance fetch — GET /api/v3/account (signed)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> fetchBalances(String apiKey, String apiSecret) {
        if (!liveMode) {
            log.warn("[SANDBOX] Returning mock Binance balances");
            return Map.of("USDT", new BigDecimal("1240.50"), "BTC", new BigDecimal("0.024"));
        }
        try {
            long ts  = System.currentTimeMillis();
            String qs = "timestamp=" + ts;
            String sig = hmacSha256(qs, apiSecret);
            String url = BINANCE_API + "/api/v3/account?" + qs + "&signature=" + sig;
            HttpHeaders h = new HttpHeaders();
            h.set("X-MBX-APIKEY", apiKey);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(h), new ParameterizedTypeReference<>() {});
            Map<String, Object> body = resp.getBody();
            if (body == null) throw new RuntimeException("Empty response from Binance account API");

            Map<String, BigDecimal> balances = new HashMap<>();
            List<Map<String, Object>> assets = (List<Map<String, Object>>) body.get("balances");
            if (assets != null) {
                for (Map<String, Object> entry : assets) {
                    BigDecimal free   = new BigDecimal((String) entry.get("free"));
                    BigDecimal locked = new BigDecimal((String) entry.get("locked"));
                    BigDecimal total  = free.add(locked);
                    if (total.compareTo(BigDecimal.ZERO) > 0) {
                        balances.put((String) entry.get("asset"), total);
                    }
                }
            }
            return balances;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Binance balances: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sell to fiat — MARKET SELL on {ASSET}USDT, then Convert endpoint
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> sellToFiat(String asset, BigDecimal amount, String targetFiat,
                                           String apiKey, String apiSecret) {
        if (!liveMode) {
            log.warn("[SANDBOX] Simulating Binance sellToFiat: {} {} → {}", amount, asset, targetFiat);
            return Map.of("status", "SIMULATED_SUCCESS", "orderId", "BIN_MOCK_" + System.currentTimeMillis(),
                          "soldAsset", asset, "soldAmount", amount,
                          "receivedFiat", targetFiat, "receivedAmount", amount.multiply(BigDecimal.valueOf(129.5)));
        }
        // Step 1: MARKET SELL assetUSDT on Binance Spot
        Map<String, Object> sellResult = binanceApiClient.placeMarketOrder(
            asset.toUpperCase() + "USDT", "SELL", amount
        );
        // Step 2: For KES, use Binance Convert or P2P — not yet natively in API;
        //         NylePay internal conversion rate applies for non-USDT targets.
        BigDecimal usdtReceived = extractExecutedValue(sellResult);
        BigDecimal kesAmount = usdtReceived.multiply(BigDecimal.valueOf(129.5)); // live rate via CoinGecko in prod
        Map<String, Object> result = new HashMap<>(sellResult);
        result.put("receivedFiat", targetFiat);
        result.put("receivedAmount", kesAmount);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  On-chain withdrawal — POST /sapi/v1/capital/withdraw/apply (signed)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> withdrawOnChain(String asset, BigDecimal amount, String toAddress,
                                                String network, String apiKey, String apiSecret) {
        if (!liveMode) {
            log.warn("[SANDBOX] Simulating Binance on-chain withdrawal: {} {} → {} on {}", amount, asset, toAddress, network);
            return Map.of("status", "INITIATED", "id", "BIN_WDR_" + System.currentTimeMillis(),
                          "destination", toAddress, "network", network);
        }
        try {
            long ts  = System.currentTimeMillis();
            String qs = "coin=" + asset.toUpperCase()
                      + "&network=" + network.toUpperCase()
                      + "&address=" + toAddress
                      + "&amount=" + amount.toPlainString()
                      + "&timestamp=" + ts;
            String sig = hmacSha256(qs, apiSecret);
            String url = BINANCE_API + "/sapi/v1/capital/withdraw/apply?" + qs + "&signature=" + sig;
            HttpHeaders h = new HttpHeaders();
            h.set("X-MBX-APIKEY", apiKey);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(h), new ParameterizedTypeReference<>() {});
            Map<String, Object> body = resp.getBody();
            if (body == null) throw new RuntimeException("Empty withdrawal response from Binance");
            body.put("destination", toAddress);
            body.put("network", network);
            return body;
        } catch (Exception e) {
            throw new RuntimeException("Binance on-chain withdrawal failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Legacy — kept for backward compatibility; delegates to withdrawOnChain
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> externalWithdraw(String asset, BigDecimal amount, String destAddress,
                                                  String apiKey, String apiSecret) {
        return withdrawOnChain(asset, amount, destAddress, "ETH", apiKey, apiSecret);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private BigDecimal extractExecutedValue(Map<String, Object> orderResult) {
        Object fills = orderResult.get("fills");
        if (fills instanceof List<?> list && !list.isEmpty()) {
            return ((List<Map<String, Object>>) list).stream()
                .map(f -> new BigDecimal((String) f.get("price")).multiply(new BigDecimal((String) f.get("qty"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        Object cummQuote = orderResult.get("cummulativeQuoteQty");
        return cummQuote != null ? new BigDecimal(cummQuote.toString()) : BigDecimal.ZERO;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            StringBuilder sb = new StringBuilder();
            for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign Binance request", e);
        }
    }
}
