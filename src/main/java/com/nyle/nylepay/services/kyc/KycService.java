package com.nyle.nylepay.services.kyc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/**
 * KYC (Know Your Customer) integration using Smile Identity.
 *
 * Smile Identity is the leading KYC provider in Kenya and Africa.
 * It verifies government-issued IDs (national ID, passport, driving licence)
 * and can optionally perform biometric face-matching.
 *
 * CBK AML/CFT requirement:
 *   - All NylePay users must complete KYC before:
 *     (a) initiating card transactions above KES 70,000/month
 *     (b) registering as a merchant
 *   - Unverified users are limited to KES 70,000 cumulative monthly transactions.
 *
 * Smile Identity docs: https://docs.smileidentity.com/server-to-server/rest
 * Partner sign-up: https://portal.smileidentity.com
 */
@Service
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);
    private static final String SMILE_BASE_URL = "https://api.smileidentity.com/v1";

    @Value("${kyc.smile.partner-id:}")
    private String partnerId;

    @Value("${kyc.smile.api-key:}")
    private String apiKey;

    @Value("${kyc.smile.live-mode:false}")
    private boolean liveMode;

    @Value("${kyc.monthly-limit-unverified-kes:70000}")
    private long monthlyLimitUnverifiedKes;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public KycService(UserRepository userRepository,
                      TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Submits a KYC job to Smile Identity.
     *
     * @param userId       NylePay user ID
     * @param idType       "NATIONAL_ID" | "PASSPORT" | "DRIVERS_LICENSE"
     * @param idNumber     the ID document number
     * @param country      ISO-3166 alpha-2, typically "KE"
     * @param selfieBase64 base64-encoded selfie image (optional for enhanced verification)
     */
    @Transactional
    public Map<String, Object> submitKyc(Long userId, String idType,
                                          String idNumber, String country,
                                          String selfieBase64) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if ("VERIFIED".equals(user.getKycStatus())) {
            return Map.of("status", "ALREADY_VERIFIED", "kycStatus", "VERIFIED");
        }

        user.setKycStatus("PENDING");
        userRepository.save(user);

        if (!liveMode) {
            log.warn("[SANDBOX] KYC submission simulated: userId={} idType={}", userId, idType);
            // In sandbox: auto-approve immediately and generate account number
            String jobRef = "KYC_SANDBOX_" + UUID.randomUUID();
            user.setKycStatus("VERIFIED");
            user.setKycProvider("SANDBOX");
            user.setKycReference(jobRef);
            user.setKycVerifiedAt(LocalDateTime.now());
            generateAccountNumber(user);
            userRepository.save(user);
            return Map.of(
                "status",        "VERIFIED",
                "jobReference",  jobRef,
                "accountNumber", user.getAccountNumber(),
                "message",       "KYC auto-approved (SANDBOX). Account number generated."
            );
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("partner_id",  partnerId);
            body.put("job_type",    1);  // 1 = ID verification + biometric
            body.put("country",     country.toUpperCase());
            body.put("id_type",     idType);
            body.put("id_number",   idNumber);
            body.put("user_id",     "NYLE_" + userId);
            if (selfieBase64 != null && !selfieBase64.isBlank()) {
                body.put("selfie_image", selfieBase64);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " +
                java.util.Base64.getEncoder().encodeToString((partnerId + ":" + apiKey)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            ResponseEntity<String> response = restTemplate.exchange(
                SMILE_BASE_URL + "/id_verification",
                HttpMethod.POST,
                new HttpEntity<>(body.toString(), headers),
                String.class
            );

            Map<String, Object> result = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            String jobRef = String.valueOf(result.getOrDefault("SmileJobID", "UNKNOWN"));

            user.setKycReference(jobRef);
            user.setKycProvider("SMILE_IDENTITY");
            userRepository.save(user);

            return Map.of(
                "status",       "PENDING",
                "jobReference", jobRef,
                "message",      "KYC submitted. You will be notified once verification is complete."
            );
        } catch (Exception e) {
            user.setKycStatus("NONE"); // revert to allow retry
            userRepository.save(user);
            throw new RuntimeException("KYC submission failed: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a Smile Identity result webhook.
     * Updates user kycStatus to VERIFIED or REJECTED.
     */
    @Transactional
    public void processKycWebhook(Map<String, Object> payload) {
        String jobRef = String.valueOf(payload.getOrDefault("SmileJobID",
                                       payload.getOrDefault("job_id", "")));
        String resultCode = String.valueOf(payload.getOrDefault("ResultCode", ""));

        // ResultCode 1220 = ID verification passed
        boolean passed = "1220".equals(resultCode) || "0810".equals(resultCode)
                      || "true".equalsIgnoreCase(String.valueOf(payload.getOrDefault("Actions", "")));

        userRepository.findByKycReference(jobRef).ifPresent(user -> {
            user.setKycStatus(passed ? "VERIFIED" : "REJECTED");
            if (passed) {
                user.setKycVerifiedAt(LocalDateTime.now());
                generateAccountNumber(user);
            }
            userRepository.save(user);
            log.info("KYC result: userId={} status={} accountNumber={} jobRef={}",
                     user.getId(), user.getKycStatus(), user.getAccountNumber(), jobRef);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Guard checks used by CardPaymentService and MerchantService
    // ─────────────────────────────────────────────────────────────────────

    public Map<String, Object> getKycStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return Map.of(
            "kycStatus",    user.getKycStatus() != null ? user.getKycStatus() : "NONE",
            "kycProvider",  user.getKycProvider() != null ? user.getKycProvider() : "",
            "verifiedAt",   user.getKycVerifiedAt() != null ? user.getKycVerifiedAt().toString() : "",
            "monthlyLimit", monthlyLimitUnverifiedKes
        );
    }

    /**
     * Returns true if the user is allowed to transact the given amount.
     *
     * Rules (CBK AML/CFT regulation):
     *   - VERIFIED users: no monthly limit
     *   - Unverified users: cumulative monthly transactions must not exceed KES 70,000
     *   - The check includes the proposed transaction amount
     */
    public boolean canTransact(Long userId, BigDecimal amountKes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verified users have no monthly limit
        if ("VERIFIED".equals(user.getKycStatus())) {
            return true;
        }

        // Calculate start and end of current calendar month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Query actual completed transaction total for this month
        BigDecimal monthlyTotal = transactionRepository.getMonthlyTransactionTotal(
                userId, monthStart, monthEnd);

        BigDecimal projectedTotal = monthlyTotal.add(amountKes);
        BigDecimal limit = BigDecimal.valueOf(monthlyLimitUnverifiedKes);

        if (projectedTotal.compareTo(limit) > 0) {
            log.warn("KYC limit breach: userId={} monthlyTotal={} proposed={} limit={}",
                     userId, monthlyTotal, amountKes, limit);
            return false;
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Account Number Generation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Generates a unique NylePay account number in format NPYXXXXXXXX (11 alphanumeric characters)
     * where X is alphanumeric (uppercase). Only called once on KYC verification.
     *
     * Uses SecureRandom for cryptographic randomness. Retries on collision
     * (unique constraint on User.accountNumber column).
     */
    private void generateAccountNumber(User user) {
        if (user.getAccountNumber() != null) {
            return; // Already assigned
        }

        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excludes I/O/0/1 for readability
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            StringBuilder sb = new StringBuilder("NPY");
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
            }
            String candidate = sb.toString();

            if (userRepository.findByAccountNumber(candidate).isEmpty()) {
                user.setAccountNumber(candidate);
                log.info("Account number generated: userId={} accountNumber={}", user.getId(), candidate);
                return;
            }
            log.warn("Account number collision: {} — retrying (attempt {})", candidate, attempt + 1);
        }

        throw new RuntimeException("Failed to generate unique account number after " + maxAttempts + " attempts");
    }
}
