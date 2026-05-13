package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.LocalPaymentRequest;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.TransactionService;
import com.nyle.nylepay.services.WalletService;
import com.nyle.nylepay.services.kyc.KycService;
import com.nyle.nylepay.services.AntiFraudService;
import com.nyle.nylepay.services.AuditLogService;
import com.nyle.nylepay.services.UserService;
import com.nyle.nylepay.models.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles local Kenyan payments: Till, Paybill, Pochi la Biashara, and Send
 * Money.
 *
 * Flow for each payment type:
 * 1. Validate KYC status and monthly limits
 * 2. Run Anti-Fraud velocity and amount checks
 * 3. Debit user's KSH wallet balance
 * 4. Dispatch to Safaricom (B2B for Till/Paybill/Pochi, B2C for Send Money)
 * 5. Record PENDING transaction (finalized on callback)
 * 6. Log security audit event
 */
@RestController
@RequestMapping("/api/payments/local")
public class LocalPaymentController {

    private static final Logger log = LoggerFactory.getLogger(LocalPaymentController.class);

    private final MpesaService mpesaService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final KycService kycService;
    private final AntiFraudService antiFraudService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public LocalPaymentController(MpesaService mpesaService,
            TransactionService transactionService,
            WalletService walletService,
            KycService kycService,
            AntiFraudService antiFraudService,
            AuditLogService auditLogService,
            UserService userService) {
        this.mpesaService = mpesaService;
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.kycService = kycService;
        this.antiFraudService = antiFraudService;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    /**
     * POST /api/payments/local/till
     * Pay to a Till number (Lipa na M-Pesa Buy Goods).
     */
    @PostMapping("/till")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payToTill(
            @Valid @RequestBody LocalPaymentRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "TILL", httpServletRequest);

            if (request.getTillNumber() == null || request.getTillNumber().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("tillNumber is required for Till payments"));
            }

            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            Map<String, Object> mpesaResponse = mpesaService.payToTill(
                    request.getTillNumber(),
                    request.getAmount(),
                    request.getDescription());

            var transaction = transactionService.createLocalPayment(
                    request.getUserId(),
                    "TILL",
                    request.getAmount(),
                    request.getTillNumber(),
                    null,
                    mpesaResponse);

            auditLogService.logPaymentEvent(request.getUserId(), "PAYMENT_INITIATED",
                    "Till payment to " + request.getTillNumber(), "SUCCESS",
                    Map.of("transactionId", transaction.getId(), "tillNumber", request.getTillNumber(), "amount",
                            request.getAmount()));

            return ResponseEntity.ok(ApiResponse.success(
                    "Till payment initiated. Transaction Code: " + transaction.getTransactionCode(),
                    Map.of(
                            "transactionId", transaction.getId(),
                            "transactionCode", transaction.getTransactionCode(),
                            "tillNumber", request.getTillNumber(),
                            "amount", request.getAmount(),
                            "status", transaction.getStatus(),
                            "mpesaResponse", mpesaResponse)));
        } catch (Exception e) {
            auditLogService.logPaymentEvent(request.getUserId(), "PAYMENT_FAILED",
                    "Till payment failed: " + e.getMessage(), "FAILED",
                    Map.of("tillNumber", request.getTillNumber(), "amount", request.getAmount()));
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/payments/local/paybill
     * Pay to a Paybill number.
     */
    @PostMapping("/paybill")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payToPaybill(
            @Valid @RequestBody LocalPaymentRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "PAYBILL", httpServletRequest);

            if (request.getPaybillNumber() == null || request.getPaybillNumber().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("paybillNumber is required for Paybill payments"));
            }

            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            Map<String, Object> mpesaResponse = mpesaService.payToPaybill(
                    request.getPaybillNumber(),
                    request.getAccountNumber(),
                    request.getAmount(),
                    request.getDescription());

            var transaction = transactionService.createLocalPayment(
                    request.getUserId(),
                    "PAYBILL",
                    request.getAmount(),
                    request.getPaybillNumber(),
                    request.getAccountNumber(),
                    mpesaResponse);

            auditLogService.logPaymentEvent(request.getUserId(), "PAYMENT_INITIATED",
                    "Paybill payment to " + request.getPaybillNumber() + " (Acc: " + request.getAccountNumber() + ")",
                    "SUCCESS",
                    Map.of("transactionId", transaction.getId(), "paybillNumber", request.getPaybillNumber(), "amount",
                            request.getAmount()));

            return ResponseEntity.ok(ApiResponse.success(
                    "Paybill payment initiated. Transaction Code: " + transaction.getTransactionCode(),
                    Map.of(
                            "transactionId", transaction.getId(),
                            "transactionCode", transaction.getTransactionCode(),
                            "paybillNumber", request.getPaybillNumber(),
                            "accountNumber", request.getAccountNumber() != null ? request.getAccountNumber() : "",
                            "amount", request.getAmount(),
                            "status", transaction.getStatus(),
                            "mpesaResponse", mpesaResponse)));
        } catch (Exception e) {
            auditLogService.logPaymentEvent(request.getUserId(), "PAYMENT_FAILED",
                    "Paybill payment failed: " + e.getMessage(), "FAILED",
                    Map.of("paybillNumber", request.getPaybillNumber(), "amount", request.getAmount()));
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/payments/local/pochi
     * Pay to Pochi la Biashara.
     */
    @PostMapping("/pochi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payToPochi(
            @Valid @RequestBody LocalPaymentRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "POCHI", httpServletRequest);

            if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("recipientPhone is required for Pochi payments"));
            }

            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            Map<String, Object> mpesaResponse = mpesaService.payToPochi(
                    request.getRecipientPhone(),
                    request.getAmount(),
                    request.getDescription());

            var transaction = transactionService.createLocalPayment(
                    request.getUserId(),
                    "POCHI",
                    request.getAmount(),
                    "440000",
                    request.getRecipientPhone(),
                    mpesaResponse);

            auditLogService.logPaymentEvent(request.getUserId(), "PAYMENT_INITIATED",
                    "Pochi payment to " + request.getRecipientPhone(), "SUCCESS",
                    Map.of("transactionId", transaction.getId(), "recipientPhone", request.getRecipientPhone(),
                            "amount", request.getAmount()));

            return ResponseEntity.ok(ApiResponse.success(
                    "Pochi la Biashara payment initiated. Transaction Code: " + transaction.getTransactionCode(),
                    Map.of(
                            "transactionId", transaction.getId(),
                            "transactionCode", transaction.getTransactionCode(),
                            "recipientPhone", request.getRecipientPhone(),
                            "amount", request.getAmount(),
                            "status", transaction.getStatus(),
                            "mpesaResponse", mpesaResponse)));
        } catch (Exception e) {
            auditLogService.logPaymentEvent(request.getUserId(), "PAYMENT_FAILED",
                    "Pochi payment failed: " + e.getMessage(), "FAILED",
                    Map.of("recipientPhone", request.getRecipientPhone(), "amount", request.getAmount()));
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/payments/local/send
     * Send Money to an M-Pesa phone number (B2C).
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMoney(
            @Valid @RequestBody LocalPaymentRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            validateLocalPayment(request.getUserId(), request.getAmount(), "SEND", httpServletRequest);

            if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("recipientPhone is required for Send Money"));
            }

            String normalizedPhone = mpesaService.normalizePhoneNumber(request.getRecipientPhone());

            walletService.deductBalance(request.getUserId(), "KSH", request.getAmount());

            Map<String, Object> mpesaResponse = mpesaService.initiateB2C(
                    normalizedPhone,
                    request.getAmount(),
                    request.getDescription() != null ? request.getDescription() : "NylePay Send Money");

            var transaction = transactionService.createLocalPayment(
                    request.getUserId(),
                    "SEND_MONEY",
                    request.getAmount(),
                    normalizedPhone,
                    null,
                    mpesaResponse);

            auditLogService.logPaymentEvent(request.getUserId(), "WITHDRAWAL_INITIATED",
                    "Send Money to " + normalizedPhone, "SUCCESS",
                    Map.of("transactionId", transaction.getId(), "recipientPhone", normalizedPhone, "amount",
                            request.getAmount()));

            return ResponseEntity.ok(ApiResponse.success(
                    "Send Money initiated. Transaction Code: " + transaction.getTransactionCode(),
                    Map.of(
                            "transactionId", transaction.getId(),
                            "transactionCode", transaction.getTransactionCode(),
                            "recipientPhone", normalizedPhone,
                            "amount", request.getAmount(),
                            "status", transaction.getStatus(),
                            "mpesaResponse", mpesaResponse)));
        } catch (Exception e) {
            auditLogService.logPaymentEvent(request.getUserId(), "WITHDRAWAL_FAILED",
                    "Send Money failed: " + e.getMessage(), "FAILED",
                    Map.of("recipientPhone", request.getRecipientPhone(), "amount", request.getAmount()));
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private void validateLocalPayment(Long userId, BigDecimal amount, String paymentType, HttpServletRequest request) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AntiFraudService.FraudCheckResult fraudResult = antiFraudService.checkTransaction(
                userId, amount, paymentType, user.getCreatedAt(), request);

        if (fraudResult.isBlocked()) {
            throw new RuntimeException("Transaction blocked: " + fraudResult.getReason());
        }
        if (!kycService.canTransact(userId, amount)) {
            throw new RuntimeException(
                    "Transaction blocked: KYC not verified or monthly limit exceeded. " +
                            "Complete KYC at /api/kyc/submit to increase your limits.");
        }

        BigDecimal balance = walletService.getBalance(userId, "KSH");
        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException(
                    "Insufficient KSH balance. Available: " + balance + ", Required: " + amount);
        }

        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new RuntimeException("Minimum payment amount is KES 1");
        }

        BigDecimal maxAmount = new BigDecimal("999999");
        if (amount.compareTo(maxAmount) > 0) {
            throw new RuntimeException("Maximum single payment amount is KES 999,999");
        }

        log.info("Local payment validated: userId={} type={} amount={}", userId, paymentType, amount);
    }
}
