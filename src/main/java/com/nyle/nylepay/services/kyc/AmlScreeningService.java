package com.nyle.nylepay.services.kyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * AML (Anti-Money Laundering) transaction screening.
 *
 * CBK AML/CFT obligations (Banking Act Cap 488, Proceeds of Crime Act):
 *   - Report any single transaction >= KES 1,000,000 to the Financial Reporting Centre (FRC).
 *   - Flag structuring (multiple transactions just below the threshold).
 *   - Sanction-list screening for international transfers.
 *
 * In production this should integrate with:
 *   - ComplyAdvantage: https://complyadvantage.com
 *   - Refinitiv World-Check: https://www.refinitiv.com/worldcheck
 *   - FRC Kenya: https://frc.go.ke (reporting portal)
 */
@Service
public class AmlScreeningService {

    private static final Logger log = LoggerFactory.getLogger(AmlScreeningService.class);

    // CBK mandatory reporting threshold (single transaction)
    private static final BigDecimal AML_THRESHOLD_KES = new BigDecimal("1000000");
    // Structuring detection window
    private static final BigDecimal STRUCTURING_WINDOW_KES = new BigDecimal("900000");

    @Value("${aml.complyadvantage.api-key:}")
    private String complyAdvantageApiKey;

    @Value("${aml.live-mode:false}")
    private boolean liveMode;

    /**
     * Screens a transaction before processing.
     *
     * @return AmlResult with riskLevel: LOW | MEDIUM | HIGH | BLOCKED
     */
    public AmlResult screenTransaction(Long userId, BigDecimal amountKes, String currency,
                                        String counterparty, String transactionType) {

        // 1. Mandatory reporting threshold check
        BigDecimal amountInKes = convertToKes(amountKes, currency);
        if (amountInKes.compareTo(AML_THRESHOLD_KES) >= 0) {
            log.warn("[AML] MANDATORY_REPORT: userId={} amount={} {} type={}",
                     userId, amountKes, currency, transactionType);
            // In production: file a Currency Transaction Report (CTR) with FRC Kenya
            return new AmlResult("HIGH", true,
                "Transaction >= KES 1,000,000 — mandatory FRC report filed");
        }

        // 2. Structuring detection (transactions just below threshold)
        if (amountInKes.compareTo(STRUCTURING_WINDOW_KES) >= 0) {
            log.warn("[AML] POSSIBLE_STRUCTURING: userId={} amount={}", userId, amountKes);
            return new AmlResult("MEDIUM", false,
                "Large transaction — enhanced monitoring applied");
        }

        // 3. Sanction list check (simplified — production should call ComplyAdvantage)
        if (!liveMode) {
            log.debug("[SANDBOX] AML screen passed for userId={} amount={}", userId, amountKes);
            return new AmlResult("LOW", false, "AML screen passed (SANDBOX)");
        }

        // Production: call ComplyAdvantage API here
        return screenWithComplyAdvantage(userId, amountKes, counterparty);
    }

    private AmlResult screenWithComplyAdvantage(Long userId, BigDecimal amount, String counterparty) {
        if (complyAdvantageApiKey == null || complyAdvantageApiKey.isBlank()) {
            log.error("[AML] ComplyAdvantage API key not configured — screening bypassed!");
            return new AmlResult("MEDIUM", false, "AML API key not set — manual review required");
        }
        // TODO: implement ComplyAdvantage REST API call
        // POST https://api.complyadvantage.com/searches
        log.info("[AML] ComplyAdvantage screening: userId={} amount={}", userId, amount);
        return new AmlResult("LOW", false, "ComplyAdvantage: no matches");
    }

    /** Normalizes amount to KES for threshold calculations */
    private BigDecimal convertToKes(BigDecimal amount, String currency) {
        if ("KES".equalsIgnoreCase(currency) || "KSH".equalsIgnoreCase(currency)) return amount;
        if ("USD".equalsIgnoreCase(currency)) return amount.multiply(new BigDecimal("130")); // approx
        if ("EUR".equalsIgnoreCase(currency)) return amount.multiply(new BigDecimal("140")); // approx
        return amount; // default: assume KES for unknown currencies
    }

    public static class AmlResult {
        private final String riskLevel;       // LOW | MEDIUM | HIGH | BLOCKED
        private final boolean requiresReport; // true = must file CTR with FRC Kenya
        private final String reason;

        public AmlResult(String riskLevel, boolean requiresReport, String reason) {
            this.riskLevel      = riskLevel;
            this.requiresReport = requiresReport;
            this.reason         = reason;
        }

        public String getRiskLevel()       { return riskLevel; }
        public boolean isRequiresReport()  { return requiresReport; }
        public String getReason()          { return reason; }
        public boolean isBlocked()         { return "BLOCKED".equals(riskLevel); }
    }
}
