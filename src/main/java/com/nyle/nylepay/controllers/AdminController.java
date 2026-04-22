package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.services.AdminService;
import com.nyle.nylepay.services.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    private final TransactionService transactionService;

    public AdminController(AdminService adminService,
            TransactionService transactionService) {
        this.adminService = adminService;
        this.transactionService = transactionService;
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
}
