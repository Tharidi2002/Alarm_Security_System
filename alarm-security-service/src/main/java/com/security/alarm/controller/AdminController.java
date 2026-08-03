package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.Company;
import com.security.alarm.entity.RegistrationAuditLog;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.repository.RegistrationAuditLogRepository;
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
import com.security.alarm.entity.AlarmZone;
import com.security.alarm.repository.AlarmZoneRepository;

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

    public AdminController(UserRepository userRepository,
                        UserSystemRepository userSystemRepository,
                        AlarmSystemRepository alarmSystemRepository,
                        CompanyRepository companyRepository,
                        PasswordEncoder passwordEncoder,
                        AlarmZoneRepository alarmZoneRepository,
                        RegistrationAuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.alarmZoneRepository = alarmZoneRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ========== USER MANAGEMENT ==========
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers(@RequestParam(required = false) Long companyId) {
        List<User> users;
        if (companyId != null && companyId > 0) {
            users = userRepository.findByCompanyId(companyId);
        } else {
            users = userRepository.findAll();
        }
        
        Optional<User> firstAdmin = userRepository.findFirstByRoleOrderByIdAsc("ADMIN");
        Long firstAdminId = firstAdmin.map(User::getId).orElse(null);
        
        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            map.put("isFirstAdmin", u.getId().equals(firstAdminId));
            
            // Company info
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
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User newUser, HttpServletRequest request) {
        if (newUser.getUsername() == null || newUser.getPassword() == null || newUser.getRole() == null) {
            return ResponseEntity.badRequest().body("Username, password and role are required");
        }
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        User saved = userRepository.save(newUser);
        
        // ===== AUDIT LOG =====
        String clientIp = getClientIp(request);
        RegistrationAuditLog log = new RegistrationAuditLog();
        log.setUsername(newUser.getUsername());
        log.setRole(newUser.getRole());
        log.setRegisteredBy("ADMIN:admin");
        log.setRegisteredFromIp(clientIp);
        log.setMethod("ADMIN_PANEL");
        log.setNotes("User created by admin");
        auditLogRepository.save(log);
        
        return ResponseEntity.ok(saved);
    }

    // ========== SYSTEM MANAGEMENT ==========
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
    public ResponseEntity<List<AlarmSystem>> getSystems(@RequestParam(required = false) Long companyId) {
        List<AlarmSystem> systems;
        if (companyId != null && companyId > 0) {
            systems = alarmSystemRepository.findByCompanyId(companyId);
        } else {
            systems = alarmSystemRepository.findAll();
        }
        return ResponseEntity.ok(systems);
    }

    @PostMapping("/systems")
    public ResponseEntity<?> createSystem(@RequestBody AlarmSystem system, @RequestParam(required = false) Long companyId) {
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
        
        // ===== Set company =====
        Long targetCompanyId = companyId;
        if (targetCompanyId == null && system.getCompany() != null && system.getCompany().getId() != null) {
            targetCompanyId = system.getCompany().getId();
        }
        if (targetCompanyId != null && targetCompanyId > 0) {
            companyRepository.findById(targetCompanyId).ifPresent(newSystem::setCompany);
        }
        
        newSystem.setPanelSimNumber(system.getSimNumber().trim());
        newSystem.setPanelPassword("8888");
        newSystem.setDisarmCommand("8888#2A");
        newSystem.setArmCommand("8888#1A");
        newSystem.setSirenStopCommand("8888#5A");
        newSystem.setSirenStatus("OFF");

        AlarmSystem saved = alarmSystemRepository.save(newSystem);
        createDefaultZones(saved);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/systems/{id}")
    public ResponseEntity<?> updateSystem(@PathVariable Long id, @RequestBody AlarmSystem systemDetails, @RequestParam(required = false) Long companyId) {
        Optional<AlarmSystem> opt = alarmSystemRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
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

        AlarmSystem saved = alarmSystemRepository.save(system);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/systems/{id}/status")
    public ResponseEntity<?> toggleSystemStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<AlarmSystem> opt = alarmSystemRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
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
    public ResponseEntity<?> deleteSystem(@PathVariable Long id) {
        if (!alarmSystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        alarmSystemRepository.deleteById(id);
        return ResponseEntity.ok("System deleted successfully");
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

    // ===== HELPER: Get Client IP =====
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}