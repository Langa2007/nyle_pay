package com.nyle.nylepay.services.cex;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Contract for all CEX provider integrations (Binance, Bybit, …).
 *
 * Each method accepts explicit apiKey + apiSecret decrypted by CexRoutingService
 * so providers remain stateless and multi-tenant.
 */
public interface ICexProvider {

    /** Provider identifier, e.g. "BINANCE", "BYBIT". */
    String getProviderName();

    /** Verifies the supplied keys are valid and have read access. */
    boolean verifyConnection(String apiKey, String secret);

    /** Returns a map of asset → total balance on the exchange. */
    Map<String, BigDecimal> fetchBalances(String apiKey, String secret);

    /**
     * Executes a market-sell of {@code asset} for {@code targetFiat} KES/USD.
     * Returns a map including: status, orderId, receivedFiat, receivedAmount.
     */
    Map<String, Object> sellToFiat(String asset, BigDecimal amount, String targetFiat,
                                    String apiKey, String secret);

    /**
     * Broadcasts a native on-chain withdrawal from the CEX to {@code toAddress}.
     *
     * @param asset     token symbol: ETH, USDT, USDC, DAI, BTC …
     * @param amount    amount to withdraw (exchange-side decimal)
     * @param toAddress destination 0x address
     * @param network   network/chain code as required by CEX (e.g. "ETH", "POLYGON", "ARB")
     * @param apiKey    decrypted API key
     * @param secret    decrypted API secret
     * @return          map with at minimum: status, id/txHash, destination, network
     */
    Map<String, Object> withdrawOnChain(String asset, BigDecimal amount, String toAddress,
                                         String network, String apiKey, String secret);

    /**
     * Legacy alias — delegates to withdrawOnChain with network=ETH.
     * @deprecated Use {@link #withdrawOnChain} with explicit network parameter.
     */
    @Deprecated
    Map<String, Object> externalWithdraw(String asset, BigDecimal amount, String destAddress,
                                          String apiKey, String secret);
}
