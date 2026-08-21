package com.security.alarm.config;

import com.security.alarm.entity.RegistrationAuditLog;
import com.security.alarm.entity.RetentionConfig;
import com.security.alarm.entity.SystemConfig;
import com.security.alarm.entity.User;
import com.security.alarm.repository.RegistrationAuditLogRepository;
import com.security.alarm.repository.RetentionConfigRepository;
import com.security.alarm.repository.SystemConfigRepository;
import com.security.alarm.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;
    private final RetentionConfigRepository retentionConfigRepository;
    private final UserRepository userRepository;
    private final RegistrationAuditLogRepository registrationAuditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SystemConfigRepository systemConfigRepository,
                           RetentionConfigRepository retentionConfigRepository,
                           UserRepository userRepository,
                           RegistrationAuditLogRepository registrationAuditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.systemConfigRepository = systemConfigRepository;
        this.retentionConfigRepository = retentionConfigRepository;
        this.userRepository = userRepository;
        this.registrationAuditLogRepository = registrationAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Create MASTER_SECRET_CODE
        createSecretCode();
        
        // Create RETENTION CONFIG
        createRetentionConfig();
        
        // Fix all existing users
        fixAllUsers();
        
        System.out.println("✅ Data initialization completed!");
    }

    private void createSecretCode() {
        if (systemConfigRepository.findByConfigKey("MASTER_SECRET_CODE").isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey("MASTER_SECRET_CODE");
            config.setConfigValue("ALARM-2024-SECURE-KEY");
            systemConfigRepository.save(config);
            System.out.println("✅ MASTER_SECRET_CODE created");
        }
    }

    private void createRetentionConfig() {
        String[][] configs = {
            {"RETENTION_DAYS", "90", "Number of days before alert expires"},
            {"GRACE_PERIOD_DAYS", "5", "Grace period before final deletion"},
            {"AUTO_EXPORT_ENABLED", "true", "Enable auto-export before deletion"},
            {"ARCHIVE_RETENTION_MONTHS", "6", "How long to keep archives"}
        };

        for (String[] config : configs) {
            if (retentionConfigRepository.findByConfigKey(config[0]).isEmpty()) {
                RetentionConfig rc = new RetentionConfig();
                rc.setConfigKey(config[0]);
                rc.setConfigValue(config[1]);
                rc.setDescription(config[2]);
                retentionConfigRepository.save(rc);
                System.out.println("✅ Retention config created: " + config[0] + " = " + config[1]);
            }
        }
    }

    private void fixAllUsers() {
        System.out.println("🔄 Fixing all users...");
        
        List<User> allUsers = userRepository.findAll();
        int fixedCount = 0;
        
        for (User user : allUsers) {
            if (fixUserRegistrationMethod(user)) {
                fixedCount++;
            }
        }
        
        System.out.println("✅ Fixed " + fixedCount + " users");
    }

    private boolean fixUserRegistrationMethod(User user) {
        String username = user.getUsername();
        
        if (user.getRegistrationMethod() != null && !user.getRegistrationMethod().isEmpty()) {
            boolean shouldBeSuperAdmin = "FORM".equals(user.getRegistrationMethod()) && "ADMIN".equalsIgnoreCase(user.getRole());
            boolean isCurrentlySuperAdmin = user.getIsSuperAdmin() != null && user.getIsSuperAdmin();
            
            if (shouldBeSuperAdmin != isCurrentlySuperAdmin) {
                user.setIsSuperAdmin(shouldBeSuperAdmin);
                userRepository.save(user);
                return true;
            }
            return false;
        }

        String method = determineRegistrationMethod(user);
        if (method == null) {
            method = "ADMIN".equalsIgnoreCase(user.getRole()) ? "FORM" : "ADMIN_PANEL";
        }

        user.setRegistrationMethod(method);
        user.setIsSuperAdmin("FORM".equals(method) && "ADMIN".equalsIgnoreCase(user.getRole()));
        user.setIsActive(true);
        userRepository.save(user);
        
        return true;
    }

    private String determineRegistrationMethod(User user) {
        String username = user.getUsername();
        String role = user.getRole();
        
        // Check if FORM (Super Admin)
        Optional<RegistrationAuditLog> formLog = registrationAuditLogRepository
            .findByUsernameAndMethodForm(username);
        if (formLog.isPresent()) {
            return "FORM";
        }
        
        // Check if ADMIN_PANEL
        Optional<RegistrationAuditLog> panelLog = registrationAuditLogRepository
            .findByUsernameAndMethodAdminPanel(username);
        if (panelLog.isPresent()) {
            return "ADMIN_PANEL";
        }
        
        // If ADMIN and no audit log, check if first admin
        if ("ADMIN".equalsIgnoreCase(role)) {
            long adminCount = userRepository.countAdmins();
            if (adminCount <= 1) {
                return "FORM";
            }
        }
        
        // Default based on role
        return "ADMIN".equalsIgnoreCase(role) ? "FORM" : "ADMIN_PANEL";
    }
}