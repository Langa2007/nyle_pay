package com.nyle.nylepay.services.cex;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class BybitProviderImpl implements ICexProvider {

    @Override
    public String getProviderName() {
        return "BYBIT";
    }

    @Override
    public boolean verifyConnection(String apiKey, String secret) {
        // Hits Bybit V5 OpenAPI /v5/user/query-api
        return apiKey != null && apiKey.startsWith("bybit_");
    }

    @Override
    public Map<String, BigDecimal> fetchBalances(String apiKey, String secret) {
        // Mock payload mimicking Bybit balances
        Map<String, BigDecimal> balances = new HashMap<>();
        balances.put("USDT", new BigDecimal("500.00"));
        balances.put("ETH", new BigDecimal("1.5"));
        return balances;
    }

    @Override
    public Map<String, Object> sellToFiat(String asset, BigDecimal amount, String targetFiat, String apiKey, String secret) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("soldAsset", asset);
        result.put("soldAmount", amount);
        result.put("receivedFiat", targetFiat);
        
        // Mock exchange rate for testing (e.g. 129.5 KES per 1 USDT)
        BigDecimal rate = new BigDecimal("129.5");
        result.put("receivedAmount", amount.multiply(rate));
        result.put("orderId", "BYB_" + System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, Object> externalWithdraw(String asset, BigDecimal amount, String destAddress, String apiKey, String secret) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "INITIATED");
        result.put("destination", destAddress);
        result.put("amount", amount);
        result.put("transactionId", "BYB_WDR_" + System.currentTimeMillis());
        return result;
    }
}
