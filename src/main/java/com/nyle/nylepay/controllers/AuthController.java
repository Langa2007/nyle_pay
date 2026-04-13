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
    private final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    private final com.nyle.nylepay.services.JwtService jwtService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    
    public AuthController(UserService userService, 
                          org.springframework.security.authentication.AuthenticationManager authenticationManager,
                          com.nyle.nylepay.services.JwtService jwtService,
                          org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
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
            authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            
            var user = userService.getUserByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
                
            var userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            var jwtToken = jwtService.generateToken(userDetails);
            
            Map<String, Object> response = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "token", jwtToken
            );
            
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
            
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.status(401)
                .body(ApiResponse.error("Invalid credentials"));
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
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @RequestParam String refreshToken) {
        
        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail != null) {
                var userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(refreshToken, userDetails)) {
                    String accessToken = jwtService.generateToken(userDetails);
                    return ResponseEntity.ok(ApiResponse.success(
                        "Token refreshed successfully", 
                        Map.of("accessToken", accessToken)
                    ));
                }
            }
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid refresh token"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // For stateless JWT, client-side is responsible for token disposal.
        // Server-side could implement a token blacklist here if needed.
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestParam String email) {
        
        try {
            boolean requested = userService.requestPasswordReset(email);
            if (requested) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Password reset instructions sent to your email"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User not found or request failed"));
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
        
        try {
            boolean reset = userService.resetPassword(token, newPassword);
            if (reset) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Password reset successfully"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid token or token expired"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
