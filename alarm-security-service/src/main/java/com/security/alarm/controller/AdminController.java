package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.Company;
import com.security.alarm.entity.RegistrationAuditLog;
import com.security.alarm.entity.AlarmZone;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.repository.RegistrationAuditLogRepository;
import com.security.alarm.repository.AlarmZoneRepository;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AdminController {

    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlarmZoneRepository alarmZoneRepository;
    private final PermissionService permissionService;
    private final AlertLogRepository alertLogRepository;
    private final RegistrationAuditLogRepository registrationAuditLogRepository;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    public AdminController(UserRepository userRepository,
                           UserSystemRepository userSystemRepository,
                           AlarmSystemRepository alarmSystemRepository,
                           CompanyRepository companyRepository,
                           PasswordEncoder passwordEncoder,
                           AlarmZoneRepository alarmZoneRepository,
                           PermissionService permissionService,
                           AlertLogRepository alertLogRepository,
                           RegistrationAuditLogRepository registrationAuditLogRepository) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.alarmZoneRepository = alarmZoneRepository;
        this.permissionService = permissionService;
        this.alertLogRepository = alertLogRepository;
        this.registrationAuditLogRepository = registrationAuditLogRepository;
    }

    // ============================================================
    // GET CURRENT USER INFO
    // ============================================================
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("registrationMethod", user.getRegistrationMethod());
        response.put("isSuperAdmin", user.getIsSuperAdmin() != null && user.getIsSuperAdmin());
        response.put("isActive", user.getIsActive() != null ? user.getIsActive() : true);
        
        if (user.getCompany() != null) {
            response.put("companyId", user.getCompany().getId());
            response.put("companyName", user.getCompany().getCompanyName());
            response.put("companyCode", user.getCompany().getCompanyCode());
        }
        
        if ("USER".equalsIgnoreCase(user.getRole())) {
            List<AlarmSystem> systems = permissionService.getAccessibleSystems(username);
            response.put("systems", systems.stream().map(s -> {
                Map<String, Object> sys = new HashMap<>();
                sys.put("id", s.getId());
                sys.put("systemCode", s.getSystemCode());
                return sys;
            }).collect(Collectors.toList()));
        }
        
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // USER MANAGEMENT
    // ============================================================

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) Long companyId,
                                      @RequestParam(required = false) String username) {
        if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
            Long userCompanyId = permissionService.getUserCompanyId(username);
            if (userCompanyId == null) {
                return ResponseEntity.badRequest().body("User has no company assigned");
            }
            List<User> users = userRepository.findByCompanyId(userCompanyId);
            return ResponseEntity.ok(formatUsers(users));
        }
        
        List<User> users;
        if (companyId != null && companyId > 0) {
            users = userRepository.findByCompanyId(companyId);
        } else {
            users = userRepository.findAll();
        }
        
        return ResponseEntity.ok(formatUsers(users));
    }

    private List<Map<String, Object>> formatUsers(List<User> users) {
        Optional<User> firstAdmin = userRepository.findFirstByRoleOrderByIdAsc("ADMIN");
        Long firstAdminId = firstAdmin.map(User::getId).orElse(null);
        
        return users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            map.put("isFirstAdmin", u.getId().equals(firstAdminId));
            
            // Get registration method
            String regMethod = u.getRegistrationMethod();
            if (regMethod == null || regMethod.isEmpty()) {
                regMethod = permissionService.getRegistrationMethod(u.getUsername());
            }
            map.put("registrationMethod", regMethod);
            map.put("isActive", u.getIsActive() != null ? u.getIsActive() : true);
            map.put("isSuperAdmin", u.getIsSuperAdmin() != null && u.getIsSuperAdmin());
            
            if (u.getCompany() != null) {
                map.put("companyId", u.getCompany().getId());
                map.put("companyName", u.getCompany().getCompanyName());
            }
            
            List<UserSystem> mappings = userSystemRepository.findAllByUserId(u.getId());
            List<Map<String, Object>> assigned = mappings.stream()
                .map(m -> alarmSystemRepository.findById(m.getSystemId()))
                .filter(Optional::isPresent)
                .map(opt -> {
                    Map<String, Object> s = new HashMap<>();
                    s.put("id", opt.get().getId());
                    s.put("systemCode", opt.get().getSystemCode());
                    return s;
                })
                .collect(Collectors.toList());
            map.put("assignedSystems", assigned);
            return map;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // CREATE USER - WITH ADMIN RESTRICTIONS
    // ============================================================
    
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request,
                                        @RequestParam(required = false) String adminUsername,
                                        HttpServletRequest httpRequest) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");
        Long companyId = request.get("companyId") != null ? 
            Long.valueOf(request.get("companyId").toString()) : null;
        
        // Basic validation
        if (username == null || password == null || role == null) {
            return ResponseEntity.badRequest().body("Username, password and role are required");
        }
        
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        
        if ("USER".equalsIgnoreCase(role) && (companyId == null || companyId == 0)) {
            return ResponseEntity.badRequest().body("Company is required for USER role");
        }
        
        // ============================================================
        // CRITICAL: ADMIN CREATION RESTRICTION
        // Only FORM Admins (Super Admins) can create other ADMINs
        // ============================================================
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (adminUsername == null || adminUsername.isEmpty()) {
                return ResponseEntity.status(403).body("Only admins can create admin accounts");
            }
            
            // Check if current user is FORM Admin (Super Admin)
            if (!permissionService.isFormAdmin(adminUsername)) {
                return ResponseEntity.status(403).body(
                    "Access denied: Only Super Admins (FORM Admins) can create other admin accounts"
                );
            }
        }
        
        Company company = null;
        if (companyId != null && companyId > 0) {
            Optional<Company> companyOpt = companyRepository.findById(companyId);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Company not found");
            }
            company = companyOpt.get();
        }
        
        // Create user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role.toUpperCase());
        newUser.setCompany(company);
        newUser.setIsActive(true);
        
        // ============================================================
        // Set registration method based on who created the user
        // ============================================================
        if ("ADMIN".equalsIgnoreCase(role)) {
            // Only FORM Admins can create admins
            // So registration_method = ADMIN_PANEL (created by admin)
            newUser.setRegistrationMethod("ADMIN_PANEL");
            newUser.setIsSuperAdmin(false);
        } else {
            // USER created by any admin -> ADMIN_PANEL
            newUser.setRegistrationMethod("ADMIN_PANEL");
            newUser.setIsSuperAdmin(false);
        }
        
        User saved = userRepository.save(newUser);
        
        // ============================================================
        // AUTO-ASSIGN: All systems from user's company (for USER role)
        // ============================================================
        if ("USER".equalsIgnoreCase(role) && company != null && company.getId() != null) {
            List<AlarmSystem> companySystems = alarmSystemRepository.findActiveByCompanyId(company.getId());
            for (AlarmSystem system : companySystems) {
                UserSystem us = new UserSystem();
                us.setUserId(saved.getId());
                us.setSystemId(system.getId());
                userSystemRepository.save(us);
            }
        }
        
        // ============================================================
        // AUDIT LOG
        // ============================================================
        String clientIp = getClientIp(httpRequest);
        RegistrationAuditLog log = new RegistrationAuditLog();
        log.setUsername(username);
        log.setRole(role);
        log.setRegisteredBy(adminUsername != null ? adminUsername : "SYSTEM");
        log.setRegisteredFromIp(clientIp);
        log.setMethod("ADMIN_PANEL");
        log.setNotes("User created via Admin Panel by: " + (adminUsername != null ? adminUsername : "SYSTEM") +
                     (company != null ? " | Company: " + company.getCompanyName() : ""));
        registrationAuditLogRepository.save(log);
        
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }
        
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/assign")
    public ResponseEntity<?> assignSystems(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Long> systemIds = request.get("systemIds");
        if (systemIds == null) {
            return ResponseEntity.badRequest().body("systemIds is required");
        }
        
        userSystemRepository.deleteByUserId(id);
        
        for (Long systemId : systemIds) {
            UserSystem us = new UserSystem();
            us.setUserId(id);
            us.setSystemId(systemId);
            userSystemRepository.save(us);
        }
        
        return ResponseEntity.ok("Systems assigned successfully");
    }

    // ============================================================
    // SYSTEM MANAGEMENT
    // ============================================================

    private String generateNextSystemCode() {
        Optional<String> latestCodeOpt = alarmSystemRepository.findLatestSystemCode();
        if (latestCodeOpt.isEmpty()) {
            return "ALARM-Z8B-01";
        }
        String latestCode = latestCodeOpt.get();
        try {
            String[] parts = latestCode.split("-");
            if (parts.length >= 3) {
                int lastNumber = Integer.parseInt(parts[2]);
                int nextNumber = lastNumber + 1;
                return String.format("ALARM-Z8B-%02d", nextNumber);
            }
        } catch (NumberFormatException e) {}
        return "ALARM-Z8B-01";
    }

    @GetMapping("/systems/next-code")
    public ResponseEntity<?> getNextSystemCode() {
        try {
            String nextCode = generateNextSystemCode();
            Map<String, Object> response = new HashMap<>();
            response.put("nextCode", nextCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating system code: " + e.getMessage());
        }
    }

    @GetMapping("/systems")
    public ResponseEntity<?> getSystems(@RequestParam(required = false) Long companyId,
                                        @RequestParam(required = false) String username) {
        System.out.println("DEBUG: getSystems called with username: " + username + ", companyId: " + companyId);
        
        if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
            Long userCompanyId = permissionService.getUserCompanyId(username);
            System.out.println("DEBUG: User company ID: " + userCompanyId);
            if (userCompanyId == null) {
                return ResponseEntity.badRequest().body("User has no company assigned");
            }
            List<AlarmSystem> systems = alarmSystemRepository.findActiveByCompanyId(userCompanyId);
            System.out.println("DEBUG: Found " + systems.size() + " systems for user's company");
            return ResponseEntity.ok(systems);
        }
        
        List<AlarmSystem> systems;
        if (companyId != null && companyId > 0) {
            systems = alarmSystemRepository.findActiveByCompanyId(companyId);
        } else {
            systems = alarmSystemRepository.findAllActive();
        }
        System.out.println("DEBUG: Found " + systems.size() + " systems total");
        return ResponseEntity.ok(systems);
    }

    @PostMapping("/systems")
    public ResponseEntity<?> createSystem(@RequestBody AlarmSystem system,
                                          @RequestParam(required = false) Long companyId,
                                          @RequestParam(required = false) String username) {
        if (alarmSystemRepository.countActive() >= 5) {
            return ResponseEntity.badRequest().body("System registration limit reached. A maximum of 5 active systems can be registered.");
        }

        if (system.getLocation() == null || system.getLocation().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Location is required");
        }
        if (system.getSimNumber() == null || system.getSimNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("SIM number is required");
        }

        if (alarmSystemRepository.findBySimNumber(system.getSimNumber()).isPresent()) {
            return ResponseEntity.badRequest().body("SIM number already registered to another system");
        }

        Long targetCompanyId = null;
        
        if (companyId != null && companyId > 0) {
            targetCompanyId = companyId;
        }
        
        if (username != null && !username.isEmpty()) {
            User user = permissionService.getUser(username);
            if (user != null) {
                if ("USER".equalsIgnoreCase(user.getRole())) {
                    if (user.getCompany() == null) {
                        return ResponseEntity.badRequest().body("User has no company assigned. Cannot create system.");
                    }
                    targetCompanyId = user.getCompany().getId();
                    System.out.println("DEBUG: USER role - forcing company ID: " + targetCompanyId);
                } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    if (system.getCompany() != null && system.getCompany().getId() != null) {
                        targetCompanyId = system.getCompany().getId();
                    }
                    System.out.println("DEBUG: ADMIN role - using company ID: " + targetCompanyId);
                }
            }
        }
        
        if (targetCompanyId == null && system.getCompany() != null && system.getCompany().getId() != null) {
            targetCompanyId = system.getCompany().getId();
        }

        Company company = null;
        if (targetCompanyId != null && targetCompanyId > 0) {
            Optional<Company> companyOpt = companyRepository.findById(targetCompanyId);
            if (companyOpt.isPresent()) {
                company = companyOpt.get();
                System.out.println("DEBUG: Company found: " + company.getCompanyName() + " (ID: " + company.getId() + ")");
            } else {
                return ResponseEntity.badRequest().body("Company not found with ID: " + targetCompanyId);
            }
        }

        // Generate unique system code
        String newSystemCode = generateNextSystemCode();
        int counter = 0;
        while (alarmSystemRepository.findBySystemCode(newSystemCode).isPresent() && counter < 100) {
            try {
                String[] parts = newSystemCode.split("-");
                if (parts.length >= 3) {
                    int num = Integer.parseInt(parts[2]);
                    newSystemCode = String.format("ALARM-Z8B-%02d", num + 1);
                } else {
                    newSystemCode = "ALARM-Z8B-01";
                }
            } catch (NumberFormatException e) {
                newSystemCode = "ALARM-Z8B-01";
            }
            counter++;
        }

        AlarmSystem newSystem = new AlarmSystem();
        newSystem.setSystemCode(newSystemCode);
        newSystem.setLocation(system.getLocation().trim());
        if (system.getDescription() != null) newSystem.setDescription(system.getDescription().trim());
        newSystem.setSimNumber(system.getSimNumber().trim());
        newSystem.setStatus(system.getStatus() != null ? system.getStatus() : "ACTIVE");
        newSystem.setLastStatusChangedAt(LocalDateTime.now());
        newSystem.setCompany(company);
        newSystem.setDeleted(false);
        newSystem.setArchived(false);
        
        newSystem.setPanelSimNumber(system.getSimNumber().trim());
        newSystem.setPanelPassword("8888");
        newSystem.setDisarmCommand("8888#2A");
        newSystem.setArmCommand("8888#1A");
        newSystem.setSirenStopCommand("8888#5A");
        newSystem.setSirenStatus("OFF");

        AlarmSystem saved = alarmSystemRepository.save(newSystem);
        createDefaultZones(saved);
        
        // Auto-assign system to all users in company
        if (company != null && company.getId() != null) {
            List<User> companyUsers = userRepository.findByCompanyId(company.getId());
            int assignedCount = 0;
            
            for (User user : companyUsers) {
                if ("USER".equalsIgnoreCase(user.getRole())) {
                    UserSystem us = new UserSystem();
                    us.setUserId(user.getId());
                    us.setSystemId(saved.getId());
                    userSystemRepository.save(us);
                    assignedCount++;
                }
            }
            
            System.out.println("✅ Auto-assigned system " + newSystemCode + 
                               " to " + assignedCount + " users in company " + company.getCompanyName());
        }
        
        System.out.println("✅ System created: " + newSystemCode + 
                           " with company: " + (company != null ? company.getCompanyName() + " (ID: " + company.getId() + ")" : "NULL"));
        
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/systems/{id}")
    public ResponseEntity<?> updateSystem(@PathVariable Long id,
                                          @RequestBody AlarmSystem systemDetails,
                                          @RequestParam(required = false) Long companyId,
                                          @RequestParam(required = false) String username) {
        Optional<AlarmSystem> opt = alarmSystemRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (username != null && !username.isEmpty()) {
            if (!permissionService.canManageSystem(username, id)) {
                return ResponseEntity.status(403).body("Access denied: You can only manage systems in your company");
            }
        }

        AlarmSystem system = opt.get();
        Long oldCompanyId = system.getCompany() != null ? system.getCompany().getId() : null;
        Long newCompanyId = oldCompanyId;
        
        if (systemDetails.getLocation() != null && !systemDetails.getLocation().trim().isEmpty()) {
            system.setLocation(systemDetails.getLocation().trim());
        }
        if (systemDetails.getDescription() != null) {
            system.setDescription(systemDetails.getDescription().trim());
        }
        if (systemDetails.getSimNumber() != null && !systemDetails.getSimNumber().trim().isEmpty()) {
            system.setSimNumber(systemDetails.getSimNumber().trim());
            system.setPanelSimNumber(systemDetails.getSimNumber().trim());
        }
        if (systemDetails.getPanelSimNumber() != null && !systemDetails.getPanelSimNumber().trim().isEmpty()) {
            system.setPanelSimNumber(systemDetails.getPanelSimNumber().trim());
        }
        if (systemDetails.getDisarmCommand() != null) {
            system.setDisarmCommand(systemDetails.getDisarmCommand());
        }
        if (systemDetails.getArmCommand() != null) {
            system.setArmCommand(systemDetails.getArmCommand());
        }
        if (systemDetails.getStatus() != null) {
            system.setStatus(systemDetails.getStatus());
        }

        if (username == null || !username.isEmpty() && permissionService.isAdmin(username)) {
            Long targetCompanyId = companyId;
            if (targetCompanyId == null && systemDetails.getCompany() != null) {
                targetCompanyId = systemDetails.getCompany().getId();
            }
            if (targetCompanyId != null) {
                if (targetCompanyId > 0) {
                    Optional<Company> companyOpt = companyRepository.findById(targetCompanyId);
                    if (companyOpt.isPresent()) {
                        system.setCompany(companyOpt.get());
                        newCompanyId = targetCompanyId;
                    }
                } else {
                    system.setCompany(null);
                    newCompanyId = null;
                }
            }
        }

        AlarmSystem saved = alarmSystemRepository.save(system);
        
        // If company changed, update user-system mappings
        if (!java.util.Objects.equals(newCompanyId, oldCompanyId)) {
            System.out.println("🔄 System " + saved.getSystemCode() + " company changed from " + 
                               oldCompanyId + " to " + newCompanyId);
            
            if (oldCompanyId != null) {
                List<User> oldCompanyUsers = userRepository.findByCompanyId(oldCompanyId);
                for (User user : oldCompanyUsers) {
                    if ("USER".equalsIgnoreCase(user.getRole())) {
                        List<UserSystem> existing = userSystemRepository.findAllByUserId(user.getId());
                        existing.stream()
                            .filter(us -> us.getSystemId().equals(saved.getId()))
                            .findFirst()
                            .ifPresent(us -> {
                                userSystemRepository.delete(us);
                                System.out.println("✅ Removed system " + saved.getSystemCode() + 
                                                   " from user " + user.getUsername());
                            });
                    }
                }
            }
            
            if (newCompanyId != null) {
                List<User> newCompanyUsers = userRepository.findByCompanyId(newCompanyId);
                for (User user : newCompanyUsers) {
                    if ("USER".equalsIgnoreCase(user.getRole())) {
                        List<UserSystem> existing = userSystemRepository.findAllByUserId(user.getId());
                        boolean alreadyAssigned = existing.stream()
                            .anyMatch(us -> us.getSystemId().equals(saved.getId()));
                        
                        if (!alreadyAssigned) {
                            UserSystem us = new UserSystem();
                            us.setUserId(user.getId());
                            us.setSystemId(saved.getId());
                            userSystemRepository.save(us);
                            System.out.println("✅ Assigned system " + saved.getSystemCode() + 
                                               " to user " + user.getUsername());
                        }
                    }
                }
            }
        }
        
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/systems/{id}/status")
    public ResponseEntity<?> toggleSystemStatus(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                @RequestParam(required = false) String username) {
        Optional<AlarmSystem> opt = alarmSystemRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        if (username != null && !username.isEmpty()) {
            if (!permissionService.canManageSystem(username, id)) {
                return ResponseEntity.status(403).body("Access denied");
            }
        }
        
        AlarmSystem system = opt.get();
        String newStatus = body.get("status");
        if (newStatus != null) {
            system.setStatus(newStatus);
            system.setLastStatusChangedAt(LocalDateTime.now());
            alarmSystemRepository.save(system);
        }
        return ResponseEntity.ok(system);
    }

    @DeleteMapping("/systems/{id}")
    public ResponseEntity<?> deleteSystem(@PathVariable Long id,
                                        @RequestParam(required = false) String username,
                                        @RequestParam(required = false) String permanent) {
        try {
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(id);
            if (systemOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            AlarmSystem system = systemOpt.get();

            if (Boolean.TRUE.equals(system.getDeleted())) {
                if ("true".equalsIgnoreCase(permanent)) {
                    alarmZoneRepository.deleteBySystemId(id);
                    alertLogRepository.deleteByAlarmSystemId(id);
                    alarmSystemRepository.deleteById(id);
                    return ResponseEntity.ok("System permanently deleted");
                }
                return ResponseEntity.badRequest().body("System is already deleted");
            }

            if (username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, id)) {
                    return ResponseEntity.status(403).body("Access denied: You can only delete systems in your company");
                }
            }

            alarmZoneRepository.deleteBySystemId(id);
            alertLogRepository.deleteByAlarmSystemId(id);

            system.setDeleted(true);
            system.setDeletedAt(LocalDateTime.now());
            system.setDeletedBy(username != null ? username : "SYSTEM");
            system.setStatus("DELETED");
            alarmSystemRepository.save(system);

            return ResponseEntity.ok("System deleted successfully. Data archived.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting system: " + e.getMessage());
        }
    }

    private void createDefaultZones(AlarmSystem system) {
        String[] wirelessZoneNames = {
            "Main Entrance", "Cash Counter", "Lobby", "Server Room",
            "Back Office", "Vault Room", "Emergency Exit", "Parking Area",
            "Store Room", "Rest Room", "Corridor 1", "Corridor 2",
            "Main Hall", "Conference Room", "Security Room", "Generator Room"
        };

        String[] wiredZoneNames = {
            "Wired Zone 1", "Wired Zone 2", "Wired Zone 3", "Wired Zone 4",
            "Wired Zone 5", "Wired Zone 6", "Wired Zone 7", "Wired Zone 8"
        };

        for (int i = 0; i < wirelessZoneNames.length; i++) {
            AlarmZone zone = new AlarmZone();
            zone.setAlarmSystem(system);
            zone.setZoneNumber(i + 1);
            zone.setZoneName(wirelessZoneNames[i]);
            zone.setZoneType(1);
            zone.setIsActive(true);
            zone.setZoneCategory("WIRELESS");
            zone.setDescription("Wireless zone " + (i + 1));
            alarmZoneRepository.save(zone);
        }

        for (int i = 0; i < wiredZoneNames.length; i++) {
            AlarmZone zone = new AlarmZone();
            zone.setAlarmSystem(system);
            zone.setZoneNumber(i + 17);
            zone.setZoneName(wiredZoneNames[i]);
            zone.setZoneType(1);
            zone.setIsActive(true);
            zone.setZoneCategory("WIRED");
            zone.setDescription("Wired zone " + (i + 1));
            alarmZoneRepository.save(zone);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // ============================================================
    // DELETED SYSTEMS - ADMIN ONLY
    // ============================================================
    
    @GetMapping("/systems/deleted")
    public ResponseEntity<?> getDeletedSystems(@RequestParam(required = false) String username) {
        try {
            if (username == null || username.isEmpty() || !permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied: Only Admin can view deleted systems");
            }
            
            List<AlarmSystem> deletedSystems = alarmSystemRepository.findAllDeleted();
            return ResponseEntity.ok(deletedSystems);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching deleted systems: " + e.getMessage());
        }
    }

    @DeleteMapping("/systems/{id}/permanent")
    public ResponseEntity<?> permanentDeleteSystem(@PathVariable Long id,
                                                   @RequestParam(required = false) String username) {
        try {
            if (username == null || username.isEmpty() || !permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied: Only Admin can permanently delete systems");
            }
            
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(id);
            if (systemOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            AlarmSystem system = systemOpt.get();
            
            if (!Boolean.TRUE.equals(system.getDeleted())) {
                return ResponseEntity.badRequest().body("System is not marked as deleted. Use soft delete first.");
            }
            
            alarmZoneRepository.deleteBySystemId(id);
            alertLogRepository.deleteByAlarmSystemId(id);
            userSystemRepository.deleteBySystemId(id);
            alarmSystemRepository.deleteById(id);
            
            System.out.println("✅ System " + system.getSystemCode() + " permanently deleted by " + username);
            
            return ResponseEntity.ok("System permanently deleted successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error permanently deleting system: " + e.getMessage());
        }
    }

    @GetMapping("/systems/deleted/company/{companyId}")
    public ResponseEntity<?> getDeletedSystemsByCompany(@PathVariable Long companyId,
                                                        @RequestParam(required = false) String username) {
        try {
            if (username == null || username.isEmpty() || !permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<AlarmSystem> deletedSystems = alarmSystemRepository.findDeletedByCompanyId(companyId);
            return ResponseEntity.ok(deletedSystems);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // ADMIN MANAGEMENT ENDPOINTS (UPDATED)
    // ============================================================

    /**
     * Get admin management info for current user
     * Returns what permissions the current user has
     */
    @GetMapping("/admin-permissions")
    public ResponseEntity<?> getAdminPermissions(@RequestParam String username) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Check if user is admin
            boolean isAdmin = permissionService.isAdmin(username);
            response.put("isAdmin", isAdmin);
            
            if (!isAdmin) {
                response.put("canManageAdmins", false);
                response.put("canCreateAdmin", false);
                response.put("canDeleteAdmin", false);
                response.put("canToggleAdminStatus", false);
                response.put("canResetAdminPassword", false);
                response.put("isSuperAdmin", false);
                response.put("registrationMethod", "USER");
                response.put("adminType", "USER");
                response.put("admins", new ArrayList<>());
                response.put("formAdminCount", 0);
                return ResponseEntity.ok(response);
            }
            
            // Get admin details
            String registrationMethod = permissionService.getRegistrationMethod(username);
            boolean isFormAdmin = "FORM".equals(registrationMethod);
            
            response.put("registrationMethod", registrationMethod);
            response.put("isSuperAdmin", isFormAdmin);
            response.put("adminType", isFormAdmin ? "SUPER_ADMIN" : "OPERATIONAL_ADMIN");
            
            // Permissions
            response.put("canCreateAdmin", isFormAdmin);
            response.put("canManageAdmins", isFormAdmin);
            response.put("canDeleteAdmin", isFormAdmin);
            response.put("canToggleAdminStatus", isFormAdmin);
            response.put("canResetAdminPassword", true); // Can always reset own password
            
            // Get all admins with their registration info
            List<User> allUsers = userRepository.findAll();
            List<User> admins = allUsers.stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> adminList = new ArrayList<>();
            long formAdminCount = 0;
            
            for (User admin : admins) {
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("id", admin.getId());
                adminInfo.put("username", admin.getUsername());
                adminInfo.put("isActive", admin.getIsActive() != null ? admin.getIsActive() : true);
                
                // Get registration method from User entity (faster)
                String regMethod = admin.getRegistrationMethod();
                if (regMethod == null || regMethod.isEmpty()) {
                    // Fallback: check via PermissionService
                    regMethod = permissionService.getRegistrationMethod(admin.getUsername());
                }
                
                adminInfo.put("registrationMethod", regMethod);
                
                boolean isSuperAdmin = "FORM".equals(regMethod) && "ADMIN".equalsIgnoreCase(admin.getRole());
                adminInfo.put("isSuperAdmin", isSuperAdmin);
                
                if (isSuperAdmin) {
                    formAdminCount++;
                }
                
                // Check permissions for this admin
                adminInfo.put("canBeManaged", permissionService.canManageAdmin(username, admin.getUsername()));
                adminInfo.put("canBeDeleted", permissionService.canDeleteAdmin(username, admin.getUsername()));
                adminInfo.put("canBeToggled", permissionService.canToggleAdminStatus(username, admin.getUsername()));
                adminInfo.put("canBeReset", permissionService.canResetAdminPassword(username, admin.getUsername()));
                adminInfo.put("isLastSuperAdmin", isSuperAdmin && formAdminCount <= 1);
                adminInfo.put("isSelf", admin.getUsername().equals(username));
                
                adminList.add(adminInfo);
            }
            response.put("admins", adminList);
            response.put("formAdminCount", formAdminCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Reset admin password - with permission check
     */
    @PutMapping("/admins/{id}/reset-password")
    public ResponseEntity<?> resetAdminPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestParam String currentUsername) {
        
        try {
            Optional<User> targetOpt = userRepository.findById(id);
            if (targetOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User target = targetOpt.get();
            
            // Only admins can reset passwords
            if (!"ADMIN".equalsIgnoreCase(target.getRole())) {
                return ResponseEntity.badRequest().body("Target is not an admin");
            }
            
            // Check permission
            if (!permissionService.canResetAdminPassword(currentUsername, target.getUsername())) {
                return ResponseEntity.status(403).body("Access denied: You cannot manage this admin");
            }
            
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body("Password must be at least 6 characters");
            }
            
            target.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(target);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully for " + target.getUsername());
            response.put("username", target.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Toggle admin active/inactive status - with permission check
     */
    @PatchMapping("/admins/{id}/toggle-status")
    public ResponseEntity<?> toggleAdminStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestParam String currentUsername) {
        
        try {
            Optional<User> targetOpt = userRepository.findById(id);
            if (targetOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User target = targetOpt.get();
            
            if (!"ADMIN".equalsIgnoreCase(target.getRole())) {
                return ResponseEntity.badRequest().body("Target is not an admin");
            }
            
            // Check permission
            if (!permissionService.canToggleAdminStatus(currentUsername, target.getUsername())) {
                return ResponseEntity.status(403).body("Access denied: You cannot manage this admin");
            }
            
            // Check if trying to deactivate self
            if (currentUsername.equals(target.getUsername())) {
                return ResponseEntity.badRequest().body("You cannot deactivate yourself");
            }
            
            boolean isCurrentlyActive = target.getIsActive() != null ? target.getIsActive() : true;
            boolean newStatus = !isCurrentlyActive;
            
            // ============================================================
            // DEACTIVATE - Save details
            // ============================================================
            if (!newStatus) {
                // Check if trying to deactivate last FORM Admin
                if (permissionService.isFormAdmin(target.getUsername())) {
                    long formAdminCount = permissionService.countFormAdmins();
                    if (formAdminCount <= 1) {
                        return ResponseEntity.badRequest().body(
                            "Cannot deactivate the last Super Admin. System needs at least one active Super Admin."
                        );
                    }
                }
                
                // Get reason and description from request
                String reason = request.get("reason");
                String description = request.get("description");
                
                if (reason == null || reason.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Reason is required to deactivate an admin");
                }
                
                // Set deactivation details
                target.setInactivatedAt(LocalDateTime.now());
                target.setInactivatedBy(currentUsername);
                target.setInactivationReason(reason.trim());
                target.setInactivationDescription(description != null ? description.trim() : null);
            } 
            // ============================================================
            // REACTIVATE - Clear deactivation details
            // ============================================================
            else {
                target.setReactivatedAt(LocalDateTime.now());
                target.setReactivatedBy(currentUsername);
                // Keep history but clear current inactive status
                // We keep inactivated_at etc for audit trail
            }
            
            target.setIsActive(newStatus);
            userRepository.save(target);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", target.getUsername() + " is now " + (newStatus ? "ACTIVE" : "INACTIVE"));
            response.put("username", target.getUsername());
            response.put("isActive", newStatus);
            
            // Return deactivation details if inactive
            if (!newStatus) {
                response.put("inactivatedAt", target.getInactivatedAt());
                response.put("inactivatedBy", target.getInactivatedBy());
                response.put("inactivationReason", target.getInactivationReason());
                response.put("inactivationDescription", target.getInactivationDescription());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete admin - with permission check
     */
    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(
            @PathVariable Long id,
            @RequestParam String currentUsername) {
        
        try {
            Optional<User> targetOpt = userRepository.findById(id);
            if (targetOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User target = targetOpt.get();
            
            // Only admins can be deleted
            if (!"ADMIN".equalsIgnoreCase(target.getRole())) {
                return ResponseEntity.badRequest().body("Target is not an admin");
            }
            
            // Check permission
            if (!permissionService.canDeleteAdmin(currentUsername, target.getUsername())) {
                return ResponseEntity.status(403).body("Access denied: You cannot delete this admin");
            }
            
            // Check if trying to delete self
            if (currentUsername.equals(target.getUsername())) {
                return ResponseEntity.badRequest().body("You cannot delete yourself");
            }
            
            // Check if trying to delete last FORM Admin
            if (permissionService.isFormAdmin(target.getUsername())) {
                long formAdminCount = permissionService.countFormAdmins();
                if (formAdminCount <= 1) {
                    return ResponseEntity.badRequest().body(
                        "Cannot delete the last Super Admin. System needs at least one Super Admin."
                    );
                }
            }
            
            // Delete user
            userRepository.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin " + target.getUsername() + " deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get admin details for editing
     */
    @GetMapping("/admins/{id}")
    public ResponseEntity<?> getAdminDetails(
            @PathVariable Long id,
            @RequestParam String currentUsername) {
        
        try {
            Optional<User> targetOpt = userRepository.findById(id);
            if (targetOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User target = targetOpt.get();
            
            if (!"ADMIN".equalsIgnoreCase(target.getRole())) {
                return ResponseEntity.badRequest().body("Target is not an admin");
            }
            
            // Check if current user can manage this admin
            if (!permissionService.canManageAdmin(currentUsername, target.getUsername())) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", target.getId());
            response.put("username", target.getUsername());
            response.put("isActive", target.getIsActive() != null ? target.getIsActive() : true);
            response.put("registrationMethod", permissionService.getRegistrationMethod(target.getUsername()));
            response.put("isSuperAdmin", "FORM".equals(permissionService.getRegistrationMethod(target.getUsername())));
            response.put("canManage", permissionService.canManageAdmin(currentUsername, target.getUsername()));
            response.put("canDelete", permissionService.canDeleteAdmin(currentUsername, target.getUsername()));
            response.put("canToggle", permissionService.canToggleAdminStatus(currentUsername, target.getUsername()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // Get Inactive User Details
    // ============================================================

    @GetMapping("/users/{username}/inactive-details")
    public ResponseEntity<?> getInactiveUserDetails(@PathVariable String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("isActive", user.getIsActive());
            response.put("inactivatedAt", user.getInactivatedAt());
            response.put("inactivatedBy", user.getInactivatedBy());
            response.put("inactivationReason", user.getInactivationReason());
            response.put("inactivationDescription", user.getInactivationDescription());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // TOGGLE USER STATUS - With Reason
    // ============================================================

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestParam String currentUsername) {
        
        try {
            Optional<User> targetOpt = userRepository.findById(id);
            if (targetOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User target = targetOpt.get();
            
            // Check if current user is admin
            if (!permissionService.isAdmin(currentUsername)) {
                return ResponseEntity.status(403).body("Access denied: Only admins can toggle user status");
            }
            
            // Check if target is USER
            if (!"USER".equalsIgnoreCase(target.getRole())) {
                return ResponseEntity.badRequest().body("Target is not a user");
            }
            
            // Check if trying to deactivate self
            if (currentUsername.equals(target.getUsername())) {
                return ResponseEntity.badRequest().body("You cannot deactivate yourself");
            }
            
            boolean isCurrentlyActive = target.getIsActive() != null ? target.getIsActive() : true;
            boolean newStatus = !isCurrentlyActive;
            
            // ============================================================
            // DEACTIVATE - Save details
            // ============================================================
            if (!newStatus) {
                // Get reason and description from request
                String reason = request.get("reason");
                String description = request.get("description");
                
                if (reason == null || reason.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Reason is required to deactivate a user");
                }
                
                // Set deactivation details
                target.setInactivatedAt(LocalDateTime.now());
                target.setInactivatedBy(currentUsername);
                target.setInactivationReason(reason.trim());
                target.setInactivationDescription(description != null ? description.trim() : null);
            } 
            // ============================================================
            // REACTIVATE - Clear deactivation details
            // ============================================================
            else {
                target.setReactivatedAt(LocalDateTime.now());
                target.setReactivatedBy(currentUsername);
                // Keep history but clear current inactive status
            }
            
            target.setIsActive(newStatus);
            userRepository.save(target);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", target.getUsername() + " is now " + (newStatus ? "ACTIVE" : "INACTIVE"));
            response.put("username", target.getUsername());
            response.put("isActive", newStatus);
            response.put("role", target.getRole());
            
            // Return deactivation details if inactive
            if (!newStatus) {
                response.put("inactivatedAt", target.getInactivatedAt());
                response.put("inactivatedBy", target.getInactivatedBy());
                response.put("inactivationReason", target.getInactivationReason());
                response.put("inactivationDescription", target.getInactivationDescription());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}