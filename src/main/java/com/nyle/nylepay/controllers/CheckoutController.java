package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.exceptions.NylePayException;
import com.nyle.nylepay.models.CheckoutSession;
import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.repositories.CheckoutSessionRepository;
import com.nyle.nylepay.repositories.MerchantRepository;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.WalletService;
import com.nyle.nylepay.services.merchant.SettlementService;
import com.nyle.nylepay.services.merchant.WebhookDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Public-facing checkout endpoints for the NylePay hosted payment page.
 *
 * These endpoints are intentionally unauthenticated (whitelisted in SecurityConfig).
 * They are accessed by customers visiting a merchant's payment link.
 *
 * Route prefix: /api/merchant/pay/{reference}
 */
@RestController
@RequestMapping("/api/merchant/pay")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final MerchantRepository merchantRepository;
    private final MpesaService mpesaService;
    private final WalletService walletService;
    private final WebhookDeliveryService webhookDeliveryService;
    private final SettlementService settlementService;

    public CheckoutController(
            CheckoutSessionRepository checkoutSessionRepository,
            MerchantRepository merchantRepository,
            MpesaService mpesaService,
            WalletService walletService,
            WebhookDeliveryService webhookDeliveryService,
            SettlementService settlementService) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.merchantRepository        = merchantRepository;
        this.mpesaService              = mpesaService;
        this.walletService             = walletService;
        this.webhookDeliveryService    = webhookDeliveryService;
        this.settlementService         = settlementService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Load checkout session details (GET)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/merchant/pay/{reference}
     * Returns the session info needed to render the checkout page.
     * Called by checkout.js on page load.
     */
    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSession(@PathVariable String reference) {
        try {
            CheckoutSession session = checkoutSessionRepository.findByReference(reference)
                    .orElseThrow(() -> new NylePayException("Payment link not found or has expired."));

            if ("COMPLETED".equals(session.getStatus())) {
                return ResponseEntity.ok(ApiResponse.success("Payment already completed", Map.of(
                        "status", "COMPLETED",
                        "reference", reference
                )));
            }

            if ("EXPIRED".equals(session.getStatus()) ||
                    (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now()))) {
                // Auto-expire
                if (!"EXPIRED".equals(session.getStatus())) {
                    session.setStatus("EXPIRED");
                    checkoutSessionRepository.save(session);
                }
                throw new NylePayException("This payment link has expired. Please contact the merchant for a new link.");
            }

            Merchant merchant = merchantRepository.findById(session.getMerchantId())
                    .orElseThrow(() -> new NylePayException("Merchant account not found."));

            return ResponseEntity.ok(ApiResponse.success("Session loaded", Map.of(
                    "reference",     session.getReference(),
                    "amount",        session.getAmount(),
                    "currency",      session.getCurrency(),
                    "description",   session.getDescription() != null ? session.getDescription() : "",
                    "merchantName",  merchant.getBusinessName(),
                    "status",        session.getStatus(),
                    "expiresAt",     session.getExpiresAt() != null ? session.getExpiresAt().toString() : "",
                    "redirectUrl",   session.getRedirectUrl() != null ? session.getRedirectUrl() : ""
            )));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error loading checkout session {}: {}", reference, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Unable to load payment details. Please try again."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Initiate payment (POST)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/merchant/pay/{reference}/initiate
     *
     * Body for M-Pesa:
     *   { "method": "MPESA", "phone": "254712345678", "email": "customer@email.com" }
     *
     * Body for Card (Paystack):
     *   { "method": "CARD", "email": "customer@email.com" }
     *
     * Body for NylePay Wallet:
     *   { "method": "NYLEPAY_WALLET", "userId": 123, "token": "JWT_token_here" }
     */
    @PostMapping("/{reference}/initiate")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayment(
            @PathVariable String reference,
            @RequestBody Map<String, Object> body) {
        try {
            CheckoutSession session = checkoutSessionRepository.findByReference(reference)
                    .orElseThrow(() -> new NylePayException("Payment link not found."));

            if (!"PENDING".equals(session.getStatus())) {
                throw new NylePayException("This payment link is no longer active (status: " + session.getStatus() + ").");
            }

            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
                session.setStatus("EXPIRED");
                checkoutSessionRepository.save(session);
                throw new NylePayException("This payment link has expired.");
            }

            String method = (String) body.getOrDefault("method", "MPESA");
            String email  = (String) body.getOrDefault("email", "");
            String phone  = (String) body.getOrDefault("phone", "");

            session.setPaymentMethod(method);
            session.setCustomerEmail(email);
            session.setCustomerPhone(phone);
            checkoutSessionRepository.save(session);

            switch (method.toUpperCase()) {
                case "MPESA" -> {
                    return initiateMpesa(session, phone);
                }
                case "CARD" -> {
                    return initiateCard(session, email);
                }
                case "NYLEPAY_WALLET" -> {
                    Long userId = Long.valueOf(body.get("userId").toString());
                    return initiateWallet(session, userId);
                }
                default -> throw new NylePayException("Unsupported payment method: " + method);
            }

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Payment initiation failed for {}: {}", reference, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment could not be initiated. Please try again."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Status check / M-Pesa confirmation polling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/merchant/pay/{reference}/status
     * Polling endpoint — checkout.js calls this every 5s after M-Pesa STK Push.
     */
    @GetMapping("/{reference}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sessionStatus(@PathVariable String reference) {
        try {
            CheckoutSession session = checkoutSessionRepository.findByReference(reference)
                    .orElseThrow(() -> new NylePayException("Session not found."));

            return ResponseEntity.ok(ApiResponse.success("Status retrieved", Map.of(
                    "reference", reference,
                    "status",    session.getStatus()
            )));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Status check failed for {}: {}", reference, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Unable to check status."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Map<String, Object>>> initiateMpesa(
            CheckoutSession session, String phone) throws Exception {

        if (phone == null || !phone.matches("^2547[0-9]{8}$")) {
            throw new NylePayException("Please enter a valid Safaricom number (format: 2547XXXXXXXX).");
        }

        // STK Push — amount must be whole shillings for M-Pesa
        BigDecimal mpesaAmount = session.getAmount().setScale(0, RoundingMode.CEILING);
        Map<String, Object> stkResult = mpesaService.stkPush(
                phone,
                mpesaAmount,
                session.getReference()
        );

        session.setProvider("MPESA");
        Object checkoutId = stkResult.get("CheckoutRequestID");
        if (checkoutId != null) {
            session.setProviderIntentId(checkoutId.toString());
        }
        checkoutSessionRepository.save(session);

        log.info("M-Pesa STK Push sent: ref={} phone={} amount={}", session.getReference(), phone, mpesaAmount);
        return ResponseEntity.ok(ApiResponse.success(
                "M-Pesa payment request sent. Please check your phone and enter your PIN.",
                Map.of(
                        "method",    "MPESA",
                        "reference", session.getReference(),
                        "message",   "Check your phone for an M-Pesa prompt."
                )));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> initiateCard(
            CheckoutSession session, String email) {

        // For Paystack card payments we return the public key and amount so
        // checkout.js can open the Paystack inline popup directly.
        // The popup redirects to /api/merchant/pay/{ref}/status after completion.
        session.setProvider("PAYSTACK");
        checkoutSessionRepository.save(session);

        return ResponseEntity.ok(ApiResponse.success("Proceed to card payment", Map.of(
                "method",    "CARD",
                "reference", session.getReference(),
                "email",     email,
                "amount",    session.getAmount().multiply(BigDecimal.valueOf(100)).intValue(), // Paystack uses kobo/cents
                "currency",  session.getCurrency()
        )));
    }

    @Transactional
    private ResponseEntity<ApiResponse<Map<String, Object>>> initiateWallet(
            CheckoutSession session, Long userId) {

        // Deduct from NylePay user wallet and credit merchant immediately
        BigDecimal amount = session.getAmount();
        String currency = session.getCurrency();

        try {
            walletService.subtractBalance(userId, currency, amount);
        } catch (Exception e) {
            throw new NylePayException("Insufficient NylePay wallet balance.");
        }

        // Credit merchant's pending settlement (minus fee)
        creditMerchant(session, amount);

        session.setStatus("COMPLETED");
        session.setProvider("NYLEPAY_WALLET");
        session.setCustomerId(userId);
        checkoutSessionRepository.save(session);

        Merchant merchant = merchantRepository.findById(session.getMerchantId()).orElseThrow();
        webhookDeliveryService.deliverPaymentSuccess(merchant, session);

        log.info("NylePay wallet payment completed: ref={} userId={} amount={} {}",
                session.getReference(), userId, amount, currency);

        return ResponseEntity.ok(ApiResponse.success("Payment successful!", Map.of(
                "method",    "NYLEPAY_WALLET",
                "reference", session.getReference(),
                "status",    "COMPLETED"
        )));
    }

    /**
     * Credits the merchant's pendingSettlement after deducting NylePay's fee.
     * Called when a checkout session is confirmed as COMPLETED.
     */
    public void creditMerchant(CheckoutSession session, BigDecimal amount) {
        merchantRepository.findById(session.getMerchantId()).ifPresent(merchant -> {
            BigDecimal fee = amount.multiply(merchant.getFeePercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal netAmount = amount.subtract(fee);
            
            try {
                // Real-time immediate settlement
                settlementService.settle(merchant, netAmount);
                log.info("Merchant {} real-time settlement succeeded: gross={} fee={} net={}",
                        merchant.getId(), amount, fee, netAmount);
            } catch (Exception e) {
                // Fallback: if instant transfer fails (e.g., M-Pesa is down), keep in pending
                BigDecimal current = merchant.getPendingSettlement() != null
                        ? merchant.getPendingSettlement() : BigDecimal.ZERO;
                merchant.setPendingSettlement(current.add(netAmount));
                merchantRepository.save(merchant);
                log.warn("Merchant {} real-time settlement failed: {}. Added net={} to pending.",
                        merchant.getId(), e.getMessage(), netAmount);
            }
        });
    }
}
