package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.BankLinkRequest;
import com.nyle.nylepay.dto.ConversionRequest;
import com.nyle.nylepay.dto.DepositRequest;
import com.nyle.nylepay.dto.TransferRequest;
import com.nyle.nylepay.dto.WithdrawalRequest;
import com.nyle.nylepay.models.UserBankDetail;
import com.nyle.nylepay.repositories.UserBankDetailRepository;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.TransactionService;
import com.nyle.nylepay.services.providers.FlutterwaveService;
import com.nyle.nylepay.services.routing.ExchangeRoutingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nyle.nylepay.services.cex.CexRoutingService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final MpesaService mpesaService;
    private final TransactionService transactionService;
    private final CexRoutingService cexRoutingService;
    private final UserBankDetailRepository userBankDetailRepository;
    private final ExchangeRoutingService exchangeRoutingService;
    private final FlutterwaveService flutterwaveService;
    
    public PaymentController(MpesaService mpesaService, 
                           TransactionService transactionService,
                           CexRoutingService cexRoutingService,
                           UserBankDetailRepository userBankDetailRepository,
                           ExchangeRoutingService exchangeRoutingService,
                           FlutterwaveService flutterwaveService) {
        this.mpesaService = mpesaService;
        this.transactionService = transactionService;
        this.cexRoutingService = cexRoutingService;
        this.userBankDetailRepository = userBankDetailRepository;
        this.exchangeRoutingService = exchangeRoutingService;
        this.flutterwaveService = flutterwaveService;
    }
    
    @PostMapping("/deposit/mpesa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mpesaDeposit(
            @Valid @RequestBody DepositRequest request) {
        
        try {
            if (!"MPESA".equalsIgnoreCase(request.getMethod())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Deposit method must be MPESA for this endpoint"));
            }

            String normalizedPhoneNumber = mpesaService.normalizePhoneNumber(request.getMpesaNumber());
            String currency = normalizeMpesaCurrency(request.getCurrency());

            if (currency == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("MPesa deposits are only supported in KSH/KES"));
            }
            
            // Initiate MPesa STK push
            Map<String, Object> mpesaResponse = mpesaService.stkPush(
                normalizedPhoneNumber,
                request.getAmount(),
                "DEPOSIT_" + System.currentTimeMillis()
            );
            
            // Create pending transaction
            var transaction = transactionService.createDeposit(
                request.getUserId(),
                "MPESA",
                request.getAmount(),
                currency,
                (String) mpesaResponse.get("CheckoutRequestID"),
                null // No routing metadata
            );
            
            Map<String, Object> response = Map.of(
                "transactionId", transaction.getId(),
                "mpesaResponse", mpesaResponse,
                "message", "MPesa payment initiated. Please check your phone to complete payment."
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Deposit initiated successfully", 
                response
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/deposit/bank")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bankDeposit(
            @Valid @RequestBody DepositRequest request) {
        
        try {
            // Generate bank deposit instructions
            Map<String, Object> bankDetails = Map.of(
                "bankName", "NylePay Bank",
                "accountNumber", "1234567890",
                "accountName", "NylePay Limited",
                "branchCode", "123",
                "swiftCode", "NYPWKENA",
                "reference", "DEP_" + request.getUserId() + "_" + System.currentTimeMillis(),
                "amount", request.getAmount(),
                "currency", request.getCurrency()
            );
            
            // Create pending transaction
            var transaction = transactionService.createDeposit(
                request.getUserId(),
                "BANK",
                request.getAmount(),
                request.getCurrency(),
                (String) bankDetails.get("reference"),
                null // No routing metadata
            );
            
            Map<String, Object> response = Map.of(
                "transactionId", transaction.getId(),
                "bankDetails", bankDetails,
                "instructions", "Transfer the specified amount to the bank account above. Use the reference provided."
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Bank deposit instructions generated",
                response
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/bank/link")
    public ResponseEntity<ApiResponse<UserBankDetail>> linkBank(
            @Valid @RequestBody BankLinkRequest request) {
        try {
            UserBankDetail detail = new UserBankDetail();
            detail.setUserId(request.getUserId());
            detail.setBankName(request.getBankName());
            detail.setBankCode(request.getBankCode());
            detail.setAccountNumber(request.getAccountNumber());
            detail.setAccountName(request.getAccountName());
            detail.setCountry("KE");

            UserBankDetail saved = userBankDetailRepository.save(detail);
            return ResponseEntity.ok(ApiResponse.success("Bank account linked successfully", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/bank/route-to-mpesa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> routeBankToMpesa(
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String bankCode = request.get("bankCode").toString();
            String accountNumber = request.get("accountNumber").toString();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String mpesaNumber = request.get("mpesaNumber").toString();

            Map<String, Object> result = exchangeRoutingService.moveBankToMpesa(
                    userId, bankCode, accountNumber, amount, mpesaNumber);

            return ResponseEntity.ok(ApiResponse.success("Bank to M-Pesa routing initiated", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Map<String, Object>>> withdraw(
            @Valid @RequestBody WithdrawalRequest request) {
        
        try {
            String currency = request.getCurrency();
            if ("MPESA".equalsIgnoreCase(request.getMethod())) {
                currency = normalizeMpesaCurrency(request.getCurrency());
                if (currency == null) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("MPesa withdrawals are only supported in KSH/KES"));
                }
            }

            var transaction = transactionService.createWithdrawal(
                request.getUserId(),
                request.getMethod(),
                request.getAmount(),
                currency,
                getDestination(request)
            );
            
            Map<String, Object> response = Map.of(
                "transactionId", transaction.getId(),
                "amount", request.getAmount(),
                "currency", currency,
                "method", request.getMethod(),
                "status", transaction.getStatus(),
                "estimatedCompletion", "1-3 business days"
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Withdrawal request submitted",
                response
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transfer(
            @Valid @RequestBody TransferRequest request) {
        
        try {
            var transaction = transactionService.createTransfer(
                request.getFromUserId(),
                request.getToIdentifier(),
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
            );
            
            Map<String, Object> response = Map.of(
                "transactionId", transaction.getId(),
                "fromUserId", request.getFromUserId(),
                "toIdentifier", request.getToIdentifier(),
                "amount", request.getAmount(),
                "currency", request.getCurrency(),
                "status", transaction.getStatus()
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Transfer initiated successfully",
                response
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> convertCurrency(
            @Valid @RequestBody ConversionRequest request) {
        
        try {
            var transaction = transactionService.createConversion(
                request.getUserId(),
                request.getFromCurrency(),
                request.getToCurrency(),
                request.getAmount()
            );
            
            Map<String, Object> response = Map.of(
                "transactionId", transaction.getId(),
                "fromCurrency", request.getFromCurrency(),
                "toCurrency", request.getToCurrency(),
                "amount", request.getAmount(),
                "exchangeRate", transaction.getAmount(), // Amount after conversion
                "status", transaction.getStatus()
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Currency conversion initiated",
                response
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/transaction/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransaction(
            @PathVariable Long id) {
        
        try {
            var transaction = transactionService.getTransactionById(id);
            
            if (transaction.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Transaction not found"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                "Transaction retrieved",
                Map.of("transaction", transaction.get())
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            var transactions = transactionService.getUserTransactions(userId, page, size);
            var stats = transactionService.getTransactionStats(userId);
            
            Map<String, Object> response = Map.of(
                "transactions", transactions,
                "stats", stats,
                "page", page,
                "size", size
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Transactions retrieved",
                response
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/webhook/mpesa")
    public ResponseEntity<String> mpesaWebhook(@RequestBody Map<String, Object> payload) {
        try {
            transactionService.processMpesaCallback(payload);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing callback");
        }
    }

    @PostMapping("/webhook/mpesa/result")
    public ResponseEntity<String> mpesaResultWebhook(@RequestBody Map<String, Object> payload) {
        try {
            transactionService.processMpesaDisbursementResult(payload);
            return ResponseEntity.ok("Result processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing result callback");
        }
    }

    @PostMapping("/webhook/mpesa/timeout")
    public ResponseEntity<String> mpesaTimeoutWebhook(@RequestBody Map<String, Object> payload) {
        try {
            transactionService.processMpesaDisbursementTimeout(payload);
            return ResponseEntity.ok("Timeout processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing timeout callback");
        }
    }
    
    @PostMapping("/webhook/bank")
    public ResponseEntity<String> bankWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Verif-Hash", required = false) String verifHash) {
        // Security: reject unsigned or tampered webhooks before any processing
        if (!flutterwaveService.verifyWebhookSignature(verifHash)) {
            return ResponseEntity.status(401).body("Unauthorized: invalid or missing X-Verif-Hash");
        }
        try {
            transactionService.processBankCallback(payload);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing callback");
        }
    }
    
    @PostMapping("/cex/connect")
    public ResponseEntity<ApiResponse<String>> connectCex(
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String exchange = request.get("exchange").toString();
            String apiKey = request.get("apiKey").toString();
            String apiSecret = request.get("apiSecret").toString();

            cexRoutingService.linkAccount(userId, exchange, apiKey, apiSecret);
            
            return ResponseEntity.ok(ApiResponse.success("Successfully linked " + exchange + " account", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cex/{userId}/balances")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getCexBalances(
            @PathVariable Long userId) {
        try {
            Map<String, BigDecimal> balances = cexRoutingService.getAggregatedBalances(userId);
            return ResponseEntity.ok(ApiResponse.success("Balances fetched successfully", balances));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/cex/withdraw")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cexWithdrawToMpesa(
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String asset = request.get("asset").toString();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String mpesaNumber = request.get("mpesaNumber").toString();

            Map<String, Object> result = cexRoutingService.autoRouteToMpesa(userId, asset, amount, mpesaNumber);
            
            return ResponseEntity.ok(ApiResponse.success("CEX Withdrawal to M-Pesa initiated", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    private String getDestination(WithdrawalRequest request) {
        switch (request.getMethod().toUpperCase()) {
            case "MPESA":
                return mpesaService.normalizePhoneNumber(request.getMpesaNumber());
            case "BANK":
                // Format: accountNumber|bankName|KE  (TransactionService splits on |)
                return request.getBankAccount() + "|" + request.getBankName() + "|KE";
            case "CRYPTO":
                return request.getCryptoAddress();
            default:
                throw new IllegalArgumentException("Invalid withdrawal method");
        }
    }

    private String normalizeMpesaCurrency(String currency) {
        if (currency == null) {
            return null;
        }

        String normalized = currency.trim().toUpperCase();
        if ("KES".equals(normalized) || "KSH".equals(normalized)) {
            return "KSH";
        }
        return null;
    }
}
