package com.nyle.nylepay.services.cex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bybit provider implementation — real signed API calls via BybitApiClient.
 *
 * Uses Bybit Unified Account (V5 API) for all operations.
 * Controlled by cex.live-mode; sandbox returns realistic mock data.
 */
@Service
public class BybitProviderImpl implements ICexProvider {

    private static final Logger log = LoggerFactory.getLogger(BybitProviderImpl.class);

    @Value("${cex.live-mode:false}")
    private boolean liveMode;

    private final BybitApiClient bybitApiClient;

    public BybitProviderImpl(BybitApiClient bybitApiClient) {
        this.bybitApiClient = bybitApiClient;
    }

    @Override
    public String getProviderName() {
        return "BYBIT";
    }

    //  Connection verification — GET /v5/user/query-api

    @Override
    public boolean verifyConnection(String apiKey, String apiSecret) {
        if (!liveMode) {
            return apiKey != null && !apiKey.isBlank();
        }
        try {
            Map<String, Object> result = bybitApiClient.queryApiKey(apiKey, apiSecret);
            return result != null && "0".equals(String.valueOf(result.get("retCode")));
        } catch (Exception e) {
            log.error("Bybit connection verification failed: {}", e.getMessage());
            return false;
        }
    }

    //  Balance fetch — GET /v5/account/wallet-balance?accountType=UNIFIED

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> fetchBalances(String apiKey, String apiSecret) {
        if (!liveMode) {
            log.warn("[SANDBOX] Returning mock Bybit balances");
            return Map.of("USDT", new BigDecimal("500.00"), "ETH", new BigDecimal("1.5"));
        }
        try {
            Map<String, Object> resp = bybitApiClient.getWalletBalance(apiKey, apiSecret);
            if (resp == null || !"0".equals(String.valueOf(resp.get("retCode")))) {
                throw new RuntimeException("Bybit API error: " + (resp != null ? resp.get("retMsg") : "null response"));
            }

            Map<String, BigDecimal> balances = new HashMap<>();
            Map<String, Object> result = (Map<String, Object>) resp.get("result");
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) result.get("list");
            if (accounts != null) {
                for (Map<String, Object> account : accounts) {
                    List<Map<String, Object>> coins = (List<Map<String, Object>>) account.get("coin");
                    if (coins != null) {
                        for (Map<String, Object> coin : coins) {
                            String symbol = (String) coin.get("coin");
                            BigDecimal equity = new BigDecimal(String.valueOf(coin.get("equity")));
                            if (equity.compareTo(BigDecimal.ZERO) > 0) {
                                balances.merge(symbol, equity, BigDecimal::add);
                            }
                        }
                    }
                }
            }
            return balances;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Bybit balances: " + e.getMessage(), e);
        }
    }

    //  Sell to fiat — MARKET SELL spot, then KES conversion

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> sellToFiat(String asset, BigDecimal amount, String targetFiat,
                                           String apiKey, String apiSecret) {
        if (!liveMode) {
            log.warn("[SANDBOX] Simulating Bybit sellToFiat: {} {} → {}", amount, asset, targetFiat);
            return Map.of("status", "SIMULATED_SUCCESS", "orderId", "BYB_MOCK_" + System.currentTimeMillis(),
                          "soldAsset", asset, "soldAmount", amount,
                          "receivedFiat", targetFiat, "receivedAmount", amount.multiply(BigDecimal.valueOf(129.0)));
        }
        try {
            String symbol = asset.toUpperCase() + "USDT";
            Map<String, Object> orderResp = bybitApiClient.placeMarketOrder(symbol, "Sell", amount, apiKey, apiSecret);
            if (orderResp == null || !"0".equals(String.valueOf(orderResp.get("retCode")))) {
                throw new RuntimeException("Bybit order failed: " + (orderResp != null ? orderResp.get("retMsg") : "null"));
            }
            Map<String, Object> orderResult = (Map<String, Object>) ((Map<String, Object>) orderResp.get("result"));
            BigDecimal usdtReceived = orderResult != null
                ? new BigDecimal(String.valueOf(orderResult.getOrDefault("cumExecValue", "0")))
                : BigDecimal.ZERO;
            BigDecimal kesAmount = usdtReceived.multiply(BigDecimal.valueOf(129.0));

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("orderId", orderResult != null ? orderResult.get("orderId") : "UNKNOWN");
            result.put("soldAsset", asset);
            result.put("soldAmount", amount);
            result.put("receivedFiat", targetFiat);
            result.put("receivedAmount", kesAmount);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Bybit sellToFiat failed: " + e.getMessage(), e);
        }
    }

    //  On-chain withdrawal — POST /v5/asset/withdraw/create

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> withdrawOnChain(String asset, BigDecimal amount, String toAddress,
                                                String network, String apiKey, String apiSecret) {
        if (!liveMode) {
            log.warn("[SANDBOX] Simulating Bybit on-chain withdrawal: {} {} → {} on {}", amount, asset, toAddress, network);
            return Map.of("status", "INITIATED", "id", "BYB_WDR_" + System.currentTimeMillis(),
                          "destination", toAddress, "network", network);
        }
        try {
            Map<String, Object> resp = bybitApiClient.createWithdrawal(asset, network, toAddress, amount, apiKey, apiSecret);
            if (resp == null || !"0".equals(String.valueOf(resp.get("retCode")))) {
                throw new RuntimeException("Bybit withdrawal failed: " + (resp != null ? resp.get("retMsg") : "null"));
            }
            Map<String, Object> result = new HashMap<>((Map<String, Object>) resp.get("result"));
            result.put("destination", toAddress);
            result.put("network", network);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Bybit on-chain withdrawal failed: " + e.getMessage(), e);
        }
    }

    /** Legacy alias. */
    @Override
    public Map<String, Object> externalWithdraw(String asset, BigDecimal amount, String destAddress,
                                                  String apiKey, String apiSecret) {
        return withdrawOnChain(asset, amount, destAddress, "ETH", apiKey, apiSecret);
    }
}
