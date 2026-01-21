package com.nyle.nylepay.services;

import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import com.nyle.nylepay.models.Wallet;
import com.nyle.nylepay.repositories.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    public UserService(UserRepository userRepository, WalletRepository walletRepository,
            PasswordEncoder passwordEncoder, WalletService walletService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
    }

    @Transactional
    public Map<String, Object> registerUser(String fullName, String email, String password,
            String mpesaNumber, String countryCode) {

        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Invalid email format");
        }

        // Validate MPesa number if provided
        if (mpesaNumber != null && !mpesaNumber.isEmpty()) {
            if (!mpesaNumber.matches("^2547[0-9]{8}$")) {
                throw new RuntimeException("Invalid MPesa number format. Expected: 2547XXXXXXXX");
            }

            // Check if MPesa number is already registered
            if (userRepository.findByMpesaNumber(mpesaNumber).isPresent()) {
                throw new RuntimeException("MPesa number already registered");
            }
        }

        // Create user
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setMpesaNumber(mpesaNumber);
        user.setFiatBalance(BigDecimal.ZERO);
        user.setCryptoBalance(BigDecimal.ZERO);

        // Hash password before saving (if you add password field to User entity)
        // user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        // Create wallet with default balances
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());

        // Add default fiat balances based on country
        if ("KE".equals(countryCode)) {
            wallet.getBalances().put("KSH", new Wallet.Balance(BigDecimal.ZERO));
        }
        wallet.getBalances().put("USD", new Wallet.Balance(BigDecimal.ZERO));

        // Add EUR for European countries
        if (isEuropeanCountry(countryCode)) {
            wallet.getBalances().put("EUR", new Wallet.Balance(BigDecimal.ZERO));
        }

        // Add GBP for UK
        if ("GB".equals(countryCode)) {
            wallet.getBalances().put("GBP", new Wallet.Balance(BigDecimal.ZERO));
        }

        walletRepository.save(wallet);

        // Send welcome email (simulated)
        sendWelcomeEmail(email, fullName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully");
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("mpesaNumber", user.getMpesaNumber());
        response.put("requiresVerification", mpesaNumber != null);

        return response;
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User updateUserMpesaNumber(Long userId, String mpesaNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate MPesa number (Kenyan format)
        if (!mpesaNumber.matches("^2547[0-9]{8}$")) {
            throw new RuntimeException("Invalid MPesa number format. Expected format: 2547XXXXXXXX");
        }

        // Check if MPesa number is already in use by another user
        Optional<User> existingUser = userRepository.findByMpesaNumber(mpesaNumber);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new RuntimeException("MPesa number already registered to another user");
        }

        user.setMpesaNumber(mpesaNumber);
        User savedUser = userRepository.save(user);

        // Send verification SMS (simulated)
        sendMpesaVerificationSMS(mpesaNumber);

        return savedUser;
    }

    @Transactional
    public User updateUserBankAccount(Long userId, String bankAccountNumber, String bankName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate bank account number (basic validation)
        if (bankAccountNumber == null || bankAccountNumber.trim().isEmpty()) {
            throw new RuntimeException("Bank account number is required");
        }

        if (bankName == null || bankName.trim().isEmpty()) {
            throw new RuntimeException("Bank name is required");
        }

        // Remove any spaces or special characters from account number
        String cleanedAccountNumber = bankAccountNumber.replaceAll("[^0-9]", "");

        if (cleanedAccountNumber.length() < 5) {
            throw new RuntimeException("Invalid bank account number");
        }

        user.setBankAccountNumber(cleanedAccountNumber);
        User savedUser = userRepository.save(user);

        // TODO: Initiate bank account verification process

        return savedUser;
    }

    @Transactional
    public Map<String, Object> createCryptoWalletForUser(Long userId) throws Exception {
        return walletService.createCryptoWallet(userId);
    }

    public boolean verifyMpesaNumber(Long userId, String verificationCode) {
        // Implement MPesa number verification logic
        // This would typically involve sending an SMS with the code
        // and verifying it matches what the user entered

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // For demo purposes, accept any 6-digit code
        // In production, you would check against a stored verification code
        if (verificationCode != null && verificationCode.matches("\\d{6}")) {
            // Mark MPesa as verified in user profile
            // You could add a 'verifiedMpesa' boolean field to User entity
            return true;
        }

        return false;
    }

    @Transactional
    public User updateUserProfile(Long userId, Map<String, Object> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("fullName")) {
            String fullName = (String) updates.get("fullName");
            if (fullName != null && fullName.length() >= 2) {
                user.setFullName(fullName);
            }
        }

        if (updates.containsKey("phoneNumber")) {
            String phoneNumber = (String) updates.get("phoneNumber");
            // Basic phone validation
            if (phoneNumber != null && phoneNumber.matches("^\\+?[0-9]{10,15}$")) {
                // You might want to add a phoneNumber field to User entity
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public boolean deactivateUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Instead of deleting, mark as inactive
            // You could add an 'active' boolean field to User entity
            // user.setActive(false);
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public Map<String, Object> getUserStats(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        Map<String, BigDecimal> balances = walletService.getBalances(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("fullName", user.getFullName());
        stats.put("email", user.getEmail());
        stats.put("mpesaNumber", user.getMpesaNumber());
        stats.put("bankAccount", user.getBankAccountNumber() != null ? "****" +
                user.getBankAccountNumber().substring(Math.max(0, user.getBankAccountNumber().length() - 4))
                : "Not set");
        stats.put("cryptoAddress",
                user.getCryptoAddress() != null ? user.getCryptoAddress().substring(0, 10) + "..." : "Not set");
        stats.put("balances", balances);
        stats.put("totalBalanceUSD", calculateTotalBalanceUSD(balances));
        stats.put("registrationDate", "2024-01-01"); // You would store this in User entity
        stats.put("kycStatus", "NOT_VERIFIED"); // Add KYC status field to User

        return stats;
    }

    // Helper methods
    private boolean isEuropeanCountry(String countryCode) {
        String[] euCountries = { "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES",
                "FI", "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU",
                "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK" };
        for (String euCode : euCountries) {
            if (euCode.equals(countryCode)) {
                return true;
            }
        }
        return false;
    }

    private void sendWelcomeEmail(String email, String fullName) {
        // In production, implement email sending using JavaMailSender or service
        System.out.println("Sending welcome email to: " + email);
        System.out.println("Dear " + fullName + ", welcome to NylePay!");
    }

    private void sendMpesaVerificationSMS(String mpesaNumber) {
        // In production, integrate with SMS gateway
        System.out.println("Sending verification SMS to: " + mpesaNumber);
        System.out.println("Your NylePay verification code: 123456");
    }

    private BigDecimal calculateTotalBalanceUSD(Map<String, BigDecimal> balances) {
        // Simple conversion for demo - in production, use real exchange rates
        BigDecimal totalUSD = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            String currency = entry.getKey();
            BigDecimal amount = entry.getValue();

            switch (currency) {
                case "USD":
                    totalUSD = totalUSD.add(amount);
                    break;
                case "KSH":
                    totalUSD = totalUSD.add(amount.multiply(BigDecimal.valueOf(0.0067)));
                    break;
                case "EUR":
                    totalUSD = totalUSD.add(amount.multiply(BigDecimal.valueOf(1.1)));
                    break;
                case "GBP":
                    totalUSD = totalUSD.add(amount.multiply(BigDecimal.valueOf(1.3)));
                    break;
                case "ETH":
                    totalUSD = totalUSD.add(amount.multiply(BigDecimal.valueOf(3000)));
                    break;
                case "BTC":
                    totalUSD = totalUSD.add(amount.multiply(BigDecimal.valueOf(45000)));
                    break;
            }
        }

        return totalUSD;
    }

    public boolean requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Generate reset token
            String resetToken = generateResetToken();

            // Store token with expiration (you'd need a PasswordResetToken entity)
            // passwordResetTokenRepository.save(new PasswordResetToken(user, resetToken));

            // Send reset email
            sendPasswordResetEmail(user.getEmail(), resetToken);

            return true;
        }

        return false;
    }

    public boolean resetPassword(String token, String newPassword) {
        // Verify token and expiration
        // PasswordResetToken resetToken =
        // passwordResetTokenRepository.findByToken(token);

        // if (resetToken != null && !resetToken.isExpired()) {
        // User user = resetToken.getUser();
        // user.setPassword(passwordEncoder.encode(newPassword));
        // userRepository.save(user);
        //
        // // Invalidate token
        // passwordResetTokenRepository.delete(resetToken);
        //
        // return true;
        // }

        return false;
    }

    private String generateResetToken() {
        // Generate a secure random token
        return java.util.UUID.randomUUID().toString();
    }

    private void sendPasswordResetEmail(String email, String token) {
        // Implement email sending
        System.out.println("Password reset link for " + email + ": https://nylepay.com/reset-password?token=" + token);
    }
}
