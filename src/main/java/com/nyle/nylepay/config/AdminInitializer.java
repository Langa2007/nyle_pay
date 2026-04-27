package com.nyle.nylepay.config;

import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.full-name:Admin User}")
    private String adminFullName;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Guard: skip entirely if admin credentials are not configured
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            logger.warn("⚠ Admin credentials not configured. Set ADMIN_EMAIL and ADMIN_PASSWORD " +
                    "environment variables to seed the admin account.");
            return;
        }

        // Password strength validation — reject anything under 12 characters
        if (adminPassword.length() < 12) {
            logger.error("❌ ADMIN_PASSWORD must be at least 12 characters. Admin seeding skipped.");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setFullName(adminFullName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");

            userRepository.save(admin);
            logger.info("✅ Default admin user created: {}", adminEmail);
        } else {
            // Ensure existing account has ADMIN role
            userRepository.findByEmail(adminEmail).ifPresent(user -> {
                if (!"ADMIN".equals(user.getRole())) {
                    user.setRole("ADMIN");
                    userRepository.save(user);
                    logger.info("✅ Upgraded existing user to ADMIN: {}", adminEmail);
                }
            });
        }
    }
}
