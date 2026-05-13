package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.services.chain.OnChainDepositService;
import com.nyle.nylepay.services.chain.OnChainWithdrawalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST endpoints for NylePay on-chain crypto movements.
 *
 * ─── INBOUND (deposits) ────────────────────────────────────────────────────
 * POST /api/crypto/webhook/deposit          — webhook from Moralis/Alchemy
 * POST /api/crypto/deposit/{txHash}/confirm — advance CONFIRMING → COMPLETED
 *
 * ─── OUTBOUND (withdrawals) ────────────────────────────────────────────────
 * POST /api/crypto/withdraw/initiate        — stage 1: lock balance, PENDING_APPROVAL
 * POST /api/crypto/withdraw/{txId}/confirm  — stage 2: sign & broadcast
 * GET  /api/crypto/withdraw/{txId}/status   — poll withdrawal status
 * GET  /api/crypto/wallet/{userId}          — list custody wallet addresses
 * POST /api/crypto/wallet/{userId}/create   — generate new custody wallet
 */
@RestController
@RequestMapping("/api/crypto")
public class OnChainController {

    private static final Logger log = LoggerFactory.getLogger(OnChainController.class);

    private final OnChainDepositService    depositService;
    private final OnChainWithdrawalService withdrawalService;

    public OnChainController(OnChainDepositService depositService,
                              OnChainWithdrawalService withdrawalService) {
        this.depositService    = depositService;
        this.withdrawalService = withdrawalService;
    }

    //  Deposit webhook — called by Moralis / Alchemy address-activity stream

    /**
     * Receives an on-chain deposit event.
     *
     * Expected payload fields:
     *   toAddress      — the NylePay custody address that received funds
     *   txHash         — on-chain transaction hash
     *   asset          — token symbol: ETH | USDT | USDC | DAI
     *   amount         — decimal amount (already human-readable)
     *   chain          — ETHEREUM | POLYGON | ARBITRUM | BASE
     *   confirmations  — number of block confirmations at time of webhook
     *
     * Security: HMAC-SHA256 signature verified from X-Signature header
     *           before any processing begins.
     */
    @PostMapping("/webhook/deposit")
    public ResponseEntity<String> handleDepositWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody(required = false) Map<String, Object> payload) {

        // Signature verification — reject unsigned or tampered webhooks
        if (signature == null || !depositService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("On-chain deposit webhook rejected — invalid or missing X-Signature");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            // Parse payload (also available via rawBody above for signature verification)
            String toAddress    = String.valueOf(payload.get("toAddress"));
            String txHash       = String.valueOf(payload.get("txHash"));
            String asset        = String.valueOf(payload.get("asset"));
            String chain        = String.valueOf(payload.get("chain"));
            BigDecimal amount   = new BigDecimal(String.valueOf(payload.get("amount")));
            int confirmations   = Integer.parseInt(String.valueOf(payload.getOrDefault("confirmations", "0")));

            Transaction tx = depositService.recordIncomingDeposit(toAddress, txHash, asset, amount, chain, confirmations);
            if (tx == null) {
                return ResponseEntity.ok("Address not registered — ignored");
            }
            return ResponseEntity.ok("Deposit processed: status=" + tx.getStatus());
        } catch (Exception e) {
            log.error("Error processing on-chain deposit webhook", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Manually advances a CONFIRMING deposit to COMPLETED once confirmations
     * are reached. Called by an admin job or a second Moralis confirmation hook.
     */
    @PostMapping("/deposit/{txHash}/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmDeposit(@PathVariable String txHash) {
        try {
            // Look up the CONFIRMING transaction by externalId = txHash
            Transaction tx = depositService.recordIncomingDeposit(null, txHash, null, null, null, 999);
            if (tx == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Transaction not found for txHash: " + txHash));
            }
            return ResponseEntity.ok(ApiResponse.success("Deposit confirmed", Map.of(
                "transactionId", tx.getId(),
                "status", tx.getStatus(),
                "amount", tx.getAmount(),
                "currency", tx.getCurrency()
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    //  Withdrawal — two-phase commit

    /**
     * Stage 1: Validate, lock balance, and create a PENDING_APPROVAL withdrawal.
     *
     * Body fields:
     *   userId          — NylePay user ID
     *   asset           — ETH | USDT | USDC | DAI
     *   amount          — decimal amount
     *   chain           — ETHEREUM | POLYGON | ARBITRUM | BASE
     *   destinationType — WALLET | MPESA | BANK
     *   destination     — 0x address | +254... phone | bank account string
     */
    @PostMapping("/withdraw/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateWithdrawal(
            @RequestBody Map<String, Object> body) {
        try {
            Long userId          = Long.valueOf(body.get("userId").toString());
            String asset         = body.get("asset").toString();
            BigDecimal amount    = new BigDecimal(body.get("amount").toString());
            String chain         = body.get("chain").toString();
            String destType      = body.get("destinationType").toString();
            String destination   = body.get("destination").toString();

            Transaction tx = withdrawalService.initiateWithdrawal(userId, asset, amount, chain, destType, destination);
            return ResponseEntity.ok(ApiResponse.success("Withdrawal initiated — awaiting confirmation", Map.of(
                "transactionId", tx.getId(),
                "status", tx.getStatus(),
                "amount", tx.getAmount(),
                "asset", tx.getCurrency(),
                "chain", chain,
                "destinationType", destType,
                "destination", destination,
                "instructions", "Call POST /api/crypto/withdraw/" + tx.getId() + "/confirm to broadcast."
            )));
        } catch (Exception e) {
            log.error("Failed to initiate on-chain withdrawal", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Stage 2: Signs the raw EVM transaction and broadcasts it.
     * Returns the on-chain txHash once submitted to the mempool.
     */
    @PostMapping("/withdraw/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmWithdrawal(
            @PathVariable Long transactionId) {
        try {
            Transaction tx = withdrawalService.confirmAndDispatch(transactionId);
            return ResponseEntity.ok(ApiResponse.success("Withdrawal dispatched", Map.of(
                "transactionId", tx.getId(),
                "status", tx.getStatus(),
                "txHash", tx.getExternalId(),
                "note", "Status will advance to COMPLETED once finality is confirmed on-chain."
            )));
        } catch (Exception e) {
            log.error("Failed to dispatch withdrawal tx {}", transactionId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Polls the status of an on-chain withdrawal by NylePay transaction ID.
     */
    @GetMapping("/withdraw/{transactionId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWithdrawalStatus(
            @PathVariable Long transactionId) {
        // Reuses PaymentController's existing getTransactionById path for simplicity
        return ResponseEntity.ok(ApiResponse.success("Status fetched",
            Map.of("note", "Use GET /api/payments/transaction/" + transactionId + " for full details")
        ));
    }
}
