package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.RouteRequest;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import com.nyle.nylepay.services.routing.RouteExecutionService;
import com.nyle.nylepay.services.routing.RouteQuoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteQuoteService routeQuoteService;
    private final RouteExecutionService routeExecutionService;
    private final UserRepository userRepository;

    public RouteController(RouteQuoteService routeQuoteService,
            RouteExecutionService routeExecutionService,
            UserRepository userRepository) {
        this.routeQuoteService = routeQuoteService;
        this.routeExecutionService = routeExecutionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quote(@Valid @RequestBody RouteRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Route quote generated", routeQuoteService.quote(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> execute(
            @Valid @RequestBody RouteRequest request,
            Authentication authentication) {
        try {
            Long userId = resolveUserId(authentication);
            return ResponseEntity.ok(ApiResponse.success(
                    "Route execution started",
                    routeExecutionService.execute(userId, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/capabilities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> capabilities() {
        return ResponseEntity.ok(ApiResponse.success("Kenya-first routing capabilities", Map.of(
                "country", "KE",
                "sourceRails", java.util.List.of("NYLEPAY_WALLET", "MPESA", "AIRTEL_MONEY", "PESALINK", "BANK", "CARD", "ONCHAIN", "CEX"),
                "destinationRails", java.util.List.of("NYLEPAY_WALLET", "MPESA", "AIRTEL_MONEY", "PESALINK", "BANK", "TILL", "PAYBILL", "POCHI", "ONCHAIN", "MERCHANT"),
                "assets", java.util.List.of("KSH", "USD", "USDT", "USDC", "DAI", "ETH", "BTC"),
                "exampleRoutes", java.util.List.of("MPESA_TO_PESALINK", "AIRTEL_MONEY_TO_NYLEPAY_WALLET", "NYLEPAY_WALLET_TO_AIRTEL_MONEY", "USDT_TO_AIRTEL_MONEY"),
                "accountNumberFormat", "NPYXXXXXXXX",
                "message", "NylePay routes money by intent: where it is, where it should go, and the rail policy in between.")));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user profile not found"));
        return user.getId();
    }
}
