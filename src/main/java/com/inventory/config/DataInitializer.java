package com.inventory.config;

import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.repository.RoleRepository;
import com.inventory.repository.UserRepository;
import com.inventory.util.Constants;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role adminRole = ensureRole(Constants.ROLE_ADMIN);
        ensureRole(Constants.ROLE_EMPLOYEE);
        if (userRepository.existsByRoleName(Constants.ROLE_ADMIN)) {
            return;
        }
        boolean generated = adminPassword == null || adminPassword.isBlank();
        String password = generated ? randomPassword() : adminPassword;
        if (password.length() < 6) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 6 characters");
        }
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setStatus(Constants.ACTIVE);
        admin.setRole(adminRole);
        userRepository.save(admin);
        log.warn("==================================================================");
        log.warn("Initial administrator account created");
        log.warn("Email    : {}", admin.getEmail());
        if (generated) {
            log.warn("Password : {}  (shown only once, change it in Settings)", password);
        } else {
            log.warn("Password : taken from the ADMIN_PASSWORD environment variable");
        }
        log.warn("==================================================================");
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
