package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.*;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final MpesaService mpesaService;
    private final TransactionService transactionService;
    
    public PaymentController(MpesaService mpesaService, 
                           TransactionService transactionService) {
        this.mpesaService = mpesaService;
        this.transactionService = transactionService;
    }
    
    @PostMapping("/deposit/mpesa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mpesaDeposit(
            @Valid @RequestBody DepositRequest request) {
        
        try {
            // Validate MPesa number
            if (request.getMethod().equals("MPESA") && 
                !request.getMpesaNumber().matches("^2547[0-9]{8}$")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid MPesa number format"));
            }
            
            // Initiate MPesa STK push
            Map<String, Object> mpesaResponse = mpesaService.stkPush(
                request.getMpesaNumber(),
                request.getAmount(),
                "DEPOSIT_" + System.currentTimeMillis()
            );
            
            // Create pending transaction
            var transaction = transactionService.createDeposit(
                request.getUserId(),
                "MPESA",
                request.getAmount(),
                request.getCurrency(),
                (String) mpesaResponse.get("CheckoutRequestID")
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
                (String) bankDetails.get("reference")
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
    
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Map<String, Object>>> withdraw(
            @Valid @RequestBody WithdrawalRequest request) {
        
        try {
            var transaction = transactionService.createWithdrawal(
                request.getUserId(),
                request.getMethod(),
                request.getAmount(),
                request.getCurrency(),
                getDestination(request)
            );
            
            Map<String, Object> response = Map.of(
                "transactionId", transaction.getId(),
                "amount", request.getAmount(),
                "currency", request.getCurrency(),
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
    
    @PostMapping("/webhook/bank")
    public ResponseEntity<String> bankWebhook(@RequestBody Map<String, Object> payload) {
        try {
            transactionService.processBankCallback(payload);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing callback");
        }
    }
    
    private String getDestination(WithdrawalRequest request) {
        switch (request.getMethod().toUpperCase()) {
            case "MPESA":
                return request.getMpesaNumber();
            case "BANK":
                return request.getBankAccount() + "|" + request.getBankName();
            case "CRYPTO":
                return request.getCryptoAddress();
            default:
                throw new IllegalArgumentException("Invalid withdrawal method");
        }
    }
}
