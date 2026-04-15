package com.nyle.nylepay.config;

import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "fidellanga67@gmail.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setFullName("Admin User");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Stephanie@2007"));
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
