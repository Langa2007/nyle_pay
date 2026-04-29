package com.nyle.nylepay.controllers;

import com.nyle.nylepay.models.CheckoutSession;
import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.models.Refund;
import com.nyle.nylepay.services.merchant.MerchantService;
import com.nyle.nylepay.services.merchant.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Merchant gateway REST endpoints.
 *
 * Authentication: all endpoints require JWT (Bearer token).
 * Merchants authenticate with their NylePay user JWT, not the merchant secret key.
 * The merchant secret key is used for server-to-server API calls (future feature).
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final RefundService   refundService;

    public MerchantController(MerchantService merchantService, RefundService refundService) {
        this.merchantService = merchantService;
        this.refundService   = refundService;
    }

    /**
     * POST /api/merchant/register
     * Registers the authenticated user as a NylePay merchant.
     *
     * Body: {
     *   "businessName": "Acme Ltd",
     *   "businessEmail": "payments@acme.com",
     *   "webhookUrl": "https://acme.com/nylepay-webhook"
     * }
     *
     * Response includes secretKey — shown ONCE, store it safely.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = resolveUserId(auth);
        String businessName  = (String) body.get("businessName");
        String businessEmail = (String) body.get("businessEmail");
        String webhookUrl    = (String) body.get("webhookUrl");

        if (businessName == null || businessName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "businessName is required"));
        }

        Map<String, Object> result = merchantService.registerMerchant(
            userId, businessName, businessEmail, webhookUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/merchant/profile
     * Returns the merchant profile for the authenticated user.
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(Authentication auth) {
        Long userId = resolveUserId(auth);
        Merchant merchant = merchantService.getMerchantByUserId(userId);
        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("merchantId",         merchant.getId());
        resp.put("businessName",       merchant.getBusinessName());
        resp.put("businessEmail",      merchant.getBusinessEmail());
        resp.put("publicKey",          merchant.getPublicKey());
        resp.put("status",             merchant.getStatus());
        resp.put("kycStatus",          merchant.getKycStatus());
        resp.put("feePercent",         merchant.getFeePercent());
        resp.put("pendingSettlement",  merchant.getPendingSettlement() != null ? merchant.getPendingSettlement() : java.math.BigDecimal.ZERO);
        resp.put("settlementCurrency", merchant.getSettlementCurrency());
        resp.put("settlementPhone",    merchant.getSettlementPhone() != null ? merchant.getSettlementPhone() : "");
        resp.put("webhookUrl",         merchant.getWebhookUrl() != null ? merchant.getWebhookUrl() : "");
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/merchant/payment-link
     * Creates a payment link customers can use to pay the merchant.
     *
     * Body: {
     *   "amount": 1500.00,
     *   "currency": "KES",
     *   "description": "Order #1234",
     *   "redirectUrl": "https://mystore.com/thank-you",
     *   "expiryMinutes": 60
     * }
     */
    @PostMapping("/payment-link")
    public ResponseEntity<Map<String, Object>> createPaymentLink(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = resolveUserId(auth);
        Merchant merchant = merchantService.getMerchantByUserId(userId);

        BigDecimal amount    = new BigDecimal(body.get("amount").toString());
        String currency      = (String) body.getOrDefault("currency", "KES");
        String description   = (String) body.getOrDefault("description", "NylePay Payment");
        String redirectUrl   = (String) body.getOrDefault("redirectUrl", "");
        int expiryMinutes    = Integer.parseInt(body.getOrDefault("expiryMinutes", "60").toString());

        Map<String, Object> result = merchantService.createPaymentLink(
            merchant.getId(), amount, currency, description, redirectUrl, expiryMinutes);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/merchant/payments
     * Returns all payment sessions for the authenticated merchant.
     */
    @GetMapping("/payments")
    public ResponseEntity<List<Map<String, Object>>> payments(Authentication auth) {
        Long userId = resolveUserId(auth);
        Merchant merchant = merchantService.getMerchantByUserId(userId);
        List<CheckoutSession> sessions = merchantService.getMerchantSessions(merchant.getId());
        List<Map<String, Object>> result = sessions.stream().map(s -> Map.<String, Object>of(
            "id",          s.getId(),
            "reference",   s.getReference(),
            "amount",      s.getAmount(),
            "currency",    s.getCurrency(),
            "status",      s.getStatus(),
            "description", s.getDescription() != null ? s.getDescription() : "",
            "createdAt",   s.getCreatedAt().toString()
        )).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/merchant/refund
     * Issues a refund for a completed transaction.
     *
     * Body: {
     *   "transactionId": 123,
     *   "amount": 500.00,      — optional, omit for full refund
     *   "reason": "CUSTOMER_REQUEST"
     * }
     */
    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> refund(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = resolveUserId(auth);
        Merchant merchant = merchantService.getMerchantByUserId(userId);

        Long transactionId = Long.valueOf(body.get("transactionId").toString());
        BigDecimal amount  = body.containsKey("amount")
                           ? new BigDecimal(body.get("amount").toString()) : null;
        String reason      = (String) body.getOrDefault("reason", "CUSTOMER_REQUEST");

        Refund refund = refundService.initiateRefund(transactionId, amount, reason, merchant.getId());
        return ResponseEntity.ok(Map.of(
            "refundId",   refund.getId(),
            "status",     refund.getStatus(),
            "amount",     refund.getAmount(),
            "currency",   refund.getCurrency(),
            "refundRef",  refund.getProviderRefundId()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Settlement Account Setup
    // ─────────────────────────────────────────────────────────────────────

    /**
     * POST /api/merchant/settlement-account
     * Links a M-Pesa number or bank account as the merchant's payout destination.
     *
     * Body (M-Pesa): { "type": "MPESA", "phone": "254712345678" }
     * Body (Bank):   { "type": "BANK",  "bankName": "KCB", "accountNumber": "1234567890", "currency": "KES" }
     */
    @PostMapping("/settlement-account")
    public ResponseEntity<Map<String, Object>> updateSettlementAccount(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = resolveUserId(auth);
        com.nyle.nylepay.models.Merchant merchant = merchantService.getMerchantByUserId(userId);

        String type = (String) body.getOrDefault("type", "MPESA");

        if ("MPESA".equalsIgnoreCase(type)) {
            String phone = (String) body.get("phone");
            if (phone == null || !phone.matches("^2547[0-9]{8}$")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid M-Pesa number. Use format: 2547XXXXXXXX"));
            }
            merchant.setSettlementPhone(phone);
            merchant.setSettlementCurrency("KES");
        } else if ("BANK".equalsIgnoreCase(type)) {
            String bankName   = (String) body.get("bankName");
            String accountNo  = (String) body.get("accountNumber");
            String currency   = (String) body.getOrDefault("currency", "KES");
            if (bankName == null || accountNo == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "bankName and accountNumber are required"));
            }
            merchant.setSettlementBankName(bankName);
            merchant.setSettlementBankAccount(accountNo);
            merchant.setSettlementCurrency(currency);
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "type must be MPESA or BANK"));
        }

        merchantService.saveMerchant(merchant);
        return ResponseEntity.ok(Map.of(
                "message", "Settlement account updated successfully.",
                "type",    type
        ));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Admin — activate merchant after KYC (admin-only)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * POST /api/merchant/activate/{merchantId}
     * Admin endpoint to activate a merchant after KYC verification.
     */
    @PostMapping("/activate/{merchantId}")
    public ResponseEntity<Map<String, Object>> activate(
            @PathVariable Long merchantId,
            Authentication auth) {
        Merchant merchant = merchantService.activateMerchant(merchantId);
        return ResponseEntity.ok(Map.of(
            "merchantId", merchant.getId(),
            "status",     merchant.getStatus()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────

    private Long resolveUserId(Authentication auth) {
        // JWT subject should be userId in production; using email-based lookup as fallback
        if (auth == null) throw new RuntimeException("Not authenticated");
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot resolve userId from JWT subject: " + auth.getName());
        }
    }
}
