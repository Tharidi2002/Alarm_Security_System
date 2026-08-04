package com.security.alarm.service;

import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.Company;
import com.security.alarm.entity.User;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlertLogRepository alertLogRepository;

    public PermissionService(UserRepository userRepository,
                             CompanyRepository companyRepository,
                             AlarmSystemRepository alarmSystemRepository,
                             AlertLogRepository alertLogRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
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
    // COMPANY ACCESS CHECKS
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
        // Admin can access any company
        if (isAdmin(username)) return true;
        
        // User can only access their own company
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null) return false;
            if (companyId == null) return true;
            return userCompanyId.equals(companyId);
        }
        
        return false;
    }

    public boolean canAccessCompany(String username, Company company) {
        if (company == null) return false;
        return canAccessCompany(username, company.getId());
    }

    // ============================================================
    // SYSTEM ACCESS CHECKS
    // ============================================================

    public boolean canAccessSystem(String username, Long systemId) {
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

    public boolean canAccessSystem(String username, AlarmSystem system) {
        if (system == null) return false;
        return canAccessSystem(username, system.getId());
    }

    public boolean canManageSystem(String username, Long systemId) {
        // Admin can manage any system
        if (isAdmin(username)) return true;
        
        // User can only manage systems in their company
        if (isUser(username)) {
            Long userCompanyId = getUserCompanyId(username);
            if (userCompanyId == null) return false;
            
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
            if (systemOpt.isEmpty()) return false;
            
            AlarmSystem system = systemOpt.get();
            if (system.getCompany() == null) return false;
            
            // DEBUG
            System.out.println("DEBUG: canManageSystem - User company: " + userCompanyId + 
                            ", System company: " + system.getCompany().getId() +
                            ", Result: " + system.getCompany().getId().equals(userCompanyId));
            
            return system.getCompany().getId().equals(userCompanyId);
        }
        
        return false;
    }

    public boolean canManageSystem(String username, AlarmSystem system) {
        if (system == null) return false;
        return canManageSystem(username, system.getId());
    }

    // ============================================================
    // ALERT ACCESS CHECKS
    // ============================================================

    public boolean canAccessAlert(String username, Long alertId) {
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

    public boolean canAccessAlert(String username, AlertLog alert) {
        if (alert == null) return false;
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
            if (companyId == null) return List.of();
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
            if (companyId == null) return List.of();
            
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
            List<Long> systemIds = systems.stream()
                .map(AlarmSystem::getId)
                .collect(java.util.stream.Collectors.toList());
            
            if (systemIds.isEmpty()) return List.of();
            return alertLogRepository.findAllByAlarmSystemIdInOrderByReceivedAtDesc(systemIds);
        }
        
        return List.of();
    }

    // ============================================================
    // VALIDATION METHODS (THROW EXCEPTIONS)
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

    // ============================================================
    // GET COMPANY FROM USERNAME
    // ============================================================

    public Long getCompanyIdOrThrow(String username) {
        if (isAdmin(username)) return null;
        
        Long companyId = getUserCompanyId(username);
        if (companyId == null) {
            throw new IllegalArgumentException("User has no company assigned");
        }
        return companyId;
    }

    // ============================================================
    // DEBUG: Get user info
    // ============================================================
    
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
}