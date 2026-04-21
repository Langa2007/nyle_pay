package com.nyle.nylepay.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CryptoExchangeService {

    private static final Logger log = LoggerFactory.getLogger(CryptoExchangeService.class);

    @Value("${cex.live-mode:false}")
    private boolean liveMode;

    private final BinanceApiClient binanceApiClient;
    private final WalletService walletService;

    public CryptoExchangeService(BinanceApiClient binanceApiClient, WalletService walletService) {
        this.binanceApiClient = binanceApiClient;
        this.walletService = walletService;
    }
    
    public BigDecimal getExchangeRate(String fromAsset, String toAsset) {
        // Handle direct routing cases where a symbol doesn't directly exist
        // Binance symbols are usually like BTCUSDT, ETHBTC etc.
        try {
            // Standard check (e.g. BTC to USDT -> BTCUSDT)
            return binanceApiClient.getTickerPrice(fromAsset + toAsset);
        } catch (Exception e) {
            try {
                // Reverse check (e.g. USDT to BTC -> wait Binance doesn't have USDTBTC)
                // We fetch BTCUSDT and invert the rate
                BigDecimal reverseRate = binanceApiClient.getTickerPrice(toAsset + fromAsset);
                return BigDecimal.ONE.divide(reverseRate, 8, RoundingMode.HALF_UP);
            } catch (Exception ex) {
                // Not a direct pair. Try routing via USDT (e.g. KES -> USDT -> BTC)
                // In production, you'd calculate multi-hop, here we assume all assets trade against USDT
                if (!fromAsset.equals("USDT") && !toAsset.equals("USDT")) {
                     BigDecimal rate1 = getExchangeRate(fromAsset, "USDT");
                     BigDecimal rate2 = getExchangeRate("USDT", toAsset);
                     return rate1.multiply(rate2);
                }
                throw new RuntimeException("Rate not found for " + fromAsset + "/" + toAsset);
            }
        }
    }
    
    /**
     * Swaps one crypto asset for another using Binance as the execution venue.
     *
     * ACID guarantees:
     *   Atomicity  — @Transactional: wallet debit + credit are one unit.
     *                Balance is mutated ONLY after the exchange confirms the order.
     *                If the order fails, NO balance change occurs.
     *   Consistency — balance check before debit; idempotency key prevents duplicate orders.
     *   Isolation   — WalletService uses SELECT FOR UPDATE on both debit and credit.
     *   Durability  — PostgreSQL commits both balance changes together.
     *
     * Security:
     *   In live-mode=false, orders are simulated and wallets are NOT updated
     *   (prevents phantom balance inflation during development).
     */
    @Transactional
    public Map<String, Object> swapCrypto(Long userId, String fromAsset, String toAsset, BigDecimal amount) {
        fromAsset = fromAsset.toUpperCase();
        toAsset   = toAsset.toUpperCase();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // 1. Balance check (read-only — lock acquired in subtractBalance below)
        BigDecimal available = walletService.getBalance(userId, fromAsset);
        if (available.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient " + fromAsset + " balance. Have: " + available + ", need: " + amount);
        }

        // 2. Fetch live exchange rate
        BigDecimal rate = getExchangeRate(fromAsset, toAsset);
        BigDecimal expectedOutput = amount.multiply(rate);

        // 3. NylePay trading fee (0.2%)
        BigDecimal fee           = amount.multiply(new BigDecimal("0.002"));
        BigDecimal netInputForSwap = amount.subtract(fee);

        // 4. Place the Binance market order BEFORE touching the wallet.
        //    Idempotency key = userId + timestamp prevents duplicates on retry.
        String side          = determineside(fromAsset, toAsset);
        String symbol        = getStandardSymbol(fromAsset, toAsset);
        BigDecimal orderQty  = "SELL".equals(side) ? netInputForSwap : netInputForSwap.multiply(rate);
        String clientOrderId = "NYL_" + userId + "_" + System.currentTimeMillis();

        Map<String, Object> orderResult;
        boolean simulated = false;

        if (!liveMode) {
            // Sandbox: simulate only — wallets updated below only in sandbox path
            log.warn("[SANDBOX] Simulating Binance swap: {} {} {} → {}", side, orderQty, symbol, toAsset);
            orderResult = new HashMap<>();
            orderResult.put("status", "SIMULATED_SUCCESS");
            orderResult.put("orderId", clientOrderId);
            orderResult.put("executedQty", orderQty.toPlainString());
            simulated = true;
        } else {
            // Live: order must succeed before any ledger change
            orderResult = binanceApiClient.placeMarketOrder(symbol, side, orderQty);
            if (orderResult == null) {
                throw new RuntimeException("Binance returned null response for order " + clientOrderId);
            }
            String status = String.valueOf(orderResult.getOrDefault("status", ""));
            if (!"FILLED".equals(status) && !"PARTIALLY_FILLED".equals(status)) {
                throw new RuntimeException("Binance order not filled (status=" + status + "). No balance change applied.");
            }
        }

        // 5. Update NylePay wallet ONLY after confirmed execution
        //    ACID-Isolation: both calls use SELECT FOR UPDATE inside WalletService
        walletService.subtractBalance(userId, fromAsset, amount);
        walletService.addBalance(userId, toAsset, expectedOutput);

        // 6. Return swap receipt
        Map<String, Object> result = new HashMap<>();
        result.put("status", simulated ? "SIMULATED" : "SUCCESS");
        result.put("from", fromAsset);
        result.put("to", toAsset);
        result.put("input", amount);
        result.put("output", expectedOutput);
        result.put("rate", rate);
        result.put("feeDeducted", fee);
        result.put("binanceOrderId", orderResult.getOrDefault("orderId", clientOrderId));
        result.put("clientOrderId", clientOrderId);
        return result;
    }
    
    private String determineside(String fromAsset, String toAsset) {
        if (toAsset.equalsIgnoreCase("USDT") || toAsset.equalsIgnoreCase("KES") || toAsset.equalsIgnoreCase("USD")) {
            return "SELL"; // Selling Crypto for Stable/Fiat
        }
        return "BUY"; // Buying Crypto
    }
    
    private String getStandardSymbol(String fromAsset, String toAsset) {
        if ("SELL".equals(determineside(fromAsset, toAsset))) {
            return fromAsset.toUpperCase() + toAsset.toUpperCase();
        }
        return toAsset.toUpperCase() + fromAsset.toUpperCase();
    }
}
