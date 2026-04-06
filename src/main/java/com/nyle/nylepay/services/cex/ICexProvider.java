package com.nyle.nylepay.services.cex;

import java.math.BigDecimal;
import java.util.Map;

public interface ICexProvider {
    String getProviderName(); // e.g., "BINANCE", "BYBIT"
    
    // Verifies keys are working
    boolean verifyConnection(String apiKey, String secret);
    
    // Fetch user's spot balances across the CEX
    Map<String, BigDecimal> fetchBalances(String apiKey, String secret);
    
    // Instant trade mechanism (e.g. USDT -> KES via P2P or Internal swap depending on CEX)
    Map<String, Object> sellToFiat(String asset, BigDecimal amount, String targetFiat, String apiKey, String secret);
    
    // Transfer funds from CEX directly outwards (like bypassing to an external wallet)
    Map<String, Object> externalWithdraw(String asset, BigDecimal amount, String destAddress, String apiKey, String secret);
}
