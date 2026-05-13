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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, WalletRepository walletRepository,
            PasswordEncoder passwordEncoder, WalletService walletService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.emailService = emailService;
    }

    @Transactional
    public Map<String, Object> registerUser(String fullName, String email, String password,
            String mpesaNumber, String countryCode) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Invalid email format");
        }

        if (mpesaNumber != null && !mpesaNumber.isEmpty()) {
            if (!mpesaNumber.matches("^2547[0-9]{8}$")) {
                throw new RuntimeException("Invalid MPesa number format. Expected: 2547XXXXXXXX");
            }

            if (userRepository.findByMpesaNumber(mpesaNumber).isPresent()) {
                throw new RuntimeException("MPesa number already registered");
            }
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setMpesaNumber(mpesaNumber);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());

        if ("KE".equals(countryCode)) {
            wallet.getBalances().put("KSH", new Wallet.Balance(BigDecimal.ZERO));
        }
        wallet.getBalances().put("USD", new Wallet.Balance(BigDecimal.ZERO));

        if (isEuropeanCountry(countryCode)) {
            wallet.getBalances().put("EUR", new Wallet.Balance(BigDecimal.ZERO));
        }

        if ("GB".equals(countryCode)) {
            wallet.getBalances().put("GBP", new Wallet.Balance(BigDecimal.ZERO));
        }

        walletRepository.save(wallet);

        emailService.sendWelcomeEmail(user);

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

    public boolean isLocked(User user) {
        if (user.getAccountStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            return true;
        }
        if (user.getLockoutUntil() == null) {
            return false;
        }
        boolean locked = user.getLockoutUntil().isAfter(java.time.LocalDateTime.now());
        if (!locked) {
            // Keep the failed-attempt counter until the next successful login resets it.
            user.setLockoutUntil(null);
            userRepository.save(user);
        }
        return locked;
    }

    @Transactional
    public User applyLegalHold(Long userId, String action, String authority,
            String courtOrderReference, String reason, java.time.LocalDateTime holdUntil) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        switch (normalizedAction) {
            case "FREEZE":
            case "BLOCK":
                user.setAccountStatus(normalizedAction.equals("BLOCK") ? "BLOCKED" : "FROZEN");
                user.setLegalHoldAuthority(authority);
                user.setLegalHoldReference(courtOrderReference);
                user.setLegalHoldReason(reason);
                user.setLegalHoldUntil(holdUntil);
                break;
            case "UNFREEZE":
            case "UNBLOCK":
            case "RELEASE":
                user.setAccountStatus("ACTIVE");
                user.setLegalHoldAuthority(authority);
                user.setLegalHoldReference(courtOrderReference);
                user.setLegalHoldReason(reason);
                user.setLegalHoldUntil(null);
                break;
            default:
                throw new RuntimeException("Unsupported legal hold action: " + action);
        }

        user.setLegalHoldUpdatedAt(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void incrementFailedAttempts(User user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= 5) {
            user.setLockoutUntil(java.time.LocalDateTime.now().plusMinutes(30));
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public User updateUserMpesaNumber(Long userId, String mpesaNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!mpesaNumber.matches("^2547[0-9]{8}$")) {
            throw new RuntimeException("Invalid MPesa number format. Expected format: 2547XXXXXXXX");
        }

        Optional<User> existingUser = userRepository.findByMpesaNumber(mpesaNumber);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new RuntimeException("MPesa number already registered to another user");
        }

        user.setMpesaNumber(mpesaNumber);
        User savedUser = userRepository.save(user);

        sendMpesaVerificationSMS(mpesaNumber);

        return savedUser;
    }

    @Transactional
    public User updateUserBankAccount(Long userId, String bankAccountNumber, String bankName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bankAccountNumber == null || bankAccountNumber.trim().isEmpty()) {
            throw new RuntimeException("Bank account number is required");
        }

        if (bankName == null || bankName.trim().isEmpty()) {
            throw new RuntimeException("Bank name is required");
        }

        String cleanedAccountNumber = bankAccountNumber.replaceAll("[^0-9]", "");

        if (cleanedAccountNumber.length() < 5) {
            throw new RuntimeException("Invalid bank account number");
        }

        user.setBankAccountNumber(cleanedAccountNumber);
        user.setBankName(bankName);
        user.setBankVerificationStatus("PENDING");
        
        User savedUser = userRepository.save(user);

        // Bank account verification is deferred to the provider integration.
        log.info("Bank account update initiated for user {}: {} ({})", userId, bankName, cleanedAccountNumber);

        return savedUser;
    }

    @Transactional
    public Map<String, Object> createCryptoWalletForUser(Long userId) throws Exception {
        return walletService.createCryptoWallet(userId);
    }

    public boolean verifyMpesaNumber(Long userId, String verificationCode) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (verificationCode != null && verificationCode.matches("\\d{6}")) {
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
            if (phoneNumber != null && phoneNumber.matches("^\\+?[0-9]{10,15}$")) {
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public boolean deactivateUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
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

    public java.util.List<User> searchUsers(String email, String phone) {
        if (email != null && !email.isEmpty()) {
            return userRepository.findByEmail(email).map(java.util.List::of).orElse(java.util.List.of());
        }
        if (phone != null && !phone.isEmpty()) {
            return userRepository.findByMpesaNumber(phone).map(java.util.List::of).orElse(java.util.List.of());
        }
        return java.util.List.of();
    }

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


    private void sendMpesaVerificationSMS(String mpesaNumber) {
        System.out.println("Sending verification SMS to: " + mpesaNumber);
        System.out.println("Your NylePay verification code: 123456");
    }

    private BigDecimal calculateTotalBalanceUSD(Map<String, BigDecimal> balances) {
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

            String resetToken = generateResetToken();


            emailService.sendPasswordResetEmail(user, resetToken);

            return true;
        }

        return false;
    }

    public boolean resetPassword(String token, String newPassword) {
        return false;
    }

    private String generateResetToken() {
        return java.util.UUID.randomUUID().toString();
    }

}


