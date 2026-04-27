package com.nyle.nylepay.services.card;

import com.nyle.nylepay.models.CheckoutSession;
import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.repositories.CheckoutSessionRepository;
import com.nyle.nylepay.repositories.MerchantRepository;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified card payment orchestration layer.
 *
 * Dispatches to the right provider (Paystack / Stripe / Flutterwave)
 * and handles the ACID-safe wallet credit flow.
 *
 * ACID guarantees:
 *   Atomicity   — wallet credit + transaction save in one @Transactional unit
 *   Consistency — isFinalStatus() guard prevents double-credits on retries
 *   Isolation   — WalletService uses SELECT FOR UPDATE internally
 *   Durability  — DB commit covers both wallet balance + transaction record
 */
@Service
public class CardPaymentService {

    private static final Logger log = LoggerFactory.getLogger(CardPaymentService.class);

    private final PaystackCardService paystackCardService;
    private final StripeCardService   stripeCardService;
    private final WalletService       walletService;
    private final TransactionRepository      transactionRepository;
    private final CheckoutSessionRepository  checkoutSessionRepository;
    private final MerchantRepository         merchantRepository;

    public CardPaymentService(
            PaystackCardService paystackCardService,
            StripeCardService stripeCardService,
            WalletService walletService,
            TransactionRepository transactionRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            MerchantRepository merchantRepository) {
        this.paystackCardService     = paystackCardService;
        this.stripeCardService       = stripeCardService;
        this.walletService           = walletService;
        this.transactionRepository   = transactionRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.merchantRepository      = merchantRepository;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Paystack — initiate
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a Paystack transaction for a direct user deposit (card → NylePay wallet).
     * Returns an authorization_url for the user to complete payment.
     */
    @Transactional
    public Map<String, Object> initiatePaystackDeposit(
            Long userId,
            String email,
            BigDecimal amount,
            String currency,
            String callbackUrl) {

        String reference = "NPY-PS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // Save PENDING transaction for idempotency tracking
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType("DEPOSIT");
        tx.setProvider("PAYSTACK");
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setStatus("PENDING");
        tx.setTimestamp(LocalDateTime.now());
        tx.setExternalId(reference);
        transactionRepository.save(tx);

        Map<String, Object> result = paystackCardService.initializeTransaction(
            email, amount, currency, reference, callbackUrl, null);

        return Map.of(
            "transactionId", tx.getId(),
            "reference",     reference,
            "provider",      "PAYSTACK",
            "paystackData",  result.getOrDefault("data", Map.of())
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Stripe — initiate
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a Stripe PaymentIntent for a direct user deposit (card → NylePay wallet).
     * Returns a client_secret for Stripe.js to complete the payment in the browser.
     */
    @Transactional
    public Map<String, Object> initiateStripeDeposit(
            Long userId,
            BigDecimal amount,
            String currency) {

        String idempotencyKey = "NPY-STR-" + userId + "-" + System.currentTimeMillis();
        // Stripe expects smallest unit (cents)
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, Object> result = stripeCardService.createPaymentIntent(
            amountCents, currency, "NylePay Wallet Top-up", idempotencyKey);

        String paymentIntentId = (String) result.get("id");

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType("DEPOSIT");
        tx.setProvider("STRIPE");
        tx.setAmount(amount);
        tx.setCurrency(currency.toUpperCase());
        tx.setStatus("PENDING");
        tx.setTimestamp(LocalDateTime.now());
        tx.setExternalId(paymentIntentId);
        transactionRepository.save(tx);

        return Map.of(
            "transactionId", tx.getId(),
            "clientSecret",  result.get("client_secret"),
            "paymentIntentId", paymentIntentId,
            "provider",      "STRIPE"
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Webhook handlers — ACID-safe
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Processes a verified Paystack webhook for a successful charge.
     * ACID: idempotent — safe to call multiple times with the same reference.
     */
    @Transactional
    public void processPaystackPaymentSuccess(String reference, BigDecimal amount, String currency) {
        Optional<Transaction> txOpt = transactionRepository.findByExternalId(reference);
        if (txOpt.isEmpty()) {
            log.warn("No Paystack tx found for reference={}", reference);
            return;
        }
        Transaction tx = txOpt.get();
        if (isFinalStatus(tx.getStatus())) {
            log.info("Ignoring duplicate Paystack webhook for tx={}", tx.getId());
            return;
        }
        tx.setStatus("COMPLETED");
        walletService.addBalance(tx.getUserId(), tx.getCurrency(), amount != null ? amount : tx.getAmount());
        try {
            transactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate Paystack webhook rejected by DB constraint ref={}", reference);
            throw e;
        }

        // If this TX belongs to a merchant CheckoutSession, settle it
        checkoutSessionRepository.findByReference(reference).ifPresent(session -> {
            session.setStatus("COMPLETED");
            checkoutSessionRepository.save(session);
            creditMerchant(session);
        });
    }

    /**
     * Processes a verified Stripe webhook for payment_intent.succeeded.
     */
    @Transactional
    public void processStripePaymentSuccess(String paymentIntentId, long amountCents, String currency) {
        Optional<Transaction> txOpt = transactionRepository.findByExternalId(paymentIntentId);
        if (txOpt.isEmpty()) {
            log.warn("No Stripe tx found for paymentIntentId={}", paymentIntentId);
            return;
        }
        Transaction tx = txOpt.get();
        if (isFinalStatus(tx.getStatus())) {
            log.info("Ignoring duplicate Stripe webhook for tx={}", tx.getId());
            return;
        }
        BigDecimal amount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
        tx.setStatus("COMPLETED");
        walletService.addBalance(tx.getUserId(), currency.toUpperCase(), amount);
        transactionRepository.save(tx);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private boolean isFinalStatus(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "REFUNDED".equals(status);
    }

    /** Credits merchant wallet minus NylePay fee after checkout session completes. */
    private void creditMerchant(CheckoutSession session) {
        Optional<Merchant> merchantOpt = merchantRepository.findById(session.getMerchantId());
        if (merchantOpt.isEmpty()) return;
        Merchant merchant = merchantOpt.get();
        BigDecimal fee = session.getAmount()
            .multiply(merchant.getFeePercent())
            .divide(BigDecimal.valueOf(100));
        BigDecimal net = session.getAmount().subtract(fee);
        walletService.addBalance(merchant.getUserId(), session.getCurrency(), net);
        log.info("Merchant {} credited {} {} (fee={})", merchant.getId(), net, session.getCurrency(), fee);
    }
}
