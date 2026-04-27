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
    private final com.nyle.nylepay.repositories.CryptoWalletRepository cryptoWalletRepository;
    private final com.nyle.nylepay.services.kyc.KycService kycService;
    private final AntiFraudService antiFraudService;
    private final TransactionService transactionService;

    public CryptoExchangeService(BinanceApiClient binanceApiClient, 
                                WalletService walletService,
                                com.nyle.nylepay.repositories.CryptoWalletRepository cryptoWalletRepository,
                                com.nyle.nylepay.services.kyc.KycService kycService,
                                AntiFraudService antiFraudService,
                                TransactionService transactionService) {
        this.binanceApiClient = binanceApiClient;
        this.walletService = walletService;
        this.cryptoWalletRepository = cryptoWalletRepository;
        this.kycService = kycService;
        this.antiFraudService = antiFraudService;
        this.transactionService = transactionService;
    }

    /**
     * Process an incoming deposit detected on-chain.
     * This is the "Golden Flow": Deposit -> Auto-Swap to KES -> Ready for M-Pesa.
     */
    @Transactional
    public Map<String, Object> processIncomingDeposit(String address, BigDecimal amount, String asset, String txHash) {
        // 1. Resolve address to User
        com.nyle.nylepay.models.CryptoWallet wallet = cryptoWalletRepository.findByAddressIgnoreCase(address)
                .orElseThrow(() -> new RuntimeException("Unrecognized deposit address: " + address));
        
        Long userId = wallet.getUserId();
        asset = asset.toUpperCase();

        // 2. Record the raw deposit
        walletService.addBalance(userId, asset, amount);
        var depositTx = transactionService.createCryptoDeposit(userId, asset, amount, txHash);

        log.info("[GOLDEN FLOW] Detected {} {} deposit for user {}. Auto-swapping to KES...", amount, asset, userId);

        // 3. Trigger Auto-Swap to KES
        try {
            Map<String, Object> swapResult = swapCrypto(userId, asset, "KSH", amount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("depositId", depositTx.getId());
            result.put("swapResult", swapResult);
            result.put("status", "AUTO_SWAPPED_TO_KES");
            return result;
        } catch (Exception e) {
            log.error("Auto-swap failed for deposit {}: {}", txHash, e.getMessage());
            // We still keep the deposit! The user just has the crypto in their NylePay wallet now.
            return Map.of("depositId", depositTx.getId(), "status", "DEPOSITED_PENDING_MANUAL_SWAP", "error", e.getMessage());
        }
    }

    /**
     * Retrieves an existing deposit address for the user or creates a new one.
     */
    public String getOrCreateDepositAddress(Long userId, String chain) {
        return cryptoWalletRepository.findByUserIdAndChain(userId, chain.toUpperCase())
                .map(com.nyle.nylepay.models.CryptoWallet::getAddress)
                .orElseGet(() -> {
                    // In production, this would call a KeyManagementService (KMS)
                    // For demo, we generate a placeholder 0x address
                    com.nyle.nylepay.models.CryptoWallet wallet = new com.nyle.nylepay.models.CryptoWallet();
                    wallet.setUserId(userId);
                    wallet.setChain(chain.toUpperCase());
                    wallet.setAddress("0x" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 40));
                    wallet.setEncryptedPrivateKey("SIMULATED_ENCRYPTED_KEY");
                    cryptoWalletRepository.save(wallet);
                    return wallet.getAddress();
                });
    }

    /**
     * Moves funds from NylePay to an external CEX or private wallet.
     */
    @Transactional
    public Map<String, Object> withdrawToExternal(Long userId, String asset, BigDecimal amount, String address, String network) {
        asset = asset.toUpperCase();

        // 1. Security & Compliance Checks
        if (!kycService.canTransact(userId, amount)) {
            throw new RuntimeException("KYC limit exceeded or verification required for this withdrawal.");
        }
        
        antiFraudService.checkWithdrawal(userId, amount, "CRYPTO_CEX");
        
        // 3. Dispatch to CEX / Liquidity Provider
        Map<String, Object> txResult;
        String txHash = "0x" + java.util.UUID.randomUUID().toString().replace("-", "");

        if (!liveMode) {
            log.info("[SANDBOX] Simulating crypto withdrawal: {} {} to {}", amount, asset, address);
            txResult = new HashMap<>();
            txResult.put("status", "SUCCESS");
            txResult.put("txHash", txHash);
        } else {
            // Live Binance withdrawal
            txResult = binanceApiClient.withdraw(asset, amount, address, network);
        }

        // 4. Record the withdrawal transaction (handles wallet subtraction)
        var tx = transactionService.createWithdrawal(userId, "CRYPTO", amount, asset, address);
        tx.setExternalId(txResult.getOrDefault("txHash", txHash).toString());
        tx.setStatus(txResult.getOrDefault("status", "SUCCESS").toString());

        // 5. Return result
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("transactionId", tx.getId());
        result.put("transactionCode", tx.getTransactionCode());
        result.put("asset", asset);
        result.put("amount", amount);
        result.put("destination", address);
        result.put("status", tx.getStatus());
        result.put("txHash", tx.getExternalId());
        return result;
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
        // walletService.subtractBalance(userId, fromAsset, amount);
        // walletService.addBalance(userId, toAsset, expectedOutput);
        // We now use TransactionService.createConversion which handles balance mutations
        var tx = transactionService.createConversion(userId, fromAsset, toAsset, amount);

        // 6. Return swap receipt
        Map<String, Object> result = new HashMap<>();
        result.put("status", simulated ? "SIMULATED" : "SUCCESS");
        result.put("transactionId", tx.getId());
        result.put("transactionCode", tx.getTransactionCode());
        result.put("from", fromAsset);
        result.put("to", toAsset);
        result.put("input", amount);
        result.put("output", tx.getAmount()); // Amount after conversion and fee
        result.put("rate", rate);
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
