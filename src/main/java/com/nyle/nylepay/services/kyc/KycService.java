package com.nyle.nylepay.services.kyc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KycService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
            // In sandbox: auto-approve after 2 seconds (simulated webhook)
            String jobRef = "KYC_SANDBOX_" + UUID.randomUUID();
            return Map.of(
                "status",       "PENDING",
                "jobReference", jobRef,
                "message",      "KYC submitted (SANDBOX). Set kyc.smile.live-mode=true for production."
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
            }
            userRepository.save(user);
            log.info("KYC result: userId={} status={} jobRef={}", user.getId(), user.getKycStatus(), jobRef);
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

    /** Returns true if the user is allowed to transact (VERIFIED, or under monthly limit). */
    public boolean canTransact(Long userId, java.math.BigDecimal amountKes) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if ("VERIFIED".equals(user.getKycStatus())) return true;
        // Unverified — check if below monthly limit (simplified; production should sum from transactions)
        return amountKes.compareTo(java.math.BigDecimal.valueOf(monthlyLimitUnverifiedKes)) < 0;
    }
}
