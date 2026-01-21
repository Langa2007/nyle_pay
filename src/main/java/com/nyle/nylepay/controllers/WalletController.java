package com.nyle.nylepay.controllers;

import com.nyle.nylepay.services.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/create-crypto/{userId}")
    public ResponseEntity<Map<String, Object>> createCryptoWallet(
            @PathVariable Long userId
    ) {
        try {
            return ResponseEntity.ok(walletService.createCryptoWallet(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/balances/{userId}")
    public ResponseEntity<Map<String, Object>> getBalances(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                Map.of("balances", walletService.getBalances(userId))
        );
    }
}
