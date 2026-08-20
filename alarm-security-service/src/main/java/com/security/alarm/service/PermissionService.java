package com.security.alarm.service;

import com.security.alarm.entity.*;
import com.security.alarm.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PermissionService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlertLogRepository alertLogRepository;
    private final RegistrationAuditLogRepository registrationAuditLogRepository;

    public PermissionService(UserRepository userRepository,
            CompanyRepository companyRepository,
            AlarmSystemRepository alarmSystemRepository,
            AlertLogRepository alertLogRepository,
            RegistrationAuditLogRepository registrationAuditLogRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
        this.registrationAuditLogRepository = registrationAuditLogRepository;
    }

    // ============================================================
    // USER ROLE CHECKS
    // ============================================================

    public boolean isAdmin(String username) {
        if (username == null || username.isEmpty()) return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.isPresent() && "ADMIN".equalsIgnoreCase(userOpt.get().getRole());
    }

    public boolean isUser(String username) {
        if (username == null || username.isEmpty()) return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole());
    }

    public User getUser(String username) {
        if (username == null || username.isEmpty()) return null;
        return userRepository.findByUsername(username).orElse(null);
    }

    // ============================================================
    // NEW: ACTIVE USER CHECKS
    // ============================================================

    /**
     * Check if user is active
     */
    public boolean isUserActive(String username) {
        if (username == null || username.isEmpty()) return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        return user.getIsActive() != null && user.getIsActive();
    }

    /**
     * Check if user is active - with User object
     */
    public boolean isUserActive(User user) {
        if (user == null) return false;
        return user.getIsActive() != null && user.getIsActive();
    }

    /**
     * Validate user is active - throws exception if inactive
     */
    public void validateUserActive(String username) {
        if (!isUserActive(username)) {
            User user = getUser(username);
            String reason = user != null && user.getInactivationReason() != null 
                ? user.getInactivationReason() 
                : "Account deactivated";
            throw new SecurityException("Your account is inactive. Reason: " + reason + ". Please contact administrator.");
        }
    }

    /**
     * Get inactive user details
     */
    public Map<String, Object> getInactiveUserDetails(String username) {
        User user = getUser(username);
        if (user == null) return null;
        
        Map<String, Object> details = new HashMap<>();
        details.put("isActive", user.getIsActive());
        details.put("inactivatedAt", user.getInactivatedAt());
        details.put("inactivatedBy", user.getInactivatedBy());
        details.put("inactivationReason", user.getInactivationReason());
        details.put("inactivationDescription", user.getInactivationDescription());
        return details;
    }

    // ============================================================
    // REGISTRATION METHOD CHECKS
    // ============================================================

    public boolean isFormAdmin(String username) {
        if (username == null || username.isEmpty()) return false;
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        
        User user = userOpt.get();
        
        // Check active status first
        if (!isUserActive(user)) {
            return false;
        }
        
        if (user.getRegistrationMethod() != null) {
            if ("FORM".equals(user.getRegistrationMethod())) {
                return true;
            }
        }
        
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin()) {
            return true;
        }
        
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        
        try {
            Optional<RegistrationAuditLog> formLog = registrationAuditLogRepository
                .findByUsernameAndMethodForm(username);
            if (formLog.isPresent()) {
                user.setRegistrationMethod("FORM");
                user.setIsSuperAdmin(true);
                userRepository.save(user);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check FORM audit log for " + username);
        }
        
        return false;
    }

    public boolean isAdminPanelAdmin(String username) {
        if (username == null || username.isEmpty()) return false;
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        
        User user = userOpt.get();
        
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        
        // Check active status first
        if (!isUserActive(user)) {
            return false;
        }
        
        if (user.getRegistrationMethod() != null) {
            if ("ADMIN_PANEL".equals(user.getRegistrationMethod())) {
                return true;
            }
        }
        
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin()) {
            return false;
        }
        
        try {
            Optional<RegistrationAuditLog> panelLog = registrationAuditLogRepository
                .findByUsernameAndMethodAdminPanel(username);
            if (panelLog.isPresent()) {
                user.setRegistrationMethod("ADMIN_PANEL");
                user.setIsSuperAdmin(false);
                userRepository.save(user);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check ADMIN_PANEL audit log for " + username);
        }
        
        return false;
    }

    public String getRegistrationMethod(String username) {
        if (username == null || username.isEmpty()) return null;
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return null;
        
        User user = userOpt.get();
        
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        
        if (user.getRegistrationMethod() != null && !user.getRegistrationMethod().isEmpty()) {
            return user.getRegistrationMethod();
        }
        
        try {
            Optional<RegistrationAuditLog> formLog = registrationAuditLogRepository
                    .findByUsernameAndMethodForm(username);
            if (formLog.isPresent()) {
                user.setRegistrationMethod("FORM");
                user.setIsSuperAdmin(true);
                userRepository.save(user);
                return "FORM";
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check FORM audit log for " + username);
        }
        
        try {
            Optional<RegistrationAuditLog> panelLog = registrationAuditLogRepository
                    .findByUsernameAndMethodAdminPanel(username);
            if (panelLog.isPresent()) {
                user.setRegistrationMethod("ADMIN_PANEL");
                user.setIsSuperAdmin(false);
                userRepository.save(user);
                return "ADMIN_PANEL";
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check ADMIN_PANEL audit log for " + username);
        }
        
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin()) {
            user.setRegistrationMethod("FORM");
            userRepository.save(user);
            return "FORM";
        }
        
        return null;
    }

    public boolean isSuperAdmin(String username) {
        return isFormAdmin(username);
    }

    public long countFormAdmins() {
        long count = userRepository.findAll().stream()
            .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
            .filter(u -> isUserActive(u))
            .filter(u -> {
                if (u.getRegistrationMethod() != null) {
                    return "FORM".equals(u.getRegistrationMethod());
                }
                return u.getIsSuperAdmin() != null && u.getIsSuperAdmin();
            })
            .count();
        
        if (count == 0) {
            count = registrationAuditLogRepository.countFormAdmins();
        }
        
        return count;
    }

    // ============================================================
    // ADMIN MANAGEMENT PERMISSIONS - WITH ACTIVE CHECKS
    // ============================================================

    public boolean canManageAdmin(String currentUsername, String targetUsername) {
        if (!isAdmin(targetUsername)) return false;
        if (!isAdmin(currentUsername)) return false;
        
        // Check if current user is active
        if (!isUserActive(currentUsername)) return false;
        
        if (currentUsername.equals(targetUsername)) return true;
        
        String currentMethod = getRegistrationMethod(currentUsername);
        String targetMethod = getRegistrationMethod(targetUsername);
        
        if (currentMethod == null) return false;
        
        if ("FORM".equals(currentMethod)) {
            return "ADMIN_PANEL".equals(targetMethod);
        }
        
        if ("ADMIN_PANEL".equals(currentMethod)) {
            return false;
        }
        
        return false;
    }

    public boolean canDeleteAdmin(String currentUsername, String targetUsername) {
        if (!canManageAdmin(currentUsername, targetUsername)) return false;
        
        if (isFormAdmin(targetUsername)) {
            long formAdminCount = countFormAdmins();
            if (formAdminCount <= 1) return false;
        }
        
        return true;
    }

    public boolean canToggleAdminStatus(String currentUsername, String targetUsername) {
        return canManageAdmin(currentUsername, targetUsername);
    }

    public boolean canResetAdminPassword(String currentUsername, String targetUsername) {
        return canManageAdmin(currentUsername, targetUsername);
    }

    public boolean canCreateAdmin(String currentUsername) {
        return isFormAdmin(currentUsername) && isUserActive(currentUsername);
    }

    public boolean canCreateUser(String currentUsername) {
        return isAdmin(currentUsername) && isUserActive(currentUsername);
    }

    public String getAdminTypeLabel(String username) {
        String method = getRegistrationMethod(username);
        if ("FORM".equals(method)) {
            return "🔑 Super Admin (System Registered)";
        } else if ("ADMIN_PANEL".equals(method)) {
            return "🛠️ Operational Admin (Created by Admin)";
        }
        return "Unknown";
    }

    // ============================================================
    // COMPANY ACCESS CHECKS - WITH ACTIVE CHECK
    // ============================================================

    public Long getUserCompanyId(String username) {
        if (username == null || username.isEmpty()) return null;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();
        if (user.getCompany() == null) return null;
        return user.getCompany().getId();
    }

    public Company getUserCompany(String username) {
        if (username == null || username.isEmpty()) return null;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return null;
        return userOpt.get().getCompany();
    }

    public boolean canAccessCompany(String username, Long companyId) {
        if (!isUserActive(username)) return false;
        if (isAdmin(username)) return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null) return false;
            if (companyId == null) return true;
            return userCompanyId.equals(companyId);
        }
        return false;
    }

    // ============================================================
    // SYSTEM ACCESS CHECKS - WITH ACTIVE CHECK
    // ============================================================

    public boolean canAccessSystem(String username, Long systemId) {
        if (!isUserActive(username)) return false;
        if (isAdmin(username)) return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null) return false;
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
            if (systemOpt.isEmpty()) return false;
            AlarmSystem system = systemOpt.get();
            if (system.getCompany() == null) return false;
            return system.getCompany().getId().equals(userCompanyId);
        }
        return false;
    }

    public boolean canManageSystem(String username, Long systemId) {
        if (!isUserActive(username)) return false;
        if (isAdmin(username)) return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null) return false;
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
            if (systemOpt.isEmpty()) return false;
            AlarmSystem system = systemOpt.get();
            if (system.getCompany() == null) return false;
            return system.getCompany().getId().equals(userCompanyId);
        }
        return false;
    }

    // ============================================================
    // ALERT ACCESS CHECKS - WITH ACTIVE CHECK
    // ============================================================

    public boolean canAccessAlert(String username, Long alertId) {
        if (!isUserActive(username)) return false;
        if (isAdmin(username)) return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null) return false;
            Optional<AlertLog> alertOpt = alertLogRepository.findById(alertId);
            if (alertOpt.isEmpty()) return false;
            AlertLog alert = alertOpt.get();
            if (alert.getAlarmSystem() == null) return false;
            if (alert.getAlarmSystem().getCompany() == null) return false;
            return alert.getAlarmSystem().getCompany().getId().equals(userCompanyId);
        }
        return false;
    }

    public boolean canResolveAlert(String username, Long alertId) {
        return canAccessAlert(username, alertId);
    }

    // ============================================================
    // GET FILTERED DATA - WITH ACTIVE CHECK
    // ============================================================

    public List<AlarmSystem> getAccessibleSystems(String username) {
        if (!isUserActive(username)) return List.of();
        if (isAdmin(username)) {
            return alarmSystemRepository.findAll();
        }
        if (isUser(username)) {
            Long companyId = getUserCompanyId(username);
            if (companyId == null) return List.of();
            return alarmSystemRepository.findByCompanyId(companyId);
        }
        return List.of();
    }

    public List<AlertLog> getAccessibleAlerts(String username) {
        if (!isUserActive(username)) return List.of();
        if (isAdmin(username)) {
            return alertLogRepository.findAllActiveAlerts();
        }
        if (isUser(username)) {
            Long companyId = getUserCompanyId(username);
            if (companyId == null) return List.of();
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
            List<Long> systemIds = systems.stream()
                    .map(AlarmSystem::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (systemIds.isEmpty()) return List.of();
            return alertLogRepository.findAllByAlarmSystemIdInAndNotRejected(systemIds);
        }
        return List.of();
    }

    // ============================================================
    // VALIDATION METHODS - WITH ACTIVE CHECK
    // ============================================================

    public void validateCompanyAccess(String username, Long companyId) {
        if (!canAccessCompany(username, companyId)) {
            throw new SecurityException("Access denied: You do not have permission to access this company");
        }
    }

    public void validateSystemAccess(String username, Long systemId) {
        if (!canAccessSystem(username, systemId)) {
            throw new SecurityException("Access denied: You do not have permission to access this system");
        }
    }

    public void validateAlertAccess(String username, Long alertId) {
        if (!canAccessAlert(username, alertId)) {
            throw new SecurityException("Access denied: You do not have permission to access this alert");
        }
    }

    public Long getCompanyIdOrThrow(String username) {
        if (isAdmin(username)) return null;
        Long companyId = getUserCompanyId(username);
        if (companyId == null) {
            throw new IllegalArgumentException("User has no company assigned");
        }
        return companyId;
    }

    public void debugUser(String username) {
        if (username == null || username.isEmpty()) {
            System.out.println("DEBUG: username is null or empty");
            return;
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("DEBUG: User not found: " + username);
            return;
        }
        User user = userOpt.get();
        System.out.println("DEBUG: User: " + user.getUsername() +
                ", Role: " + user.getRole() +
                ", Registration Method: " + user.getRegistrationMethod() +
                ", isSuperAdmin: " + user.getIsSuperAdmin() +
                ", isActive: " + user.getIsActive() +
                ", inactivatedBy: " + user.getInactivatedBy() +
                ", Company: " + (user.getCompany() != null ? user.getCompany().getCompanyName() : "NULL") +
                ", Company ID: " + (user.getCompany() != null ? user.getCompany().getId() : "NULL"));
    }
}