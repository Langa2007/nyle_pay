package com.nyle.nylepay.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Sandbox health and diagnostics endpoint.
 *
 * Accessible without authentication so developers can quickly verify
 * that the local backend is running and sandbox mode is active.
 *
 * GET /api/sandbox/health
 */
@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    @Value("${nylepay.sandbox.enabled:false}")
    private boolean sandboxEnabled;

    @Value("${spring.application.name:nylepay}")
    private String appName;

    /**
     * Health check for the sandbox environment.
     * Safe to expose without authentication — returns no sensitive data.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",        "UP",
            "app",           appName,
            "sandbox",       sandboxEnabled,
            "environment",   sandboxEnabled ? "SANDBOX" : "PRODUCTION",
            "timestamp",     LocalDateTime.now().toString(),
            "message",       sandboxEnabled
                                 ? "Sandbox mode active. No real money moves. All payment gateways are simulated."
                                 : "Production mode. Real transactions are enabled.",
            "endpoints", Map.of(
                "register",     "POST /api/auth/register",
                "login",        "POST /api/auth/login",
                "merchant",     "POST /api/merchant/register",
                "paymentLink",  "POST /api/merchant/payment-link",
                "swagger",      "/swagger-ui.html"
            )
        ));
    }
}
