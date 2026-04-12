package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.CexLinkRequest;
import com.nyle.nylepay.services.cex.CexRoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/cex")
public class CexController {

    private final CexRoutingService cexRoutingService;

    public CexController(CexRoutingService cexRoutingService) {
        this.cexRoutingService = cexRoutingService;
    }

    @PostMapping("/link")
    public ResponseEntity<Map<String, String>> linkExchange(@RequestBody CexLinkRequest request) {
        try {
            cexRoutingService.linkAccount(
                    request.getUserId(), 
                    request.getExchangeName(), 
                    request.getApiKey(), 
                    request.getApiSecret()
            );
            return ResponseEntity.ok(Map.of("message", request.getExchangeName() + " linked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getBalances(@RequestParam Long userId) {
        try {
            Map<String, BigDecimal> balances = cexRoutingService.getAggregatedBalances(userId);
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> autoWithdraw(
            @RequestParam Long userId,
            @RequestParam String asset,
            @RequestParam BigDecimal amount,
            @RequestParam String mpesaNumber) {
        try {
            Map<String, Object> result = cexRoutingService.autoRouteToMpesa(userId, asset, amount, mpesaNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
