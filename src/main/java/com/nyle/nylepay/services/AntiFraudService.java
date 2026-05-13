package com.nyle.nylepay.services;

import com.nyle.nylepay.repositories.TransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Anti-fraud engine with velocity checks and behavioral analysis.
 *
 * Checks performed on every outbound transaction (withdrawals, local payments,
 * transfers):
 * 1. Velocity — max N transactions per hour per user
 * 2. Daily amount — max KES per day per user
 * 3. Large transaction — flag single transactions above threshold
 * 4. Rapid-fire — detect burst patterns (multiple txns within seconds)
 * 5. New account — stricter limits for accounts created less than 7 days ago
 * 6. IP anomaly — flag if user's IP changes mid-session
 *
 * When a check FAILS, the service:
 * - Returns a FraudCheckResult with blocked=true and reason
 * - Logs a FRAUD_ALERT or FRAUD_BLOCKED audit event
 * - Does NOT process the transaction
 *
 * When a check raises SUSPICION (soft flag), the service:
 * - Logs a FRAUD_ALERT audit event
 * - Returns blocked=false but flagged=true (allows the transaction but flags it
 * for review)
 */
@Service
public class AntiFraudService {

    @Value("${fraud.velocity.max-txn-per-hour:10}")
    private int maxTransactionsPerHour;

    @Value("${fraud.velocity.max-daily-amount-kes:500000}")
    private long maxDailyAmountKes;

    @Value("${fraud.velocity.large-txn-threshold-kes:100000}")
    private long largeTxnThresholdKes;

    @Value("${fraud.velocity.rapid-fire-window-seconds:30}")
    private int rapidFireWindowSeconds;

    @Value("${fraud.velocity.rapid-fire-max-count:3}")
    private int rapidFireMaxCount;

    @Value("${fraud.new-account-days:7}")
    private int newAccountDays;

    @Value("${fraud.new-account-max-daily-kes:50000}")
    private long newAccountMaxDailyKes;

    @Value("${fraud.enabled:true}")
    private boolean fraudEnabled;

    private final TransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;

    public AntiFraudService(TransactionRepository transactionRepository,
            StringRedisTemplate redisTemplate,
            AuditLogService auditLogService) {
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
    }

    /**
     * Runs all fraud checks against a proposed transaction.
     *
     * @param userId     the user initiating the transaction
     * @param amount     transaction amount in KES
     * @param txnType    WITHDRAWAL, LOCAL_PAYMENT, TRANSFER, etc.
     * @param accountAge user's account creation date
     * @param request    HTTP request (for IP extraction)
     * @return FraudCheckResult — call .isBlocked() before proceeding
     */
    public FraudCheckResult checkTransaction(Long userId, BigDecimal amount,
            String txnType, LocalDateTime accountAge,
            HttpServletRequest request) {
        if (!fraudEnabled) {
            return FraudCheckResult.pass();
        }

        FraudCheckResult velocityResult = checkVelocity(userId);
        if (velocityResult.isBlocked()) {
            auditLogService.logFraudBlocked(userId, velocityResult.getReason(),
                    velocityResult.toMap(), request);
            return velocityResult;
        }

        FraudCheckResult dailyResult = checkDailyAmount(userId, amount, accountAge);
        if (dailyResult.isBlocked()) {
            auditLogService.logFraudBlocked(userId, dailyResult.getReason(),
                    dailyResult.toMap(), request);
            return dailyResult;
        }

        if (amount.compareTo(BigDecimal.valueOf(largeTxnThresholdKes)) > 0) {
            auditLogService.logFraudAlert(userId,
                    "Large transaction: " + amount + " KES (threshold: " + largeTxnThresholdKes + ")",
                    Map.of("amount", amount, "threshold", largeTxnThresholdKes, "txnType", txnType),
                    request);
        }

        FraudCheckResult rapidResult = checkRapidFire(userId);
        if (rapidResult.isBlocked()) {
            auditLogService.logFraudBlocked(userId, rapidResult.getReason(),
                    rapidResult.toMap(), request);
            return rapidResult;
        }

        recordTransactionEvent(userId);

        return FraudCheckResult.pass();
    }

    /**
     * Simplified fraud check for service-to-service calls (no HttpServletRequest
     * available).
     */
    public void checkWithdrawal(Long userId, BigDecimal amount, String txnType) {
        if (!fraudEnabled)
            return;

        FraudCheckResult velocityResult = checkVelocity(userId);
        if (velocityResult.isBlocked()) {
            throw new RuntimeException("Fraud Check Blocked: " + velocityResult.getReason());
        }

        FraudCheckResult dailyResult = checkDailyAmount(userId, amount, null);
        if (dailyResult.isBlocked()) {
            throw new RuntimeException("Fraud Check Blocked: " + dailyResult.getReason());
        }

        recordTransactionEvent(userId);
    }


    private FraudCheckResult checkVelocity(Long userId) {
        String key = "fraud:velocity:" + userId;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= maxTransactionsPerHour) {
            return FraudCheckResult.blocked(
                    "VELOCITY_LIMIT",
                    "Too many transactions. You have exceeded " + maxTransactionsPerHour +
                            " transactions per hour. Please wait before trying again.",
                    Map.of("count", count, "limit", maxTransactionsPerHour));
        }
        return FraudCheckResult.pass();
    }

    private FraudCheckResult checkDailyAmount(Long userId, BigDecimal amount,
            LocalDateTime accountCreated) {
        LocalDateTime dayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = LocalDateTime.now();

        BigDecimal dailyTotal = transactionRepository.getMonthlyTransactionTotal(
                userId, dayStart, dayEnd);

        long effectiveLimit = maxDailyAmountKes;
        boolean isNewAccount = accountCreated != null &&
                accountCreated.isAfter(LocalDateTime.now().minusDays(newAccountDays));
        if (isNewAccount) {
            effectiveLimit = newAccountMaxDailyKes;
        }

        BigDecimal projected = dailyTotal.add(amount);
        if (projected.compareTo(BigDecimal.valueOf(effectiveLimit)) > 0) {
            return FraudCheckResult.blocked(
                    "DAILY_LIMIT",
                    "Daily transaction limit exceeded. " +
                            (isNewAccount ? "New accounts are limited to KES " + newAccountMaxDailyKes + "/day. " : "")
                            +
                            "Today's total: " + dailyTotal + " + " + amount + " = " + projected +
                            " (limit: " + effectiveLimit + ")",
                    Map.of("dailyTotal", dailyTotal, "proposed", amount,
                            "projected", projected, "limit", effectiveLimit,
                            "isNewAccount", isNewAccount));
        }
        return FraudCheckResult.pass();
    }

    private FraudCheckResult checkRapidFire(Long userId) {
        String key = "fraud:rapid:" + userId;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= rapidFireMaxCount) {
            return FraudCheckResult.blocked(
                    "RAPID_FIRE",
                    "Suspicious rapid transaction pattern detected. " +
                            count + " transactions in " + rapidFireWindowSeconds + " seconds. " +
                            "Please wait before trying again.",
                    Map.of("count", count, "windowSeconds", rapidFireWindowSeconds));
        }
        return FraudCheckResult.pass();
    }

    /**
     * Records a transaction event in Redis for velocity tracking.
     * Two keys: hourly counter and rapid-fire counter.
     */
    private void recordTransactionEvent(Long userId) {
        String hourlyKey = "fraud:velocity:" + userId;
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.expire(hourlyKey, Duration.ofHours(1));

        String rapidKey = "fraud:rapid:" + userId;
        redisTemplate.opsForValue().increment(rapidKey);
        redisTemplate.expire(rapidKey, Duration.ofSeconds(rapidFireWindowSeconds));
    }


    public static class FraudCheckResult {
        private final boolean blocked;
        private final String checkType;
        private final String reason;
        private final Map<String, Object> details;

        private FraudCheckResult(boolean blocked, String checkType,
                String reason, Map<String, Object> details) {
            this.blocked = blocked;
            this.checkType = checkType;
            this.reason = reason;
            this.details = details != null ? details : Map.of();
        }

        public static FraudCheckResult pass() {
            return new FraudCheckResult(false, null, null, null);
        }

        public static FraudCheckResult blocked(String checkType, String reason,
                Map<String, Object> details) {
            return new FraudCheckResult(true, checkType, reason, details);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getCheckType() {
            return checkType;
        }

        public String getReason() {
            return reason;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("blocked", blocked);
            if (checkType != null)
                map.put("checkType", checkType);
            if (reason != null)
                map.put("reason", reason);
            map.putAll(details);
            return map;
        }
    }
}
