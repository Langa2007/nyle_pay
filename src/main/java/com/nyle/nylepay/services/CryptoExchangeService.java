package com.nyle.nylepay.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CryptoExchangeService {
    
    @Value("${coingecko.api-key}")
    private String coinGeckoApiKey;
    
    @Value("${binance.api-key}")
    private String binanceApiKey;
    
    @Value("${binance.secret-key}")
    private String binanceSecretKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public BigDecimal getExchangeRate(String fromCrypto, String toCurrency) {
        try {
            // Try CoinGecko first
            String url = String.format("https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=%s", 
                                      fromCrypto.toLowerCase(), toCurrency.toLowerCase());
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey(fromCrypto.toLowerCase())) {
                Map<String, Object> cryptoData = (Map<String, Object>) response.get(fromCrypto.toLowerCase());
                return BigDecimal.valueOf((Double) cryptoData.get(toCurrency.toLowerCase()));
            }
            
            // Fallback to Binance
            return getBinanceRate(fromCrypto, toCurrency);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get exchange rate", e);
        }
    }
    
    public Map<String, Object> swapCrypto(Long userId, String fromAsset, String toAsset, BigDecimal amount) {
        // Implement crypto-to-crypto swap
        Map<String, Object> result = new HashMap<>();
        
        BigDecimal rate = getExchangeRate(fromAsset, toAsset);
        BigDecimal receivedAmount = amount.multiply(rate);
        
        result.put("from", fromAsset);
        result.put("to", toAsset);
        result.put("input", amount);
        result.put("output", receivedAmount);
        result.put("rate", rate);
        result.put("fee", amount.multiply(BigDecimal.valueOf(0.001))); // 0.1% fee
        
        return result;
    }

    
    private BigDecimal getBinanceRate(String fromCrypto, String toCurrency) {
        try {
            String symbol = fromCrypto.toUpperCase() + toCurrency.toUpperCase();
            String url = String.format("https://api.binance.com/api/v3/ticker/price?symbol=%s", symbol);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("price")) {
                return new BigDecimal((String) response.get("price"));
            } else {
                throw new RuntimeException("Binance rate not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Binance exchange rate", e);
        }
    }
}
