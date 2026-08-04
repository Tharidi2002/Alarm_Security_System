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
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
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
    private final RegistrationAuditLogRepository auditLogRepository;
    private final PermissionService permissionService;

    public AdminController(UserRepository userRepository,
                        UserSystemRepository userSystemRepository,
                        AlarmSystemRepository alarmSystemRepository,
                        CompanyRepository companyRepository,
                        PasswordEncoder passwordEncoder,
                        AlarmZoneRepository alarmZoneRepository,
                        RegistrationAuditLogRepository auditLogRepository,
                        PermissionService permissionService) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.alarmZoneRepository = alarmZoneRepository;
        this.auditLogRepository = auditLogRepository;
        this.permissionService = permissionService;
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

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");
        Long companyId = request.get("companyId") != null ? 
            Long.valueOf(request.get("companyId").toString()) : null;
        
        if (username == null || password == null || role == null) {
            return ResponseEntity.badRequest().body("Username, password and role are required");
        }
        
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        
        if ("USER".equalsIgnoreCase(role) && (companyId == null || companyId == 0)) {
            return ResponseEntity.badRequest().body("Company is required for USER role");
        }
        
        Company company = null;
        if (companyId != null && companyId > 0) {
            Optional<Company> companyOpt = companyRepository.findById(companyId);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Company not found");
            }
            company = companyOpt.get();
        }
        
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role.toUpperCase());
        newUser.setCompany(company);
        
        User saved = userRepository.save(newUser);
        
        String clientIp = getClientIp(httpRequest);
        RegistrationAuditLog log = new RegistrationAuditLog();
        log.setUsername(username);
        log.setRole(role);
        log.setRegisteredBy("ADMIN:" + (httpRequest.getParameter("adminUsername") != null ? 
            httpRequest.getParameter("adminUsername") : "admin"));
        log.setRegisteredFromIp(clientIp);
        log.setMethod("ADMIN_PANEL");
        log.setNotes("User created by admin with company: " + (company != null ? company.getCompanyName() : "None"));
        auditLogRepository.save(log);
        
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

    @GetMapping("/systems")
    public ResponseEntity<?> getSystems(@RequestParam(required = false) Long companyId,
                                        @RequestParam(required = false) String username) {
        System.out.println("DEBUG: getSystems called with username: " + username + ", companyId: " + companyId);
        
        // If USER, return only their company systems
        if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
            Long userCompanyId = permissionService.getUserCompanyId(username);
            System.out.println("DEBUG: User company ID: " + userCompanyId);
            if (userCompanyId == null) {
                return ResponseEntity.badRequest().body("User has no company assigned");
            }
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(userCompanyId);
            System.out.println("DEBUG: Found " + systems.size() + " systems for user's company");
            return ResponseEntity.ok(systems);
        }
        
        // Admin - all systems or filter by company
        List<AlarmSystem> systems;
        if (companyId != null && companyId > 0) {
            systems = alarmSystemRepository.findByCompanyId(companyId);
        } else {
            systems = alarmSystemRepository.findAll();
        }
        System.out.println("DEBUG: Found " + systems.size() + " systems total");
        return ResponseEntity.ok(systems);
    }

    @PostMapping("/systems")
    public ResponseEntity<?> createSystem(@RequestBody AlarmSystem system,
                                          @RequestParam(required = false) Long companyId,
                                          @RequestParam(required = false) String username) {
        // Check system limit
        if (alarmSystemRepository.count() >= 5) {
            return ResponseEntity.badRequest().body("System registration limit reached. A maximum of 5 systems can be registered.");
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

        // ============================================================
        // DETERMINE COMPANY ID
        // ============================================================
        Long targetCompanyId = null;
        
        // 1. If companyId is passed as parameter, use it
        if (companyId != null && companyId > 0) {
            targetCompanyId = companyId;
        }
        
        // 2. If username is provided, check user's role
        if (username != null && !username.isEmpty()) {
            User user = permissionService.getUser(username);
            if (user != null) {
                // If user is USER role, force their company
                if ("USER".equalsIgnoreCase(user.getRole())) {
                    if (user.getCompany() == null) {
                        return ResponseEntity.badRequest().body("User has no company assigned. Cannot create system.");
                    }
                    targetCompanyId = user.getCompany().getId();
                    System.out.println("DEBUG: USER role - forcing company ID: " + targetCompanyId);
                } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    // Admin can override with companyId from request body
                    if (system.getCompany() != null && system.getCompany().getId() != null) {
                        targetCompanyId = system.getCompany().getId();
                    }
                    System.out.println("DEBUG: ADMIN role - using company ID: " + targetCompanyId);
                }
            }
        }
        
        // 3. If still null, try from system object
        if (targetCompanyId == null && system.getCompany() != null && system.getCompany().getId() != null) {
            targetCompanyId = system.getCompany().getId();
        }

        // ============================================================
        // GET COMPANY ENTITY
        // ============================================================
        Company company = null;
        if (targetCompanyId != null && targetCompanyId > 0) {
            Optional<Company> companyOpt = companyRepository.findById(targetCompanyId);
            if (companyOpt.isPresent()) {
                company = companyOpt.get();
                System.out.println("DEBUG: Company found: " + company.getCompanyName() + " (ID: " + company.getId() + ")");
            } else {
                return ResponseEntity.badRequest().body("Company not found with ID: " + targetCompanyId);
            }
        } else {
            System.out.println("DEBUG: No company assigned to this system");
        }

        // Generate system code
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

        // ============================================================
        // CREATE SYSTEM
        // ============================================================
        AlarmSystem newSystem = new AlarmSystem();
        newSystem.setSystemCode(newSystemCode);
        newSystem.setLocation(system.getLocation().trim());
        if (system.getDescription() != null) newSystem.setDescription(system.getDescription().trim());
        newSystem.setSimNumber(system.getSimNumber().trim());
        newSystem.setStatus(system.getStatus() != null ? system.getStatus() : "ACTIVE");
        newSystem.setLastStatusChangedAt(LocalDateTime.now());
        
        // CRITICAL: Set the company
        newSystem.setCompany(company);
        
        newSystem.setPanelSimNumber(system.getSimNumber().trim());
        newSystem.setPanelPassword("8888");
        newSystem.setDisarmCommand("8888#2A");
        newSystem.setArmCommand("8888#1A");
        newSystem.setSirenStopCommand("8888#5A");
        newSystem.setSirenStatus("OFF");

        AlarmSystem saved = alarmSystemRepository.save(newSystem);
        createDefaultZones(saved);
        
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

        // Only Admin can change company
        if (username == null || !username.isEmpty() && permissionService.isAdmin(username)) {
            Long targetCompanyId = companyId;
            if (targetCompanyId == null && systemDetails.getCompany() != null) {
                targetCompanyId = systemDetails.getCompany().getId();
            }
            if (targetCompanyId != null) {
                if (targetCompanyId > 0) {
                    companyRepository.findById(targetCompanyId).ifPresent(system::setCompany);
                } else {
                    system.setCompany(null);
                }
            }
        }

        AlarmSystem saved = alarmSystemRepository.save(system);
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
                                        @RequestParam(required = false) String username) {
        try {
            // Check if system exists
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(id);
            if (systemOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("DEBUG: deleteSystem called - System ID: " + id + ", Username: " + username);
            
            // Permission check
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, id)) {
                    System.out.println("DEBUG: deleteSystem - Access denied for user: " + username);
                    return ResponseEntity.status(403).body("Access denied: You can only delete systems in your company");
                }
            }
            
            // Delete zones first (foreign key constraint)
            alarmZoneRepository.deleteBySystemId(id);
            
            // Delete the system
            alarmSystemRepository.deleteById(id);
            
            System.out.println("DEBUG: deleteSystem - System " + id + " deleted successfully");
            return ResponseEntity.ok("System deleted successfully");
            
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

    // ============================================================
    // HELPER: Get Client IP
    // ============================================================
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}