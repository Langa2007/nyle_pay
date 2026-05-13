package com.nyle.nylepay.controllers;

import com.nyle.nylepay.dto.ApiResponse;
import com.nyle.nylepay.dto.LoginRequest;
import com.nyle.nylepay.dto.RegisterRequest;
import com.nyle.nylepay.services.AuditLogService;
import com.nyle.nylepay.services.OtpService;
import com.nyle.nylepay.services.UserService;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.exceptions.NylePayException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    private final com.nyle.nylepay.services.JwtService jwtService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final OtpService otpService;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService,
            org.springframework.security.authentication.AuthenticationManager authenticationManager,
            com.nyle.nylepay.services.JwtService jwtService,
            org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
            OtpService otpService,
            AuditLogService auditLogService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.otpService = otpService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {

        try {
            Map<String, Object> result = userService.registerUser(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getMpesaNumber(),
                    request.getCountryCode());

            Long userId = (Long) result.get("userId");
            auditLogService.logEvent(userId, "USER_REGISTERED",
                    "New user registered: " + request.getEmail(), "SUCCESS", httpServletRequest, null);

            return ResponseEntity.ok(ApiResponse.success(
                    "Registration successful. Please verify your email.",
                    result));

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during registration: {}", e.getMessage(), e);
            auditLogService.logEvent(null, "USER_REGISTRATION_FAILED",
                    "Registration failed for: " + request.getEmail() + " Reason: Internal Error",
                    "FAILED", httpServletRequest, null);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to complete registration. Please try again or contact support."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {

        Optional<User> userOpt = userService.getUserByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getAccountStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
                return ResponseEntity.status(423)
                        .body(ApiResponse.error("Account is " + user.getAccountStatus()
                                + ". Please contact NylePay support."));
            }
            if (userService.isLocked(user)) {
                return ResponseEntity.status(423) // Locked
                        .body(ApiResponse.error("Account is locked due to too many failed attempts. Try again later."));
            }
        }

        try {
            authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            User user = userOpt.orElseThrow(() -> new NylePayException("User not found after authentication"));
            userService.resetFailedAttempts(user);

            var userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            var jwtToken = jwtService.generateToken(userDetails);

            Map<String, Object> response = Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName(),
                    "token", jwtToken,
                    "accountNumber", user.getAccountNumber() != null ? user.getAccountNumber() : "",
                    "otpEnabled", user.isOtpEnabled());

            auditLogService.logLoginSuccess(user.getId(), httpServletRequest);

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (org.springframework.security.core.AuthenticationException e) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userService.incrementFailedAttempts(user);
                if (user.getFailedLoginAttempts() >= 5) {
                    auditLogService.logAccountLocked(user.getId(), user.getFailedLoginAttempts(), httpServletRequest);
                }
            }
            auditLogService.logLoginFailed(null, request.getEmail(), httpServletRequest);
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid credentials"));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during login: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to process login. Please try again later."));
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
                        "MPesa number verified successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid verification code"));
            }

        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during MPesa verification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Verification failed. Please try again later."));
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
                            Map.of("accessToken", accessToken)));
                }
            }
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid refresh token"));
        } catch (Exception e) {
            logger.error("Internal error during token refresh: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Token refresh failed. Please login again."));
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
                        "Password reset instructions sent to your email"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found or request failed"));
            }
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during forgot password: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Request failed. Please try again later."));
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
                        "Password reset successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid token or token expired"));
            }
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during password reset: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Reset failed. Please try again later."));
        }
    }

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestOtp(
            @RequestParam Long userId,
            @RequestParam String purpose,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {
        try {
            Map<String, Object> result = otpService.requestOtp(userId, purpose);
            auditLogService.logEvent(userId, "AUTH_OTP_REQUEST",
                    "OTP requested for purpose: " + purpose, "SUCCESS", httpServletRequest, Map.of("purpose", purpose));
            return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", result));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during OTP request: {}", e.getMessage(), e);
            auditLogService.logEvent(userId, "AUTH_OTP_REQUEST_FAILED",
                    "OTP request failed for purpose: " + purpose + " Reason: Internal Error",
                    "FAILED", httpServletRequest, Map.of("purpose", purpose));
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to send OTP. Please try again later."));
        }
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @RequestParam Long userId,
            @RequestParam String purpose,
            @RequestParam String otp,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {
        try {
            boolean valid = otpService.verifyOtp(userId, purpose, otp);
            if (valid) {
                auditLogService.logEvent(userId, "AUTH_OTP_VERIFY",
                        "OTP verified for purpose: " + purpose, "SUCCESS", httpServletRequest,
                        Map.of("purpose", purpose));
                return ResponseEntity.ok(ApiResponse.success(
                        "OTP verified successfully",
                        Map.of("verified", true, "purpose", purpose)));
            } else {
                auditLogService.logEvent(userId, "AUTH_OTP_VERIFY_FAILED",
                        "Invalid OTP entered for purpose: " + purpose, "FAILED", httpServletRequest,
                        Map.of("purpose", purpose));
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP"));
            }
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during OTP verification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Verification failed. Please try again later."));
        }
    }

    @PostMapping("/otp/enable")
    public ResponseEntity<ApiResponse<String>> enableOtp(@RequestParam Long userId) {
        try {
            otpService.enableOtp(userId);
            return ResponseEntity.ok(ApiResponse.success("2FA has been enabled for your account"));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during 2FA enable: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to enable 2FA. Please try again later."));
        }
    }

    @PostMapping("/otp/disable")
    public ResponseEntity<ApiResponse<String>> disableTwoFactorAuth(@RequestParam Long userId) {
        try {
            otpService.disableOtp(userId);
            return ResponseEntity.ok(ApiResponse.success("2FA has been disabled for your account"));
        } catch (NylePayException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Internal error during 2FA disable: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to disable 2FA. Please try again later."));
        }
    }
}
