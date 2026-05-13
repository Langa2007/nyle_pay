package com.nyle.nylepay.services.merchant;

import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.repositories.MerchantRepository;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.services.MpesaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Real-time settlement service.
 *
 * When a CheckoutSession is marked COMPLETED, NylePay calculates the fee
 * and immediately initiates the outbound transfer of funds to the merchant's
 * configured M-Pesa number or bank account.
 *
 * Minimum settlement amount: KES 100 (configurable).
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    private static final BigDecimal MIN_SETTLEMENT = new BigDecimal("100");

    private final MerchantRepository     merchantRepository;
    private final TransactionRepository  transactionRepository;
    private final MpesaService           mpesaService;

    public SettlementService(MerchantRepository merchantRepository,
                              TransactionRepository transactionRepository,
                              MpesaService mpesaService) {
        this.merchantRepository    = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.mpesaService          = mpesaService;
    }

    /**
     * Legacy daily settlement logic — now used only as a manual fallback
     * if real-time settlement fails and funds accumulate in pendingSettlement.
     */
    public void processPendingSettlements() {
        log.info("=== Manual Settlement Sweep Started: {} ===", LocalDateTime.now());

        List<Merchant> merchants = merchantRepository.findAll();
        int processed = 0;
        int skipped = 0;

        for (Merchant merchant : merchants) {
            if (!"ACTIVE".equals(merchant.getStatus())) {
                continue;
            }
            BigDecimal pending = merchant.getPendingSettlement();
            if (pending == null || pending.compareTo(MIN_SETTLEMENT) < 0) {
                skipped++;
                continue;
            }

            try {
                settle(merchant, pending);
                processed++;
            } catch (Exception e) {
                log.error("Settlement failed for merchantId={}: {}", merchant.getId(), e.getMessage(), e);
            }
        }

        log.info("=== Daily Settlement Complete: {} processed, {} skipped ===", processed, skipped);
    }

    /**
     * Calculates fees and settles a merchant immediately for a specific checkout session.
     * Called when a payment is confirmed (M-Pesa, Card, or Wallet).
     */
    @Transactional
    public void settleMerchantRealTime(com.nyle.nylepay.models.CheckoutSession session, BigDecimal grossAmount) {
        merchantRepository.findById(session.getMerchantId()).ifPresent(merchant -> {
            BigDecimal fee = grossAmount.multiply(merchant.getFeePercent())
                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal netAmount = grossAmount.subtract(fee);

            try {
                settle(merchant, netAmount);
                log.info("Merchant {} real-time settlement succeeded: session={} gross={} fee={} net={}",
                        merchant.getId(), session.getReference(), grossAmount, fee, netAmount);
            } catch (Exception e) {
                // If real-time settlement fails (e.g. M-Pesa API down), add to pending for later sweep
                BigDecimal current = merchant.getPendingSettlement() != null
                        ? merchant.getPendingSettlement() : BigDecimal.ZERO;
                merchant.setPendingSettlement(current.add(netAmount));
                merchantRepository.save(merchant);
                log.warn("Merchant {} real-time settlement failed: {}. Added net={} to pending.",
                        merchant.getId(), e.getMessage(), netAmount);
            }
        });
    }

    /**
     * Performs a single merchant settlement.
     * Prefers M-Pesa if settlementPhone is set, otherwise logs a manual bank transfer alert.
     */
    @Transactional
    public void settle(Merchant merchant, BigDecimal amount) throws Exception {
        String ref = "NPY-SET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        if (merchant.getSettlementPhone() != null && !merchant.getSettlementPhone().isBlank()) {
            // M-Pesa B2C payout
            int mpesaAmount = amount.setScale(0, java.math.RoundingMode.FLOOR).intValue();
            mpesaService.initiateB2C(
                    merchant.getSettlementPhone(),
                    new BigDecimal(mpesaAmount),
                    "NylePay Settlement " + ref + " for " + merchant.getBusinessName()
            );
            log.info("Settlement M-Pesa sent: merchantId={} phone={} amount={} ref={}",
                    merchant.getId(), merchant.getSettlementPhone(), mpesaAmount, ref);
        } else if (merchant.getSettlementBankAccount() != null) {
            // Bank transfer — logged for manual dispatch until Flutterwave bank payout is wired
            log.info("[ACTION REQUIRED] Bank settlement pending: merchantId={} bank={} account={} amount={} ref={}",
                    merchant.getId(), merchant.getSettlementBankName(),
                    merchant.getSettlementBankAccount(), amount, ref);
        } else {
            log.warn("Merchant {} has no settlement destination configured — skipping.", merchant.getId());
            return;
        }

        // Record settlement transaction
        Transaction tx = new Transaction();
        tx.setUserId(merchant.getUserId());
        tx.setType("SETTLEMENT");
        tx.setAmount(amount);
        tx.setCurrency(merchant.getSettlementCurrency());
        tx.setStatus("COMPLETED");
        tx.setTransactionCode(ref);
        tx.setTimestamp(LocalDateTime.now());
        tx.setMetadata("{\"merchantId\":" + merchant.getId() + ",\"business\":\"" + merchant.getBusinessName() + "\"}");
        transactionRepository.save(tx);

        // Zero out pending settlement
        merchant.setPendingSettlement(BigDecimal.ZERO);
        merchantRepository.save(merchant);
    }
}
