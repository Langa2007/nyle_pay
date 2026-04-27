package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.LocalPaymentRequest;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.TransactionService;
import com.nyle.nylepay.services.WalletService;
import com.nyle.nylepay.services.kyc.KycService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles local Kenyan payments: Till, Paybill, Pochi la Biashara, and Send Money.
 *
 * Flow for each payment type:
 *   1. Validate KYC status and monthly limits
 *   2. Debit user's KSH wallet balance
 *   3. Dispatch to Safaricom (B2B for Till/Paybill/Pochi, B2C for Send Money)
 *   4. Record PENDING transaction (finalized on callback)
 */
@RestController
@RequestMapping("/api/payments/local")
public class LocalPaymentController {

    private static final Logger log = LoggerFactory.getLogger(LocalPaymentController.class);

    private final MpesaService mpesaService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final KycService kycService;

    public LocalPaymentController(MpesaService mpesaService,
                                  TransactionService transactionService,
                                  WalletService walletService,
                                  KycService kycService) {
        this.mpesaService = mpesaService;
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.kycService = kycService;
    }

    /**
     * POST /api/payments/local/till
     * Pay to a Till number (Lipa na M-Pesa Buy Goods).
     */
    @PostMapping("/till")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payToTill(
            @Valid @RequestBody LocalPaymentRequest request) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "TILL");

            if (request.getTillNumber() == null || request.getTillNumber().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("tillNumber is required for Till payments"));
            }

            // Debit wallet
            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            // Dispatch B2B to Safaricom
            Map<String, Object> mpesaResponse = mpesaService.payToTill(
                request.getTillNumber(),
                request.getAmount(),
                request.getDescription()
            );

            // Record transaction
            var transaction = transactionService.createLocalPayment(
                request.getUserId(),
                "TILL",
                request.getAmount(),
                request.getTillNumber(),
                null,
                mpesaResponse
            );

            return ResponseEntity.ok(ApiResponse.success(
                "Till payment initiated",
                Map.of(
                    "transactionId", transaction.getId(),
                    "tillNumber", request.getTillNumber(),
                    "amount", request.getAmount(),
                    "status", transaction.getStatus(),
                    "mpesaResponse", mpesaResponse
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/payments/local/paybill
     * Pay to a Paybill number.
     */
    @PostMapping("/paybill")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payToPaybill(
            @Valid @RequestBody LocalPaymentRequest request) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "PAYBILL");

            if (request.getPaybillNumber() == null || request.getPaybillNumber().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("paybillNumber is required for Paybill payments"));
            }

            // Debit wallet
            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            // Dispatch B2B to Safaricom
            Map<String, Object> mpesaResponse = mpesaService.payToPaybill(
                request.getPaybillNumber(),
                request.getAccountNumber(),
                request.getAmount(),
                request.getDescription()
            );

            // Record transaction
            var transaction = transactionService.createLocalPayment(
                request.getUserId(),
                "PAYBILL",
                request.getAmount(),
                request.getPaybillNumber(),
                request.getAccountNumber(),
                mpesaResponse
            );

            return ResponseEntity.ok(ApiResponse.success(
                "Paybill payment initiated",
                Map.of(
                    "transactionId", transaction.getId(),
                    "paybillNumber", request.getPaybillNumber(),
                    "accountNumber", request.getAccountNumber() != null ? request.getAccountNumber() : "",
                    "amount", request.getAmount(),
                    "status", transaction.getStatus(),
                    "mpesaResponse", mpesaResponse
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/payments/local/pochi
     * Pay to Pochi la Biashara.
     */
    @PostMapping("/pochi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payToPochi(
            @Valid @RequestBody LocalPaymentRequest request) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "POCHI");

            if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("recipientPhone is required for Pochi payments"));
            }

            // Debit wallet
            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            // Dispatch via B2B (Pochi shortcode 440000 + phone as account reference)
            Map<String, Object> mpesaResponse = mpesaService.payToPochi(
                request.getRecipientPhone(),
                request.getAmount(),
                request.getDescription()
            );

            // Record transaction
            var transaction = transactionService.createLocalPayment(
                request.getUserId(),
                "POCHI",
                request.getAmount(),
                "440000",
                request.getRecipientPhone(),
                mpesaResponse
            );

            return ResponseEntity.ok(ApiResponse.success(
                "Pochi la Biashara payment initiated",
                Map.of(
                    "transactionId", transaction.getId(),
                    "recipientPhone", request.getRecipientPhone(),
                    "amount", request.getAmount(),
                    "status", transaction.getStatus(),
                    "mpesaResponse", mpesaResponse
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/payments/local/send
     * Send Money to an M-Pesa phone number (B2C).
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMoney(
            @Valid @RequestBody LocalPaymentRequest request) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "SEND");

            if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("recipientPhone is required for Send Money"));
            }

            String normalizedPhone = mpesaService.normalizePhoneNumber(request.getRecipientPhone());

            // Debit wallet
            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            // Dispatch B2C to Safaricom
            Map<String, Object> mpesaResponse = mpesaService.initiateB2C(
                normalizedPhone,
                request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "NylePay Send Money"
            );

            // Record transaction
            var transaction = transactionService.createLocalPayment(
                request.getUserId(),
                "SEND_MONEY",
                request.getAmount(),
                normalizedPhone,
                null,
                mpesaResponse
            );

            return ResponseEntity.ok(ApiResponse.success(
                "Send Money initiated",
                Map.of(
                    "transactionId", transaction.getId(),
                    "recipientPhone", normalizedPhone,
                    "amount", request.getAmount(),
                    "status", transaction.getStatus(),
                    "mpesaResponse", mpesaResponse
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateLocalPayment(Long userId, BigDecimal amount, String paymentType) {
        // KYC check
        if (!kycService.canTransact(userId, amount)) {
            throw new RuntimeException(
                "Transaction blocked: KYC not verified or monthly limit exceeded. " +
                "Complete KYC at /api/kyc/submit to increase your limits."
            );
        }

        // Balance check
        BigDecimal balance = walletService.getBalance(userId, "KSH");
        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException(
                "Insufficient KSH balance. Available: " + balance + ", Required: " + amount
            );
        }

        // Minimum amount (Safaricom minimum is KES 1)
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new RuntimeException("Minimum payment amount is KES 1");
        }

        // Maximum B2B amount (Safaricom B2B max is KES 999,999)
        BigDecimal maxAmount = new BigDecimal("999999");
        if (amount.compareTo(maxAmount) > 0) {
            throw new RuntimeException("Maximum single payment amount is KES 999,999");
        }

        log.info("Local payment validated: userId={} type={} amount={}", userId, paymentType, amount);
    }
}
