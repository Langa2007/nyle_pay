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
 * Automated daily settlement service.
 *
 * At 22:00 EAT every day, NylePay sweeps all merchants with a pending
 * settlement balance and sends the funds to their configured M-Pesa number
 * or bank account, recording a SETTLEMENT transaction for the audit trail.
 *
 * NylePay's fee has already been deducted when the CheckoutController credits
 * the merchant (gross - fee = pendingSettlement). This job simply initiates
 * the outbound transfer of whatever is pending.
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
     * Scheduled daily at 22:00 EAT (19:00 UTC).
     * Processes all ACTIVE merchants with a pending settlement balance >= KES 100.
     */
    @Scheduled(cron = "0 0 19 * * *") // 19:00 UTC = 22:00 EAT
    public void runDailySettlement() {
        log.info("=== Daily Settlement Run Started: {} ===", LocalDateTime.now());

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
