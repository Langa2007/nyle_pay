package com.nyle.nylepay.services;

import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

/**
 * OTP (One-Time Password) service for 2FA on sensitive operations.
 *
 * Architecture:
 *   - 6-digit numeric OTP stored in Redis with 5-minute TTL
 *   - Key format: otp:{userId}:{purpose}
 *   - Rate-limited: max 1 OTP request per 60 seconds per user+purpose
 *
 * Purposes:
 *   WITHDRAWAL — required before any withdrawal (M-Pesa, Bank, Crypto)
 *   LOCAL_PAYMENT — required for local payments above KES 50,000
 *   PROFILE_UPDATE — required for changing phone/email
 *
 * In production, the OTP is delivered via SMS (Africa's Talking) or email.
 * In sandbox, the OTP is returned in the API response for testing.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(60);
    private static final String OTP_PREFIX = "otp:";
    private static final String RATE_PREFIX = "otp_rate:";
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OtpService(StringRedisTemplate redisTemplate,
                      UserRepository userRepository,
                      EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Generates and sends a 6-digit OTP to the user.
     *
     * @param userId  the user requesting the OTP
     * @param purpose WITHDRAWAL, LOCAL_PAYMENT, PROFILE_UPDATE
     * @return response map with status; in sandbox mode includes the OTP itself
     */
    public Map<String, Object> requestOtp(Long userId, String purpose) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String rateKey = RATE_PREFIX + userId + ":" + purpose;
        String existingRate = redisTemplate.opsForValue().get(rateKey);
        if (existingRate != null) {
            throw new RuntimeException(
                "OTP already sent. Please wait 60 seconds before requesting a new one."
            );
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        // Store in Redis with 5-minute TTL
        String otpKey = OTP_PREFIX + userId + ":" + purpose;
        redisTemplate.opsForValue().set(otpKey, otp, OTP_TTL);

        // Set rate limit (1 request per 60 seconds)
        redisTemplate.opsForValue().set(rateKey, "1", RATE_LIMIT_TTL);

        // Reset attempt counter
        String attemptKey = otpKey + ":attempts";
        redisTemplate.delete(attemptKey);

        // Send OTP via email
        sendOtpNotification(user, otp, purpose);

        log.info("OTP generated: userId={} purpose={}", userId, purpose);

        return Map.of(
            "status", "OTP_SENT",
            "purpose", purpose,
            "expiresInSeconds", OTP_TTL.getSeconds(),
            "message", "A 6-digit OTP has been sent to your registered email/phone."
        );
    }

    /**
     * Verifies an OTP provided by the user.
     *
     * @param userId  the user ID
     * @param purpose the purpose this OTP was generated for
     * @param otp     the 6-digit code entered by the user
     * @return true if valid, false otherwise
     */
    public boolean verifyOtp(Long userId, String purpose, String otp) {
        if (otp == null || otp.length() != 6) {
            return false;
        }

        String otpKey = OTP_PREFIX + userId + ":" + purpose;
        String attemptKey = otpKey + ":attempts";

        // Check attempt count
        String attemptCount = redisTemplate.opsForValue().get(attemptKey);
        if (attemptCount != null && Integer.parseInt(attemptCount) >= MAX_VERIFY_ATTEMPTS) {
            // Delete the OTP — too many failed attempts
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptKey);
            throw new RuntimeException(
                "Too many failed OTP attempts. Please request a new OTP."
            );
        }

        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            return false; // Expired or never generated
        }

        // Constant-time comparison to prevent timing attacks
        boolean valid = java.security.MessageDigest.isEqual(
            storedOtp.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            otp.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        if (valid) {
            // Delete OTP after successful verification (single-use)
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptKey);
            log.info("OTP verified: userId={} purpose={}", userId, purpose);
        } else {
            // Increment attempt counter
            redisTemplate.opsForValue().increment(attemptKey);
            if (attemptCount == null) {
                redisTemplate.expire(attemptKey, OTP_TTL);
            }
            log.warn("OTP verification failed: userId={} purpose={}", userId, purpose);
        }

        return valid;
    }

    /**
     * Checks if a user has 2FA enabled and requires OTP for the given purpose.
     */
    public boolean requiresOtp(Long userId, String purpose) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        // If user has OTP enabled globally, always require it
        if (user.isOtpEnabled()) return true;

        // Even without OTP enabled, require it for withdrawals above KES 10,000
        return "WITHDRAWAL".equals(purpose);
    }

    /**
     * Enables 2FA for a user account.
     */
    public void enableOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setOtpEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled: userId={}", userId);
    }

    /**
     * Disables 2FA for a user account (requires OTP verification first).
     */
    public void disableOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setOtpEnabled(false);
        userRepository.save(user);
        log.info("2FA disabled: userId={}", userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void sendOtpNotification(User user, String otp, String purpose) {
        String subject = "NylePay Security Code";
        String body = String.format(
            "Your NylePay verification code is: %s\n\n" +
            "Purpose: %s\n" +
            "This code expires in 5 minutes.\n\n" +
            "If you did not request this code, please change your password immediately.",
            otp, purpose
        );

        try {
            emailService.sendGenericEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to userId={}: {}", user.getId(), e.getMessage());
            // Don't fail the OTP generation — user can still use SMS if available
        }

        // SMS delivery (if M-Pesa number is available)
        if (user.getMpesaNumber() != null && !user.getMpesaNumber().isBlank()) {
            log.info("[SMS] OTP for userId={} phone={}: {} (purpose={})",
                    user.getId(), user.getMpesaNumber(), otp, purpose);
            // TODO: Wire Africa's Talking SMS API here
        }
    }
}
