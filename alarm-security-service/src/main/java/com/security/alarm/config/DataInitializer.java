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
        
        // Update admin user (created via FORM)
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            if (admin.getRegistrationMethod() == null || admin.getRegistrationMethod().isEmpty()) {
                // Check audit log to determine method
                boolean isFormAdmin = registrationAuditLogRepository
                    .findByUsernameAndMethodForm("admin")
                    .isPresent();
                
                if (isFormAdmin) {
                    admin.setRegistrationMethod("FORM");
                    admin.setIsSuperAdmin(true);
                    admin.setIsActive(true);
                    userRepository.save(admin);
                    System.out.println("✅ Updated admin: FORM (Super Admin)");
                } else {
                    admin.setRegistrationMethod("ADMIN_PANEL");
                    admin.setIsSuperAdmin(false);
                    admin.setIsActive(true);
                    userRepository.save(admin);
                    System.out.println("✅ Updated admin: ADMIN_PANEL");
                }
            }
        }

        // Update developer user (created via FORM with secret code)
        Optional<User> developerOpt = userRepository.findByUsername("developer");
        if (developerOpt.isPresent()) {
            User developer = developerOpt.get();
            if (developer.getRegistrationMethod() == null || developer.getRegistrationMethod().isEmpty()) {
                boolean isFormAdmin = registrationAuditLogRepository
                    .findByUsernameAndMethodForm("developer")
                    .isPresent();
                
                if (isFormAdmin) {
                    developer.setRegistrationMethod("FORM");
                    developer.setIsSuperAdmin(true);
                    developer.setIsActive(true);
                    userRepository.save(developer);
                    System.out.println("✅ Updated developer: FORM (Super Admin)");
                } else {
                    developer.setRegistrationMethod("ADMIN_PANEL");
                    developer.setIsSuperAdmin(false);
                    developer.setIsActive(true);
                    userRepository.save(developer);
                    System.out.println("✅ Updated developer: ADMIN_PANEL");
                }
            }
        }

        // Update user1 (created via ADMIN_PANEL)
        Optional<User> user1Opt = userRepository.findByUsername("user1");
        if (user1Opt.isPresent()) {
            User user1 = user1Opt.get();
            if (user1.getRegistrationMethod() == null || user1.getRegistrationMethod().isEmpty()) {
                boolean isFormAdmin = registrationAuditLogRepository
                    .findByUsernameAndMethodForm("user1")
                    .isPresent();
                
                if (isFormAdmin) {
                    user1.setRegistrationMethod("FORM");
                    user1.setIsSuperAdmin(true);
                    user1.setIsActive(true);
                    user1.setRole("ADMIN");
                    userRepository.save(user1);
                    System.out.println("✅ Updated user1: FORM (Super Admin)");
                } else {
                    user1.setRegistrationMethod("ADMIN_PANEL");
                    user1.setIsSuperAdmin(false);
                    user1.setIsActive(true);
                    userRepository.save(user1);
                    System.out.println("✅ Updated user1: ADMIN_PANEL");
                }
            }
        }

        // Update admin2 (created via ADMIN_PANEL)
        Optional<User> admin2Opt = userRepository.findByUsername("admin2");
        if (admin2Opt.isPresent()) {
            User admin2 = admin2Opt.get();
            if (admin2.getRegistrationMethod() == null || admin2.getRegistrationMethod().isEmpty()) {
                boolean isFormAdmin = registrationAuditLogRepository
                    .findByUsernameAndMethodForm("admin2")
                    .isPresent();
                
                if (isFormAdmin) {
                    admin2.setRegistrationMethod("FORM");
                    admin2.setIsSuperAdmin(true);
                    admin2.setIsActive(true);
                    userRepository.save(admin2);
                    System.out.println("✅ Updated admin2: FORM (Super Admin)");
                } else {
                    admin2.setRegistrationMethod("ADMIN_PANEL");
                    admin2.setIsSuperAdmin(false);
                    admin2.setIsActive(true);
                    userRepository.save(admin2);
                    System.out.println("✅ Updated admin2: ADMIN_PANEL");
                }
            }
        }

        System.out.println("✅ All users updated with registration method!");
    }
}