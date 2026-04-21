package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.services.CryptoExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.services.UserService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final CryptoExchangeService cryptoExchangeService;
    private final UserService userService;

    public ExchangeController(CryptoExchangeService cryptoExchangeService, UserService userService) {
        this.cryptoExchangeService = cryptoExchangeService;
        this.userService = userService;
    }

    @GetMapping("/rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            BigDecimal rate = cryptoExchangeService.getExchangeRate(from.toUpperCase(), to.toUpperCase());
            return ResponseEntity.ok(ApiResponse.success(
                    "Current exchange rate retrieved successfully",
                    Map.of("from", from.toUpperCase(), "to", to.toUpperCase(), "rate", rate)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/swap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> swapCrypto(
            @RequestBody Map<String, Object> payload) {
        try {
            String fromAsset = (String) payload.get("from");
            String toAsset = (String) payload.get("to");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            // Get logged in user context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("User holds no valid authentication context");
            }
            
            Object principal = authentication.getPrincipal();
            String email;
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("Authenticated user profile not found");
            }
            Long userId = userOpt.get().getId();

            Map<String, Object> result = cryptoExchangeService.swapCrypto(userId, fromAsset, toAsset, amount);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Crypto Swap executed successfully",
                    result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
