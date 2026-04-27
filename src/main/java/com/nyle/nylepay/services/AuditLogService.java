package com.nyle.nylepay.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyle.nylepay.models.AuditLog;
import com.nyle.nylepay.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Centralized audit logging service for all security-sensitive operations.
 *
 * Design:
 *   - Append-only: logs are never modified or deleted
 *   - Async: audit writes do not block the main request thread
 *   - Structured: every log captures userId, eventType, IP, User-Agent, outcome
 *   - Queryable: indexed for compliance exports and fraud investigation
 *
 * CBK AML/CFT requires 7-year retention of financial transaction audit trails.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core logging methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Logs a security event asynchronously (non-blocking).
     */
    @Async
    public void logEvent(Long userId, String eventType, String description,
                         String outcome, HttpServletRequest request,
                         Map<String, Object> metadata) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setEventType(eventType);
            entry.setDescription(description);
            entry.setOutcome(outcome);

            if (request != null) {
                entry.setIpAddress(extractIp(request));
                entry.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
            }

            if (metadata != null && !metadata.isEmpty()) {
                entry.setMetadata(objectMapper.writeValueAsString(metadata));
            }

            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit logging must NEVER crash the main flow
            log.error("Failed to write audit log: eventType={} userId={} error={}",
                      eventType, userId, e.getMessage());
        }
    }

    /**
     * Synchronous version — for critical events that must be persisted
     * before the response is sent (e.g. account lockout).
     */
    public void logEventSync(Long userId, String eventType, String description,
                             String outcome, HttpServletRequest request,
                             Map<String, Object> metadata) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setEventType(eventType);
            entry.setDescription(description);
            entry.setOutcome(outcome);

            if (request != null) {
                entry.setIpAddress(extractIp(request));
                entry.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
            }

            if (metadata != null && !metadata.isEmpty()) {
                entry.setMetadata(objectMapper.writeValueAsString(metadata));
            }

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log (sync): eventType={} userId={} error={}",
                      eventType, userId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience methods for common events
    // ─────────────────────────────────────────────────────────────────────────

    public void logLoginSuccess(Long userId, HttpServletRequest request) {
        logEvent(userId, "AUTH_LOGIN_SUCCESS", "User logged in successfully",
                 "SUCCESS", request, null);
    }

    public void logLoginFailed(Long userId, String email, HttpServletRequest request) {
        logEvent(userId, "AUTH_LOGIN_FAILED",
                 "Failed login attempt for: " + email,
                 "FAILED", request, Map.of("email", email));
    }

    public void logAccountLocked(Long userId, int failedAttempts, HttpServletRequest request) {
        logEventSync(userId, "ACCOUNT_LOCKED",
                     "Account locked after " + failedAttempts + " failed login attempts",
                     "BLOCKED", request, Map.of("failedAttempts", failedAttempts));
    }

    public void logAccountUnlocked(Long userId) {
        logEvent(userId, "ACCOUNT_UNLOCKED",
                 "Account lockout expired — user can login again",
                 "SUCCESS", null, null);
    }

    public void logFraudAlert(Long userId, String reason, Map<String, Object> details,
                              HttpServletRequest request) {
        logEventSync(userId, "FRAUD_ALERT", reason, "ALERT", request, details);
    }

    public void logFraudBlocked(Long userId, String reason, Map<String, Object> details,
                                HttpServletRequest request) {
        logEventSync(userId, "FRAUD_BLOCKED", reason, "BLOCKED", request, details);
    }

    public void logPaymentEvent(Long userId, String eventType, String description,
                                String outcome, Map<String, Object> metadata) {
        logEvent(userId, eventType, description, outcome, null, metadata);
    }

    public void logKycEvent(Long userId, String eventType, String description) {
        logEvent(userId, eventType, description, "SUCCESS", null, null);
    }

    public void logAdminAction(Long adminUserId, String description,
                               HttpServletRequest request, Map<String, Object> metadata) {
        logEvent(adminUserId, "ADMIN_ACTION", description, "SUCCESS", request, metadata);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query methods (for admin dashboard and compliance)
    // ─────────────────────────────────────────────────────────────────────────

    /** Get paginated audit logs for a specific user */
    public Page<AuditLog> getUserAuditLog(Long userId, int page, int size) {
        return auditLogRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

    /** Get recent fraud alerts */
    public List<AuditLog> getRecentFraudAlerts(int limit) {
        return auditLogRepository.findRecentFraudAlerts(PageRequest.of(0, limit));
    }

    /** Count failed logins for lockout logic */
    public long countRecentFailedLogins(Long userId, int windowMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        return auditLogRepository.countFailedLoginsSince(userId, since);
    }

    /** Count events from a specific IP (for IP-based rate analysis) */
    public long countIpEvents(String ip, String eventType, int windowMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        return auditLogRepository.countByIpAndEventTypeSince(ip, eventType, since);
    }

    /** Export user audit trail for compliance (date range) */
    public List<AuditLog> exportUserAuditTrail(Long userId,
                                                LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndTimestampBetween(userId, start, end);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) return xri;
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
