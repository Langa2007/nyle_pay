package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import com.nyle.nylepay.services.merchant.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final MerchantService merchantService;
    private final UserRepository userRepository;

    public BusinessController(MerchantService merchantService, UserRepository userRepository) {
        this.merchantService = merchantService;
        this.userRepository = userRepository;
    }

    @GetMapping("/sandbox-keys")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sandboxKeys(Authentication auth) {
        try {
            User user = resolveUser(auth);
            Map<String, Object> result = merchantService.getOrCreateSandboxWorkspace(user.getId(), user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Sandbox API keys ready", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            User user = resolveUser(auth);
            Map<String, Object> result = merchantService.registerOrUpdateBusiness(
                    user.getId(),
                    asString(body.get("businessName")),
                    asString(body.getOrDefault("businessEmail", user.getEmail())),
                    asString(body.get("webhookUrl")),
                    asString(body.get("settlementMethod")),
                    asString(body.get("settlementPhone")),
                    asString(body.get("bankName")),
                    asString(body.get("bankAccount")));
            return ResponseEntity.ok(ApiResponse.success("Business activation profile saved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private User resolveUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }
        String principal = auth.getName();
        return userRepository.findByEmail(principal)
                .or(() -> {
                    try {
                        return userRepository.findById(Long.parseLong(principal));
                    } catch (NumberFormatException e) {
                        return java.util.Optional.empty();
                    }
                })
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
