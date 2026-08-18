package com.security.alarm.service;

import com.security.alarm.entity.*;
import com.security.alarm.repository.*;

import org.springframework.stereotype.Service;

import java.util.List;
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
        if (username == null || username.isEmpty())
            return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.isPresent() && "ADMIN".equalsIgnoreCase(userOpt.get().getRole());
    }

    public boolean isUser(String username) {
        if (username == null || username.isEmpty())
            return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole());
    }

    public User getUser(String username) {
        if (username == null || username.isEmpty())
            return null;
        return userRepository.findByUsername(username).orElse(null);
    }

    // ============================================================
    // COMPANY ACCESS CHECKS
    // ============================================================

    public Long getUserCompanyId(String username) {
        if (username == null || username.isEmpty())
            return null;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty())
            return null;
        User user = userOpt.get();
        if (user.getCompany() == null)
            return null;
        return user.getCompany().getId();
    }

    public Company getUserCompany(String username) {
        if (username == null || username.isEmpty())
            return null;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty())
            return null;
        return userOpt.get().getCompany();
    }

    public boolean canAccessCompany(String username, Long companyId) {
        if (isAdmin(username))
            return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null)
                return false;
            if (companyId == null)
                return true;
            return userCompanyId.equals(companyId);
        }
        return false;
    }

    public boolean canAccessCompany(String username, Company company) {
        if (company == null)
            return false;
        return canAccessCompany(username, company.getId());
    }

    // ============================================================
    // SYSTEM ACCESS CHECKS
    // ============================================================

    public boolean canAccessSystem(String username, Long systemId) {
        if (isAdmin(username))
            return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null)
                return false;
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
            if (systemOpt.isEmpty())
                return false;
            AlarmSystem system = systemOpt.get();
            if (system.getCompany() == null)
                return false;
            return system.getCompany().getId().equals(userCompanyId);
        }
        return false;
    }

    public boolean canAccessSystem(String username, AlarmSystem system) {
        if (system == null)
            return false;
        return canAccessSystem(username, system.getId());
    }

    public boolean canManageSystem(String username, Long systemId) {
        if (isAdmin(username))
            return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null)
                return false;
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
            if (systemOpt.isEmpty())
                return false;
            AlarmSystem system = systemOpt.get();
            if (system.getCompany() == null)
                return false;
            return system.getCompany().getId().equals(userCompanyId);
        }
        return false;
    }

    public boolean canManageSystem(String username, AlarmSystem system) {
        if (system == null)
            return false;
        return canManageSystem(username, system.getId());
    }

    // ============================================================
    // ALERT ACCESS CHECKS
    // ============================================================

    public boolean canAccessAlert(String username, Long alertId) {
        if (isAdmin(username))
            return true;
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null)
                return false;
            Optional<AlertLog> alertOpt = alertLogRepository.findById(alertId);
            if (alertOpt.isEmpty())
                return false;
            AlertLog alert = alertOpt.get();
            if (alert.getAlarmSystem() == null)
                return false;
            if (alert.getAlarmSystem().getCompany() == null)
                return false;
            return alert.getAlarmSystem().getCompany().getId().equals(userCompanyId);
        }
        return false;
    }

    public boolean canAccessAlert(String username, AlertLog alert) {
        if (alert == null)
            return false;
        return canAccessAlert(username, alert.getId());
    }

    public boolean canResolveAlert(String username, Long alertId) {
        return canAccessAlert(username, alertId);
    }

    // ============================================================
    // GET FILTERED DATA
    // ============================================================

    public List<AlarmSystem> getAccessibleSystems(String username) {
        if (isAdmin(username)) {
            return alarmSystemRepository.findAll();
        }
        if (isUser(username)) {
            Long companyId = getUserCompanyId(username);
            if (companyId == null)
                return List.of();
            return alarmSystemRepository.findByCompanyId(companyId);
        }
        return List.of();
    }

    public List<AlertLog> getAccessibleAlerts(String username) {
        if (isAdmin(username)) {
            return alertLogRepository.findAllByOrderByReceivedAtDesc();
        }
        if (isUser(username)) {
            Long companyId = getUserCompanyId(username);
            if (companyId == null)
                return List.of();
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
            List<Long> systemIds = systems.stream()
                    .map(AlarmSystem::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (systemIds.isEmpty())
                return List.of();
            return alertLogRepository.findAllByAlarmSystemIdInOrderByReceivedAtDesc(systemIds);
        }
        return List.of();
    }

    // ============================================================
    // VALIDATION METHODS
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
        if (isAdmin(username))
            return null;
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
                ", Company: " + (user.getCompany() != null ? user.getCompany().getCompanyName() : "NULL") +
                ", Company ID: " + (user.getCompany() != null ? user.getCompany().getId() : "NULL"));
    }

    // ============================================================
    // ADMIN ACCESS CONTROL METHODS (FIXED)
    // ============================================================

    public boolean isFormAdmin(String username) {
        if (username == null || username.isEmpty())
            return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty())
            return false;
        User user = userOpt.get();
        if (!"ADMIN".equalsIgnoreCase(user.getRole()))
            return false;
        Optional<RegistrationAuditLog> logOpt = registrationAuditLogRepository
                .findByUsernameAndMethodForm(username);
        return logOpt.isPresent();
    }

    public boolean isAdminPanelAdmin(String username) {
        if (username == null || username.isEmpty())
            return false;
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty())
            return false;
        User user = userOpt.get();
        if (!"ADMIN".equalsIgnoreCase(user.getRole()))
            return false;
        Optional<RegistrationAuditLog> logOpt = registrationAuditLogRepository
                .findByUsernameAndMethodAdminPanel(username);
        return logOpt.isPresent();
    }

    /**
     * Get registration method for a user
     * Returns: "FORM", "ADMIN_PANEL", or null
     * FIXED: Check if user exists and has method set in User entity first
     */
    public String getRegistrationMethod(String username) {
        if (username == null || username.isEmpty())
            return null;
        
        // First check if user exists
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        User user = userOpt.get();
        
        // If user is not ADMIN, return null
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        
        // Check if user has registration_method set in User entity
        if (user.getRegistrationMethod() != null && !user.getRegistrationMethod().isEmpty()) {
            return user.getRegistrationMethod();
        }
        
        // Fallback: Check audit log (with error handling)
        try {
            Optional<RegistrationAuditLog> formLog = registrationAuditLogRepository
                    .findByUsernameAndMethodForm(username);
            if (formLog.isPresent()) {
                return "FORM";
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check FORM audit log for " + username);
        }
        
        try {
            Optional<RegistrationAuditLog> panelLog = registrationAuditLogRepository
                    .findByUsernameAndMethodAdminPanel(username);
            if (panelLog.isPresent()) {
                return "ADMIN_PANEL";
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check ADMIN_PANEL audit log for " + username);
        }
        
        return null;
    }

    /**
     * Check if current user can manage (reset password, active/inactive, delete) a target admin
     * 
     * RULES (FIXED):
     * 1. FORM Admin can manage ADMIN_PANEL Admins ONLY (NOT other FORM Admins)
     * 2. ADMIN_PANEL Admin can ONLY manage themselves
     * 3. ADMIN_PANEL Admin CANNOT manage other admins
     * 4. FORM Admins CANNOT manage other FORM Admins
     */
    public boolean canManageAdmin(String currentUsername, String targetUsername) {
        // If target is not an admin, return false
        if (!isAdmin(targetUsername)) {
            return false;
        }

        // If current user is not an admin, return false
        if (!isAdmin(currentUsername)) {
            return false;
        }

        // If trying to manage self, always allow (for password reset)
        if (currentUsername.equals(targetUsername)) {
            return true;
        }

        // Get registration methods
        String currentMethod = getRegistrationMethod(currentUsername);
        String targetMethod = getRegistrationMethod(targetUsername);

        // ============================================================
        // FIX: FORM Admin can ONLY manage ADMIN_PANEL Admins
        // ============================================================
        if ("FORM".equals(currentMethod)) {
            // FORM Admin can manage ADMIN_PANEL Admins ONLY
            return "ADMIN_PANEL".equals(targetMethod);
        }

        // If current user is ADMIN_PANEL Admin, they CANNOT manage other admins
        if ("ADMIN_PANEL".equals(currentMethod)) {
            return false; // Can only manage self (already handled above)
        }

        return false;
    }

    /**
     * Check if current user can delete a target admin
     * Same rules as canManageAdmin, but additional check:
     * - Cannot delete the last FORM Admin (system needs at least one super admin)
     */
    public boolean canDeleteAdmin(String currentUsername, String targetUsername) {
        // First check if they can manage the admin
        if (!canManageAdmin(currentUsername, targetUsername)) {
            return false;
        }

        // If target is FORM Admin, check if it's the last one
        if (isFormAdmin(targetUsername)) {
            long formAdminCount = registrationAuditLogRepository.countFormAdmins();
            // Cannot delete if this is the last FORM Admin
            if (formAdminCount <= 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if current user can change status (active/inactive) of a target admin
     */
    public boolean canToggleAdminStatus(String currentUsername, String targetUsername) {
        // Same as canManageAdmin
        return canManageAdmin(currentUsername, targetUsername);
    }

    public boolean isSuperAdmin(String username) {
        return isFormAdmin(username);
    }

    public boolean canCreateAdmin(String currentUsername) {
        return isFormAdmin(currentUsername);
    }

    public boolean canCreateUser(String currentUsername) {
        return isAdmin(currentUsername);
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
    // CHECK IF COMPANY IS ACTIVE
    // ============================================================
    public boolean isCompanyActive(Long companyId) {
        if (companyId == null) return false;
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) return false;
        return "ACTIVE".equals(companyOpt.get().getStatus());
    }

    public boolean isCompanyActive(Company company) {
        if (company == null) return false;
        return "ACTIVE".equals(company.getStatus());
    }

    public boolean isCompanyInactive(Long companyId) {
        return !isCompanyActive(companyId);
    }

    public boolean canAccessCompanyData(String username) {
        if (isAdmin(username)) return true;
        if (isUser(username)) {
            Long companyId = getUserCompanyId(username);
            if (companyId == null) return false;
            return isCompanyActive(companyId);
        }
        return false;
    }

    public String getCompanyStatusMessage(String username) {
        if (isAdmin(username)) return null;
        if (isUser(username)) {
            Long companyId = getUserCompanyId(username);
            if (companyId == null) return "No company assigned";
            Optional<Company> companyOpt = companyRepository.findById(companyId);
            if (companyOpt.isEmpty()) return "Company not found";
            Company company = companyOpt.get();
            if ("INACTIVE".equals(company.getStatus())) {
                return "⚠️ Your company (" + company.getCompanyName() + 
                    ") is currently INACTIVE. Please contact administrator.";
            }
            return null;
        }
        return null;
    }
}