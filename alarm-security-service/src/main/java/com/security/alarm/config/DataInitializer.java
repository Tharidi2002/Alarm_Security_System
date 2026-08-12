package com.security.alarm.config;

import com.security.alarm.entity.SystemConfig;
import com.security.alarm.entity.User;
import com.security.alarm.repository.SystemConfigRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.RegistrationAuditLogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final RegistrationAuditLogRepository registrationAuditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SystemConfigRepository systemConfigRepository,
                           UserRepository userRepository,
                           RegistrationAuditLogRepository registrationAuditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.systemConfigRepository = systemConfigRepository;
        this.userRepository = userRepository;
        this.registrationAuditLogRepository = registrationAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create MASTER_SECRET_CODE if not exists
        if (systemConfigRepository.findByConfigKey("MASTER_SECRET_CODE").isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey("MASTER_SECRET_CODE");
            config.setConfigValue("ALARM-2024-SECURE-KEY");
            systemConfigRepository.save(config);
            System.out.println("✅ MASTER_SECRET_CODE created: ALARM-2024-SECURE-KEY");
        } else {
            System.out.println("✅ MASTER_SECRET_CODE already exists");
        }

        // ============================================================
        // FIX: Update existing users with registration method
        // ============================================================
        System.out.println("🔄 Checking and updating existing users...");
        
        // Update all users
        userRepository.findAll().forEach(user -> {
            updateUserRegistrationMethod(user.getUsername());
        });

        System.out.println("✅ All users updated with registration method!");
    }

    private void updateUserRegistrationMethod(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("⚠️ User not found: " + username);
            return;
        }
        
        User user = userOpt.get();
        
        // Skip if already set
        if (user.getRegistrationMethod() != null && !user.getRegistrationMethod().isEmpty()) {
            System.out.println("✅ User " + username + " already has registration method: " + user.getRegistrationMethod());
            return;
        }
        
        // Check audit log to determine method
        boolean isFormAdmin = registrationAuditLogRepository
            .findByUsernameAndMethodForm(username)
            .isPresent();
        
        boolean isAdminPanelAdmin = registrationAuditLogRepository
            .findByUsernameAndMethodAdminPanel(username)
            .isPresent();
        
        if (isFormAdmin) {
            user.setRegistrationMethod("FORM");
            user.setIsSuperAdmin(true);
            user.setIsActive(true);
            userRepository.save(user);
            // System.out.println("✅ Updated " + username + ": FORM (Super Admin)");
        } else if (isAdminPanelAdmin) {
            user.setRegistrationMethod("ADMIN_PANEL");
            user.setIsSuperAdmin(false);
            user.setIsActive(true);
            userRepository.save(user);
            System.out.println("✅ Updated " + username + ": ADMIN_PANEL");
        } else {
            // If no audit log found, check role
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                user.setRegistrationMethod("FORM");
                user.setIsSuperAdmin(true);
                user.setIsActive(true);
                userRepository.save(user);
                System.out.println("✅ Updated " + username + ": FORM (default for ADMIN)");
            } else {
                user.setRegistrationMethod("ADMIN_PANEL");
                user.setIsSuperAdmin(false);
                user.setIsActive(true);
                userRepository.save(user);
                System.out.println("✅ Updated " + username + ": ADMIN_PANEL (default for USER)");
            }
        }
    }
}