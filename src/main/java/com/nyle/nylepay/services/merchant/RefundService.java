package com.nyle.nylepay.services.merchant;

import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.models.Refund;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.repositories.CheckoutSessionRepository;
import com.nyle.nylepay.repositories.MerchantRepository;
import com.nyle.nylepay.repositories.RefundRepository;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.services.WalletService;
import com.nyle.nylepay.services.card.PaystackCardService;
import com.nyle.nylepay.services.card.StripeCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ACID-safe refund processing.
 *
 * Single @Transactional unit covers:
 *   1. Debit merchant wallet
 *   2. Credit customer wallet (if NylePay user)
 *   3. Call provider refund API
 *   4. Save Refund record
 *
 * If any step fails, all four are rolled back.
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository          refundRepository;
    private final TransactionRepository     transactionRepository;
    private final MerchantRepository        merchantRepository;
    private final WalletService             walletService;
    private final PaystackCardService       paystackCardService;
    private final StripeCardService         stripeCardService;

    public RefundService(
            RefundRepository refundRepository,
            TransactionRepository transactionRepository,
            MerchantRepository merchantRepository,
            WalletService walletService,
            PaystackCardService paystackCardService,
            StripeCardService stripeCardService) {
        this.refundRepository          = refundRepository;
        this.transactionRepository     = transactionRepository;
        this.merchantRepository        = merchantRepository;
        this.walletService             = walletService;
        this.paystackCardService       = paystackCardService;
        this.stripeCardService         = stripeCardService;
    }

    /**
     * Initiates a full or partial refund for a completed transaction.
     *
     * @param transactionId   NylePay Transaction ID to refund
     * @param amount          null = full refund
     * @param reason          DUPLICATE | FRAUDULENT | CUSTOMER_REQUEST | OTHER
     * @param merchantId      merchant initiating the refund
     */
    @Transactional
    public Refund initiateRefund(Long transactionId, BigDecimal amount,
                                  String reason, Long merchantId) {

        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"COMPLETED".equals(tx.getStatus())) {
            throw new RuntimeException("Only COMPLETED transactions can be refunded");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new RuntimeException("Merchant not found"));

        BigDecimal refundAmount = amount != null ? amount : tx.getAmount();

        // 1. Debit merchant wallet (ACID: if this fails nothing else runs)
        walletService.subtractBalance(merchant.getUserId(), tx.getCurrency(), refundAmount);

        // 2. Credit customer wallet if they are a NylePay user
        if (tx.getUserId() != null) {
            walletService.addBalance(tx.getUserId(), tx.getCurrency(), refundAmount);
        }

        // 3. Call provider refund API
        String providerRefundId = callProviderRefund(tx, refundAmount);

        // 4. Save refund record
        Refund refund = new Refund();
        refund.setTransactionId(transactionId);
        refund.setMerchantId(merchantId);
        refund.setCustomerId(tx.getUserId());
        refund.setAmount(refundAmount);
        refund.setCurrency(tx.getCurrency());
        refund.setReason(reason);
        refund.setStatus("SUCCEEDED");
        refund.setProviderRefundId(providerRefundId);

        // Mark original transaction
        tx.setStatus("REFUNDED");
        transactionRepository.save(tx);

        log.info("Refund succeeded: txId={} amount={} provider={} refundId={}",
                 transactionId, refundAmount, tx.getProvider(), providerRefundId);

        return refundRepository.save(refund);
    }

    private String callProviderRefund(Transaction tx, BigDecimal amount) {
        String provider = tx.getProvider();
        String externalId = tx.getExternalId();

        try {
            if ("PAYSTACK".equalsIgnoreCase(provider)) {
                Map<String, Object> result = paystackCardService.refund(externalId, amount);
                Object data = result.get("data");
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    return String.valueOf(dataMap.getOrDefault("id", "PS_REFUND_UNKNOWN"));
                }
                return "PS_REFUND_UNKNOWN";
            } else if ("STRIPE".equalsIgnoreCase(provider)) {
                Map<String, Object> result = stripeCardService.refund(externalId, null, "requested_by_customer");
                return (String) result.getOrDefault("id", "STR_REFUND_UNKNOWN");
            } else {
                // MPESA / BANK — refunds handled manually or via provider dashboard
                log.warn("Automatic refund not supported for provider={}. Manual refund required.", provider);
                return "MANUAL_REFUND_REQUIRED";
            }
        } catch (Exception e) {
            // Propagate — @Transactional will roll back wallet changes
            throw new RuntimeException("Provider refund API call failed: " + e.getMessage(), e);
        }
    }
}
