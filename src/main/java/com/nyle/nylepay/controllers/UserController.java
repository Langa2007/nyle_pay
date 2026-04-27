package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.exceptions.NylePayException;
import com.nyle.nylepay.services.UserService;
import com.nyle.nylepay.services.WalletService;
import com.nyle.nylepay.services.kyc.KycService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final WalletService walletService;
    private final KycService kycService;

    public UserController(UserService userService, WalletService walletService, KycService kycService) {
        this.userService = userService;
        this.walletService = walletService;
        this.kycService = kycService;
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
                    "balances", balances);

            return ResponseEntity.ok(ApiResponse.success(
                    "User profile retrieved",
                    profile));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving user profile {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to retrieve profile. Please try again later."));
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
                    Map.of("user", user)));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating MPesa number for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to update MPesa number. Please verify the format and try again."));
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
                    Map.of("user", user)));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating bank account for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to update bank account. Please check your details and try again."));
        }
    }

    @PostMapping("/{userId}/crypto-wallet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCryptoWallet(
            @PathVariable Long userId) {

        try {
            var walletInfo = userService.createCryptoWalletForUser(userId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Crypto wallet created successfully",
                    walletInfo));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating crypto wallet for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to create crypto wallet. Please try again later."));
        }
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserBalance(
            @PathVariable Long userId) {

        try {
            var balances = walletService.getBalances(userId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Balances retrieved",
                    Map.of("balances", balances)));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving balances for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to retrieve balances."));
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
                    Map.of("currency", currency, "balance", balance)));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving {} balance for user {}: {}", currency, userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to retrieve balance."));
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
                    fromUserId, toUserId, currency, java.math.BigDecimal.valueOf(amount));

            return ResponseEntity.ok(ApiResponse.success(
                    "Transfer completed successfully",
                    result));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during transfer from {} to {}: {}", fromUserId, toUserId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to complete transfer. Please check your balance and recipient details."));
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
                    userId, fromCurrency, toCurrency, java.math.BigDecimal.valueOf(amount));

            return ResponseEntity.ok(ApiResponse.success(
                    "Currency conversion completed",
                    result));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during conversion for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to perform conversion. Please try again later."));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {

        try {
            var users = userService.searchUsers(email, phone);
            return ResponseEntity.ok(ApiResponse.success(
                    users.isEmpty() ? "No users found" : "Users found",
                    Map.of("users", users)));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error searching users: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search failed. Please try again later."));
        }
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates) {

        try {
            var updatedUser = userService.updateUserProfile(userId, updates);
            return ResponseEntity.ok(ApiResponse.success(
                    "Profile updated successfully",
                    Map.of("user", updatedUser)));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to update profile. Please try again later."));
        }
    }

    @PostMapping("/{userId}/verify-identity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyIdentity(
            @PathVariable Long userId,
            @RequestParam String documentType, // "ID", "PASSPORT", "DRIVER_LICENSE"
            @RequestParam String documentNumber,
            @RequestParam(required = false, defaultValue = "KE") String country,
            @RequestParam(required = false) String selfieBase64) {

        try {
            var result = kycService.submitKyc(userId, documentType, documentNumber, country, selfieBase64);
            return ResponseEntity.ok(ApiResponse.success(
                    "Identity verification submitted for processing", result));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting KYC for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Identity verification submission failed. Please try again later."));
        }
    }
}
