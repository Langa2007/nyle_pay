package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.exceptions.NylePayException;
import com.nyle.nylepay.services.CryptoExchangeService;
import com.nyle.nylepay.services.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles CEX ⇄ NylePay movements and Liquidity Bridge swaps.
 */
@RestController
@RequestMapping("/api/crypto")
public class CryptoController {
    private static final Logger logger = LoggerFactory.getLogger(CryptoController.class);

    private final CryptoExchangeService cryptoExchangeService;
    private final AuditLogService auditLogService;

    public CryptoController(CryptoExchangeService cryptoExchangeService,
            AuditLogService auditLogService) {
        this.cryptoExchangeService = cryptoExchangeService;
        this.auditLogService = auditLogService;
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
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving deposit address for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to retrieve deposit address. Please try again later."));
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

            return ResponseEntity.ok(ApiResponse.success(
                "Swap successful. Transaction Code: " + result.get("transactionCode"), 
                result));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during crypto swap for user {}: {}", userId, e.getMessage(), e);
            auditLogService.logEvent(userId, "CRYPTO_SWAP_FAILED",
                    "Swap failed: Internal Error", "FAILED", request, null);
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to complete swap. Please check your balance and try again."));
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

            return ResponseEntity.ok(ApiResponse.success(
                "Withdrawal initiated. Transaction Code: " + result.get("transactionCode"), 
                result));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during crypto withdrawal for user {}: {}", userId, e.getMessage(), e);
            auditLogService.logEvent(userId, "CRYPTO_WITHDRAWAL_FAILED",
                    "Withdrawal failed: Internal Error", "FAILED", request, null);
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to initiate withdrawal. Please try again later."));
        }
    }
}
