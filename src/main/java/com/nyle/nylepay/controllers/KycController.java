package com.nyle.nylepay.controllers;

import com.nyle.nylepay.services.kyc.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * KYC (Know Your Customer) endpoints.
 *
 * CBK requirement: all users must complete KYC to:
 *   - Initiate card transactions above KES 70,000/month
 *   - Register as a merchant
 *   - Transact above KES 1,000,000 (AML threshold)
 *
 * The KYC webhook endpoint is permit-all (no JWT) because Smile Identity
 * calls it directly from their servers after completing verification.
 */
@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    /**
     * GET /api/kyc/status
     * Returns the KYC status for the authenticated user.
     *
     * Response: {
     *   "kycStatus": "NONE | PENDING | VERIFIED | REJECTED",
     *   "verifiedAt": "2026-04-22T...",
     *   "monthlyLimit": 70000
     * }
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(kycService.getKycStatus(userId));
    }

    /**
     * POST /api/kyc/submit
     * Submits a KYC verification request to Smile Identity.
     *
     * Body: {
     *   "idType":       "NATIONAL_ID",     // or "PASSPORT" | "DRIVERS_LICENSE"
     *   "idNumber":     "12345678",
     *   "country":      "KE",
     *   "selfieBase64": "data:image/jpeg;base64,..."  // optional, enables biometric match
     * }
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = resolveUserId(auth);

        String idType       = (String) body.get("idType");
        String idNumber     = (String) body.get("idNumber");
        String country      = (String) body.getOrDefault("country", "KE");
        String selfieBase64 = (String) body.get("selfieBase64");

        if (idType == null || idNumber == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "idType and idNumber are required"));
        }

        Map<String, Object> result = kycService.submitKyc(
            userId, idType, idNumber, country, selfieBase64);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/kyc/webhook
     * Receives KYC result callbacks from Smile Identity.
     *
     * Configure this URL in Smile Identity portal:
     *   https://portal.smileidentity.com → Webhooks → Callback URL
     *
     * This endpoint is permit-all (no JWT) — authentication is by
     * Smile Identity's HMAC signature (future enhancement).
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> kycWebhook(@RequestBody Map<String, Object> payload) {
        try {
            kycService.processKycWebhook(payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────

    private Long resolveUserId(Authentication auth) {
        if (auth == null) throw new RuntimeException("Not authenticated");
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot resolve userId from JWT: " + auth.getName());
        }
    }
}
