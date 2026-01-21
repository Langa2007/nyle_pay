package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.services.UserService;
import com.nyle.nylepay.services.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final WalletService walletService;
    
    public UserController(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile(
            @PathVariable Long userId) {
        
        try {
            var user = userService.getUserById(userId);
            
            if (user.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User not found"));
            }
            
            var balances = walletService.getBalances(userId);
            
            Map<String, Object> profile = Map.of(
                "user", user.get(),
                "balances", balances
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "User profile retrieved",
                profile
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}/mpesa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMpesaNumber(
            @PathVariable Long userId,
            @RequestParam String mpesaNumber) {
        
        try {
            var user = userService.updateUserMpesaNumber(userId, mpesaNumber);
            
            return ResponseEntity.ok(ApiResponse.success(
                "MPesa number updated successfully",
                Map.of("user", user)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}/bank")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateBankAccount(
            @PathVariable Long userId,
            @RequestParam String bankAccountNumber,
            @RequestParam String bankName) {
        
        try {
            var user = userService.updateUserBankAccount(userId, bankAccountNumber, bankName);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Bank account updated successfully",
                Map.of("user", user)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/crypto-wallet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCryptoWallet(
            @PathVariable Long userId) {
        
        try {
            var walletInfo = userService.createCryptoWalletForUser(userId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Crypto wallet created successfully",
                walletInfo
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{userId}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserBalance(
            @PathVariable Long userId) {
        
        try {
            var balances = walletService.getBalances(userId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Balances retrieved",
                Map.of("balances", balances)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{userId}/balance/{currency}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSpecificBalance(
            @PathVariable Long userId,
            @PathVariable String currency) {
        
        try {
            var balance = walletService.getBalance(userId, currency);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Balance retrieved",
                Map.of("currency", currency, "balance", balance)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/transfer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferToUser(
            @PathVariable Long fromUserId,
            @RequestParam Long toUserId,
            @RequestParam String currency,
            @RequestParam Double amount) {
        
        try {
            var result = walletService.transferBetweenUsers(
                fromUserId, toUserId, currency, java.math.BigDecimal.valueOf(amount)
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Transfer completed successfully",
                result
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/convert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> convertCurrency(
            @PathVariable Long userId,
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam Double amount) {
        
        try {
            var result = walletService.convertCurrency(
                userId, fromCurrency, toCurrency, java.math.BigDecimal.valueOf(amount)
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Currency conversion completed",
                result
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        
        try {
            // TODO: Implement user search logic
            return ResponseEntity.ok(ApiResponse.success(
                "Search functionality coming soon"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates) {
        
        try {
            // TODO: Implement profile update logic
            return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully",
                Map.of("updates", updates)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/verify-identity")
    public ResponseEntity<ApiResponse<String>> verifyIdentity(
            @PathVariable Long userId,
            @RequestParam String documentType, // "ID", "PASSPORT", "DRIVER_LICENSE"
            @RequestParam String documentNumber) {
        
        try {
            // TODO: Implement identity verification with third-party service
            return ResponseEntity.ok(ApiResponse.success(
                "Identity verification submitted for processing"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
