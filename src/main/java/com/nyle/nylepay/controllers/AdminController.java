package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.LegalHoldRequest;
import com.nyle.nylepay.dto.ReversalResolutionRequest;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.services.AdminService;
import com.nyle.nylepay.services.TransactionService;
import com.nyle.nylepay.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    private final TransactionService transactionService;
    private final UserService userService;

    public AdminController(AdminService adminService,
            TransactionService transactionService,
            UserService userService) {
        this.adminService = adminService;
        this.transactionService = transactionService;
        this.userService = userService;
    }

    // DASHBOARD METRICS
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics() {
        try {
            var metrics = adminService.getDashboardMetrics();
            return ResponseEntity.ok(ApiResponse.success("Metrics retrieved", metrics));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // TRANSACTION MONITORING
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        try {
            var transactions = adminService.getAllTransactions(page, size, status);
            return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", Map.of(
                    "content", transactions.getContent(),
                    "totalPages", transactions.getTotalPages(),
                    "totalElements", transactions.getTotalElements(),
                    "page", page,
                    "size", size)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/transactions/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        try {
            transactionService.updateTransactionStatus(id, status, notes);
            return ResponseEntity.ok(ApiResponse.success("Transaction status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/transactions/{id}/reversal/resolve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveReversal(
            @PathVariable Long id,
            @Valid @RequestBody ReversalResolutionRequest request) {
        try {
            Map<String, Object> response = transactionService.resolveTransferReversal(
                    id,
                    request.getRecipientOutcome(),
                    request.getSupportAgentUserId(),
                    request.getNotes());
            return ResponseEntity.ok(ApiResponse.success("Reversal review recorded", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // USER MANAGEMENT
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var users = adminService.getAllUsers(page, size);
            return ResponseEntity.ok(ApiResponse.success("Users retrieved", Map.of(
                    "content", users.getContent(),
                    "totalPages", users.getTotalPages(),
                    "totalElements", users.getTotalElements(),
                    "page", page,
                    "size", size)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDetail(
            @PathVariable Long userId) {
        try {
            var detail = adminService.getUserDetail(userId);
            return ResponseEntity.ok(ApiResponse.success("User detail retrieved", detail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/legal-hold")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyLegalHold(
            @PathVariable Long userId,
            @Valid @RequestBody LegalHoldRequest request) {
        try {
            User user = userService.applyLegalHold(
                    userId,
                    request.getAction(),
                    request.getAuthority(),
                    request.getCourtOrderReference(),
                    request.getReason(),
                    request.getHoldUntil());
            return ResponseEntity.ok(ApiResponse.success("Account legal hold updated", Map.of(
                    "userId", user.getId(),
                    "accountStatus", user.getAccountStatus(),
                    "authority", user.getLegalHoldAuthority(),
                    "courtOrderReference", user.getLegalHoldReference(),
                    "holdUntil", user.getLegalHoldUntil() != null ? user.getLegalHoldUntil() : "")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
