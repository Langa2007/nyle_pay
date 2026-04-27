package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.LoginRequest;
import com.nyle.nylepay.dto.RegisterRequest;
import com.nyle.nylepay.services.OtpService;
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
    private final OtpService otpService;
    
    public AuthController(UserService userService, 
                          org.springframework.security.authentication.AuthenticationManager authenticationManager,
                          com.nyle.nylepay.services.JwtService jwtService,
                          org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
                          OtpService otpService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.otpService = otpService;
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
                "token", jwtToken,
                "accountNumber", user.getAccountNumber() != null ? user.getAccountNumber() : "",
                "otpEnabled", user.isOtpEnabled()
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

    // ─────────────────────────────────────────────────────────────────────────
    // 2FA / OTP Endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/otp/request
     * Request a 6-digit OTP for a specific purpose.
     */
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestOtp(
            @RequestParam Long userId,
            @RequestParam String purpose) {
        try {
            Map<String, Object> result = otpService.requestOtp(userId, purpose);
            return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/otp/verify
     * Verify a 6-digit OTP.
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @RequestParam Long userId,
            @RequestParam String purpose,
            @RequestParam String otp) {
        try {
            boolean valid = otpService.verifyOtp(userId, purpose, otp);
            if (valid) {
                return ResponseEntity.ok(ApiResponse.success(
                    "OTP verified successfully",
                    Map.of("verified", true, "purpose", purpose)
                ));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/otp/enable
     * Enable 2FA for a user account.
     */
    @PostMapping("/otp/enable")
    public ResponseEntity<ApiResponse<String>> enableOtp(@RequestParam Long userId) {
        try {
            otpService.enableOtp(userId);
            return ResponseEntity.ok(ApiResponse.success("2FA has been enabled for your account"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/otp/disable
     * Disable 2FA (requires OTP verification first via /otp/verify with purpose=DISABLE_2FA).
     */
    @PostMapping("/otp/disable")
    public ResponseEntity<ApiResponse<String>> disableOtp(@RequestParam Long userId) {
        try {
            otpService.disableOtp(userId);
            return ResponseEntity.ok(ApiResponse.success("2FA has been disabled for your account"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
