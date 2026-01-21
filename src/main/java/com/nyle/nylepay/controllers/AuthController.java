package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.LoginRequest;
import com.nyle.nylepay.dto.RegisterRequest;
import com.nyle.nylepay.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {
        
        try {
            Map<String, Object> result = userService.registerUser(
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                request.getMpesaNumber(),
                request.getCountryCode()
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Registration successful. Please verify your email.", 
                result
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {
        
        try {
            // TODO: Implement JWT authentication
            // For now, just check if user exists
            var user = userService.getUserByEmail(request.getEmail());
            
            if (user.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid credentials"));
            }
            
            Map<String, Object> response = Map.of(
                "userId", user.get().getId(),
                "email", user.get().getEmail(),
                "fullName", user.get().getFullName(),
                "token", "jwt-token-placeholder" // TODO: Generate JWT
            );
            
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/verify-mpesa")
    public ResponseEntity<ApiResponse<String>> verifyMpesa(
            @RequestParam Long userId,
            @RequestParam String verificationCode) {
        
        try {
            boolean verified = userService.verifyMpesaNumber(userId, verificationCode);
            
            if (verified) {
                return ResponseEntity.ok(ApiResponse.success(
                    "MPesa number verified successfully"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid verification code"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @RequestParam String refreshToken) {
        
        // TODO: Implement token refresh logic
        return ResponseEntity.ok(ApiResponse.success(
            "Token refreshed successfully", 
            "new-jwt-token-placeholder"
        ));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // TODO: Implement logout logic (invalidate token)
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestParam String email) {
        
        try {
            var user = userService.getUserByEmail(email);
            
            if (user.isPresent()) {
                // TODO: Send password reset email
                return ResponseEntity.ok(ApiResponse.success(
                    "Password reset instructions sent to your email"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User not found"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        
        // TODO: Implement password reset logic
        return ResponseEntity.ok(ApiResponse.success(
            "Password reset successfully"
        ));
    }
}
