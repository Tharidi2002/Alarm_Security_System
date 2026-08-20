package com.security.alarm.config;

import com.security.alarm.entity.SystemConfig;
import com.security.alarm.entity.User;
import com.security.alarm.entity.RegistrationAuditLog;
import com.security.alarm.repository.SystemConfigRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.RegistrationAuditLogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final RegistrationAuditLogRepository registrationAuditLogRepository;

    public DataInitializer(SystemConfigRepository systemConfigRepository,
                           UserRepository userRepository,
                           RegistrationAuditLogRepository registrationAuditLogRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.userRepository = userRepository;
        this.registrationAuditLogRepository = registrationAuditLogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Create MASTER_SECRET_CODE if not exists
        createSecretCode();
        
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
            System.out.println("✅ MASTER_SECRET_CODE created: ALARM-2024-SECURE-KEY");
        } else {
            System.out.println("✅ MASTER_SECRET_CODE already exists");
        }
    }

    @Transactional
    protected void fixAllUsers() {
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

    /**
     * Fix a single user's registration method
     * Returns true if user was updated
     */
    @Transactional
    protected boolean fixUserRegistrationMethod(User user) {
        String username = user.getUsername();
        
        // Skip if already has registration method
        if (user.getRegistrationMethod() != null && !user.getRegistrationMethod().isEmpty()) {
            // But check if is_super_admin is correct
            boolean shouldBeSuperAdmin = "FORM".equals(user.getRegistrationMethod()) && "ADMIN".equalsIgnoreCase(user.getRole());
            boolean isCurrentlySuperAdmin = user.getIsSuperAdmin() != null && user.getIsSuperAdmin();
            
            if (shouldBeSuperAdmin != isCurrentlySuperAdmin) {
                user.setIsSuperAdmin(shouldBeSuperAdmin);
                userRepository.save(user);
                System.out.println("✅ Fixed is_super_admin for: " + username + " -> " + shouldBeSuperAdmin);
                return true;
            }
            return false;
        }

        // Determine registration method
        String method = determineRegistrationMethod(user);
        
        if (method == null) {
            // Default: If user is ADMIN, set as FORM (Super Admin)
            // If user is USER, set as ADMIN_PANEL
            method = "ADMIN".equalsIgnoreCase(user.getRole()) ? "FORM" : "ADMIN_PANEL";
            System.out.println("⚠️ No registration method found for " + username + 
                             ", defaulting to: " + method);
        }

        // Update user
        user.setRegistrationMethod(method);
        user.setIsSuperAdmin("FORM".equals(method) && "ADMIN".equalsIgnoreCase(user.getRole()));
        user.setIsActive(true);
        userRepository.save(user);
        
        System.out.println("✅ Fixed user: " + username + 
                         " (role=" + user.getRole() + 
                         ", method=" + method + 
                         ", super=" + user.getIsSuperAdmin() + ")");
        return true;
    }

    /**
     * Determine registration method from audit log
     */
    private String determineRegistrationMethod(User user) {
        String username = user.getUsername();
        String role = user.getRole();
        
        // 1. Check if FORM (Super Admin)
        Optional<RegistrationAuditLog> formLog = registrationAuditLogRepository
            .findByUsernameAndMethodForm(username);
        if (formLog.isPresent()) {
            return "FORM";
        }
        
        // 2. Check if ADMIN_PANEL
        Optional<RegistrationAuditLog> panelLog = registrationAuditLogRepository
            .findByUsernameAndMethodAdminPanel(username);
        if (panelLog.isPresent()) {
            return "ADMIN_PANEL";
        }
        
        // 3. If ADMIN and no audit log, check if first admin
        if ("ADMIN".equalsIgnoreCase(role)) {
            // Check if this is the first admin
            long adminCount = userRepository.countAdmins();
            if (adminCount <= 1) {
                // First admin is always FORM
                return "FORM";
            }
        }
        
        // 4. Default based on role
        return "ADMIN".equalsIgnoreCase(role) ? "FORM" : "ADMIN_PANEL";
    }
}