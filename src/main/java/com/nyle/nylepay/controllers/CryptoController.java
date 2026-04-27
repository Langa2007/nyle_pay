package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.services.CryptoExchangeService;
import com.nyle.nylepay.services.AuditLogService;
import com.nyle.nylepay.services.UserService;
import com.nyle.nylepay.models.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Phase 3: Crypto Bridge Controller
 * Handles CEX ⇄ NylePay movements and Liquidity Bridge swaps.
 */
@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoExchangeService cryptoExchangeService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public CryptoController(CryptoExchangeService cryptoExchangeService,
                            AuditLogService auditLogService,
                            UserService userService) {
        this.cryptoExchangeService = cryptoExchangeService;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    /**
     * GET /api/crypto/deposit-address
     * Get the NylePay deposit address for a specific asset/chain.
     */
    @GetMapping("/deposit-address")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDepositAddress(
            @RequestParam Long userId,
            @RequestParam String asset,
            @RequestParam String chain) {
        
        try {
            String address = cryptoExchangeService.getOrCreateDepositAddress(userId, chain);
            return ResponseEntity.ok(ApiResponse.success("Deposit address retrieved", 
                Map.of("asset", asset, "chain", chain, "address", address)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/crypto/swap
     * Manually swap between assets (e.g., USDT to KES).
     */
    @PostMapping("/swap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> swap(
            @RequestParam Long userId,
            @RequestParam String fromAsset,
            @RequestParam String toAsset,
            @RequestParam BigDecimal amount,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> result = cryptoExchangeService.swapCrypto(userId, fromAsset, toAsset, amount);
            
            auditLogService.logEvent(userId, "CRYPTO_SWAP", 
                "Swapped " + amount + " " + fromAsset + " to " + toAsset, "SUCCESS", request, result);
                
            return ResponseEntity.ok(ApiResponse.success("Swap successful", result));
        } catch (Exception e) {
            auditLogService.logEvent(userId, "CRYPTO_SWAP_FAILED", 
                "Swap failed: " + e.getMessage(), "FAILED", request, null);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/crypto/withdraw-to-cex
     * Withdraw funds from NylePay to an external CEX address.
     */
    @PostMapping("/withdraw-to-cex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> withdrawToCex(
            @RequestParam Long userId,
            @RequestParam String asset,
            @RequestParam BigDecimal amount,
            @RequestParam String destinationAddress,
            @RequestParam(required = false) String network,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> result = cryptoExchangeService.withdrawToExternal(
                userId, asset, amount, destinationAddress, network);
            
            auditLogService.logEvent(userId, "CRYPTO_WITHDRAWAL_CEX", 
                "Withdrawn " + amount + " " + asset + " to CEX", "SUCCESS", request, result);
                
            return ResponseEntity.ok(ApiResponse.success("Withdrawal initiated", result));
        } catch (Exception e) {
            auditLogService.logEvent(userId, "CRYPTO_WITHDRAWAL_FAILED", 
                "Withdrawal failed: " + e.getMessage(), "FAILED", request, null);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
