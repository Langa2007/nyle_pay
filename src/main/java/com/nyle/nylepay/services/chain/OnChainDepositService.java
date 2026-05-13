package com.nyle.nylepay.services.chain;

import com.nyle.nylepay.models.CryptoWallet;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.repositories.CryptoWalletRepository;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Processes incoming on-chain crypto deposits triggered by a Moralis / Alchemy
 * webhook.
 *
 * ACID guarantees:
 * Atomicity — @Transactional: wallet credit + transaction save are one unit.
 * Exception propagates if either fails → full rollback.
 * Consistency — txHash stored as externalId with @UniqueConstraint:
 * even if Moralis fires the same event twice, DB rejects the duplicate.
 * Isolation — wallet row locked with SELECT FOR UPDATE before credit.
 * Durability — PostgreSQL WAL ensures committed data survives restarts.
 *
 * Deposit flow:
 * 1. Moralis/Alchemy webhook fires POST /api/crypto/webhook/deposit
 * 2. HMAC-SHA256 signature in X-Signature header is verified
 * 3. toAddress is mapped to a NylePay user via CryptoWallet.address
 * 4. Transaction created as CONFIRMING (awaiting enough block confirmations)
 * 5. On confirm call (or second webhook with confirmations ≥ threshold),
 * status → COMPLETED and wallet balance credited
 */
@Service
public class OnChainDepositService {

    private static final Logger log = LoggerFactory.getLogger(OnChainDepositService.class);

    @Value("${crypto.min-confirmations:6}")
    private int minConfirmations;

    @Value("${crypto.webhook.secret:changeme}")
    private String webhookSecret;

    private final CryptoWalletRepository cryptoWalletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    public OnChainDepositService(CryptoWalletRepository cryptoWalletRepository,
            TransactionRepository transactionRepository,
            WalletService walletService) {
        this.cryptoWalletRepository = cryptoWalletRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
    }

    // Webhook signature verification

    /**
     * Verifies the HMAC-SHA256 signature sent by Moralis/Alchemy in the
     * X-Signature header against the raw request body.
     * Rejects any webhook that cannot be verified.
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expectedHex = HexFormat.of().formatHex(expected);
            // constant-time compare to prevent timing attacks
            return MessageDigest.isEqual(expectedHex.getBytes(), signature.getBytes());
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    // Stage 1 — record the incoming transaction (before full confirmation)

    /**
     * Called immediately when a webhook fires for an unconfirmed tx.
     * Creates a CONFIRMING transaction so the user sees "deposit pending".
     *
     * @param toAddress     NylePay custody address that received funds
     * @param txHash        on-chain transaction hash (becomes externalId — UNIQUE)
     * @param asset         "ETH", "USDT", "USDC", or "DAI"
     * @param amount        token amount (already decimal-adjusted)
     * @param chain         "ETHEREUM", "POLYGON", "ARBITRUM", or "BASE"
     * @param confirmations number of confirmations at time of webhook
     */
    @Transactional
    public Transaction recordIncomingDeposit(String toAddress, String txHash, String asset,
            BigDecimal amount, String chain, int confirmations) {

        // Resolve receiving address to a user
        Optional<CryptoWallet> walletOpt = cryptoWalletRepository.findByAddressIgnoreCase(toAddress);
        if (walletOpt.isEmpty()) {
            log.warn("On-chain deposit ignored — address {} not registered in NylePay", toAddress);
            return null;
        }
        CryptoWallet wallet = walletOpt.get();

        // Idempotency: check if we already know this tx
        Optional<Transaction> existing = transactionRepository.findByExternalId(txHash);
        if (existing.isPresent()) {
            Transaction tx = existing.get();
            if ("CONFIRMING".equals(tx.getStatus()) && confirmations >= minConfirmations) {
                // Advance to confirmed in a separate call
                return finaliseDeposit(tx, amount);
            }
            log.info("On-chain deposit {} already recorded (status={})", txHash, tx.getStatus());
            return tx;
        }

        // Create the initial CONFIRMING record
        Transaction tx = new Transaction();
        tx.setUserId(wallet.getUserId());
        tx.setType("DEPOSIT");
        tx.setProvider("ONCHAIN_" + chain.toUpperCase());
        tx.setAmount(amount);
        tx.setCurrency(asset.toUpperCase());
        tx.setStatus(confirmations >= minConfirmations ? "COMPLETED" : "CONFIRMING");
        tx.setTimestamp(LocalDateTime.now());
        tx.setExternalId(txHash);
        tx.setMetadata("{\"chain\":\"" + chain + "\",\"toAddress\":\"" + toAddress + "\",\"confirmations\":"
                + confirmations + "}");

        try {
            Transaction saved = transactionRepository.save(tx);
            if ("COMPLETED".equals(saved.getStatus())) {
                walletService.addBalance(wallet.getUserId(), asset.toUpperCase(), amount);
                log.info("On-chain deposit {} COMPLETED — credited {} {} to user {}", txHash, amount, asset,
                        wallet.getUserId());
            } else {
                log.info("On-chain deposit {} CONFIRMING — {} confirmations of {} needed", txHash, confirmations,
                        minConfirmations);
            }
            return saved;
        } catch (DataIntegrityViolationException e) {
            // ACID-Consistency: concurrent webhook duplicate blocked by unique constraint
            // on externalId
            log.warn("Duplicate on-chain deposit webhook for txHash={} — already processed", txHash);
            throw e; // propagate → @Transactional rollback of any partial work
        }
    }

    // Stage 2 — finalise a previously CONFIRMING deposit

    /**
     * Advances a CONFIRMING transaction to COMPLETED once enough block
     * confirmations have accumulated. Called either by a second webhook
     * or the /api/crypto/deposit/{txHash}/confirm endpoint.
     *
     * ACID: wallet lock acquired before credit; exception propagates for rollback.
     */
    @Transactional
    public Transaction finaliseDeposit(Transaction tx, BigDecimal confirmedAmount) {
        if (!"CONFIRMING".equals(tx.getStatus())) {
            log.info("finaliseDeposit called on non-CONFIRMING tx {} (status={})", tx.getId(), tx.getStatus());
            return tx;
        }

        BigDecimal amount = confirmedAmount != null ? confirmedAmount : tx.getAmount();
        tx.setAmount(amount);
        tx.setStatus("COMPLETED");

        // ACID-Isolation: wallet row locked before credit (uses findByUserIdForUpdate)
        walletService.addBalance(tx.getUserId(), tx.getCurrency(), amount);

        Transaction saved = transactionRepository.save(tx);
        log.info("On-chain deposit {} finalised — credited {} {} to user {}", tx.getExternalId(), amount,
                tx.getCurrency(), tx.getUserId());
        return saved;
    }
}
