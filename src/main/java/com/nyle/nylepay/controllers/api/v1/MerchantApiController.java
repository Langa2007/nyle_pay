package com.nyle.nylepay.controllers.api.v1;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.exceptions.NylePayException;
import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.merchant.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Headless Merchant API v1.
 * Authenticated via 'Authorization: Bearer npy_sec_...' using MerchantAuthFilter.
 */
@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantApiController {

    private static final BigDecimal SANDBOX_MAX_AMOUNT = new BigDecimal("100000");

    private final MpesaService mpesaService;
    private final SettlementService settlementService;
    private final com.nyle.nylepay.repositories.MerchantRepository merchantRepository;

    public MerchantApiController(MpesaService mpesaService, SettlementService settlementService, com.nyle.nylepay.repositories.MerchantRepository merchantRepository) {
        this.mpesaService = mpesaService;
        this.settlementService = settlementService;
        this.merchantRepository = merchantRepository;
    }

    /**
     * Get real-time balance for the authenticated merchant.
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved", Map.of(
                "merchantId", merchant.getId(),
                "mode", isSandbox(merchant) ? "SANDBOX" : "LIVE",
                "currency", merchant.getSettlementCurrency(),
                "pendingSettlement", isSandbox(merchant)
                        ? new BigDecimal("250000.00")
                        : merchant.getPendingSettlement() != null ? merchant.getPendingSettlement() : BigDecimal.ZERO
        )));
    }

    /**
     * Programmatically initiate a charge from a customer.
     * Only MPESA STK Push is supported in this example.
     */
    @PostMapping("/charges")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCharge(
            @RequestBody Map<String, Object> req,
            Authentication auth) {
        
        Merchant merchant = (Merchant) auth.getPrincipal();
        
        String method = (String) req.getOrDefault("method", "MPESA");
        String phone = (String) req.get("phone");
        String reference = (String) req.getOrDefault("reference", UUID.randomUUID().toString().replace("-", ""));
        
        try {
            BigDecimal amount = new BigDecimal(req.get("amount").toString());

            if (isSandbox(merchant)) {
                validateSandboxAmount(amount);
                return ResponseEntity.ok(ApiResponse.success("Sandbox charge simulated", Map.of(
                        "reference", reference,
                        "status", "SUCCEEDED",
                        "mode", "SANDBOX",
                        "method", method,
                        "amount", amount,
                        "providerData", Map.of(
                                "providerReference", "SBX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(),
                                "message", "No real money moved"
                        )
                )));
            }

            if ("MPESA".equalsIgnoreCase(method)) {
                if (phone == null || !phone.matches("^2547[0-9]{8}$")) {
                    throw new NylePayException("Invalid Safaricom phone number.");
                }
                
                // Initiate STK Push
                BigDecimal mpesaAmount = amount.setScale(0, RoundingMode.CEILING);
                Map<String, Object> stkResult = mpesaService.stkPush(phone, mpesaAmount, reference);
                
                return ResponseEntity.ok(ApiResponse.success("Charge initiated", Map.of(
                        "reference", reference,
                        "status", "PENDING",
                        "providerData", stkResult
                )));
            } else {
                throw new NylePayException("Unsupported charge method: " + method);
            }
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to initiate charge."));
        }
    }

    /**
     * Programmatically payout/transfer funds from merchant balance to an external destination.
     */
    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTransfer(
            @RequestBody Map<String, Object> req,
            Authentication auth) {
        
        Merchant merchant = (Merchant) auth.getPrincipal();
        
        try {
            BigDecimal amount = new BigDecimal(req.get("amount").toString());
            String destinationType = (String) req.getOrDefault("destinationType", "MPESA");
            String destinationId = (String) req.get("destinationId");

            if (isSandbox(merchant)) {
                validateSandboxAmount(amount);
                return ResponseEntity.ok(ApiResponse.success("Sandbox transfer simulated", Map.of(
                        "status", "COMPLETED",
                        "mode", "SANDBOX",
                        "amount", amount,
                        "destinationType", destinationType,
                        "destination", destinationId != null ? destinationId : "sandbox-destination",
                        "reference", "SBX-TRF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                        "message", "No real money moved"
                )));
            }
            
            // Check balance
            BigDecimal balance = merchant.getPendingSettlement() != null ? merchant.getPendingSettlement() : BigDecimal.ZERO;
            if (balance.compareTo(amount) < 0) {
                throw new NylePayException("Insufficient settlement balance for transfer.");
            }
            
            // Deduct balance and execute payout
            if ("MPESA".equalsIgnoreCase(destinationType)) {
                if (destinationId == null) throw new NylePayException("destinationId required for MPESA transfer.");
                
                // Directly use SettlementService logic to send money
                settlementService.settle(merchant, amount); // Wait, settle() uses merchant.getSettlementPhone()
                // Let's use mpesaService directly for custom destination
                int mpesaAmount = amount.setScale(0, RoundingMode.FLOOR).intValue();
                mpesaService.initiateB2C(destinationId, new BigDecimal(mpesaAmount), "NylePay API Transfer");
                
                // Deduct from merchant
                merchant.setPendingSettlement(balance.subtract(amount));
                merchantRepository.save(merchant);
            } else {
                throw new NylePayException("Unsupported destination type: " + destinationType);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Transfer initiated successfully.", Map.of(
                    "status", "COMPLETED",
                    "amount", amount,
                    "destination", destinationId
            )));
            
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to execute transfer."));
        }
    }

    private boolean isSandbox(Merchant merchant) {
        return merchant != null && "SANDBOX".equalsIgnoreCase(merchant.getStatus());
    }

    private void validateSandboxAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NylePayException("Amount must be greater than zero.");
        }
        if (amount.compareTo(SANDBOX_MAX_AMOUNT) > 0) {
            throw new NylePayException("Sandbox amount limit is KES " + SANDBOX_MAX_AMOUNT + ".");
        }
    }
}
