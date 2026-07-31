package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.RegistrationAuditLog;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.AlarmSystemRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final AlarmZoneRepository alarmZoneRepository;
    private final RegistrationAuditLogRepository auditLogRepository;

    public AdminController(UserRepository userRepository,
                        UserSystemRepository userSystemRepository,
                        AlarmSystemRepository alarmSystemRepository,
                        PasswordEncoder passwordEncoder,
                        AlarmZoneRepository alarmZoneRepository,
                        RegistrationAuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.passwordEncoder = passwordEncoder;
        this.alarmZoneRepository = alarmZoneRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ========== USER MANAGEMENT ==========
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<User> users = userRepository.findAll();
        Optional<User> firstAdmin = userRepository.findFirstByRoleOrderByIdAsc("ADMIN");
        Long firstAdminId = firstAdmin.map(User::getId).orElse(null);
        
        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            map.put("isFirstAdmin", u.getId().equals(firstAdminId));
            
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
        
        // ===== AUDIT LOG: Admin created user via panel =====
        String clientIp = getClientIp(request);
        String adminUsername = getAdminUsername(); // Get from session/security context
        
        RegistrationAuditLog log = new RegistrationAuditLog();
        log.setUsername(newUser.getUsername());
        log.setRole(newUser.getRole());
        log.setRegisteredBy("ADMIN:" + adminUsername);
        log.setRegisteredFromIp(clientIp);
        log.setMethod("ADMIN_PANEL");
        log.setNotes("User created by admin: " + adminUsername);
        auditLogRepository.save(log);
        
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/users/{userId}/assign")
    public ResponseEntity<?> assignSystems(@PathVariable Long userId, @RequestBody Map<String, List<Long>> payload) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Long> systemIds = payload.get("systemIds");
        if (systemIds == null) {
            return ResponseEntity.badRequest().body("systemIds list is required");
        }

        userSystemRepository.deleteByUserId(userId);

        List<UserSystem> newAssignments = systemIds.stream().map(sysId -> {
            UserSystem mapping = new UserSystem();
            mapping.setUserId(userId);
            mapping.setSystemId(sysId);
            return mapping;
        }).collect(Collectors.toList());

        userSystemRepository.saveAll(newAssignments);
        return ResponseEntity.ok("Systems assigned successfully");
    }

    @PutMapping("/users/{userId}/reset-password")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload) {
        
        String newPassword = payload.get("newPassword");
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("New password is required");
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successfully for user: " + user.getUsername());
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }

    // ========== DELETE USER WITH PROTECTION ==========
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        // ===== PROTECT FIRST ADMIN =====
        Optional<User> firstAdmin = userRepository.findFirstByRoleOrderByIdAsc("ADMIN");
        if (firstAdmin.isPresent() && firstAdmin.get().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("Cannot delete the primary admin account. This is the first admin created in the system.");
        }
        
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            long adminCount = userRepository.countAdmins();
            if (adminCount <= 1) {
                return ResponseEntity.badRequest().body("Cannot delete the last admin account. At least one admin must exist.");
            }
        }
        
        try {
            userSystemRepository.deleteByUserId(userId);
            userRepository.deleteById(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete user: " + e.getMessage());
        }
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
    public ResponseEntity<List<AlarmSystem>> getSystems() {
        return ResponseEntity.ok(alarmSystemRepository.findAll());
    }

    @PostMapping("/systems")
    public ResponseEntity<?> createSystem(@RequestBody AlarmSystem system) {
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

    @GetMapping("/systems/{id}")
    public ResponseEntity<?> getSystemById(@PathVariable Long id) {
        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(id);
        if (systemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(systemOpt.get());
    }

    @PutMapping("/systems/{id}")
    public ResponseEntity<?> updateSystem(@PathVariable Long id, @RequestBody AlarmSystem updated) {
        Optional<AlarmSystem> existingOpt = alarmSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AlarmSystem existing = existingOpt.get();

        if (updated.getLocation() != null && !updated.getLocation().trim().isEmpty()) {
            existing.setLocation(updated.getLocation().trim());
        }

        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription().trim());
        }

        if (updated.getSimNumber() != null && !updated.getSimNumber().trim().isEmpty()) {
            String newSim = updated.getSimNumber().trim();
            Optional<AlarmSystem> simCheck = alarmSystemRepository.findBySimNumber(newSim);
            if (simCheck.isPresent() && !simCheck.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("SIM number already registered to another system");
            }
            existing.setSimNumber(newSim);
        }

        if (updated.getPanelSimNumber() != null && !updated.getPanelSimNumber().trim().isEmpty()) {
            existing.setPanelSimNumber(updated.getPanelSimNumber().trim());
        }
        if (updated.getPanelPassword() != null && !updated.getPanelPassword().trim().isEmpty()) {
            existing.setPanelPassword(updated.getPanelPassword().trim());
        }
        if (updated.getDisarmCommand() != null && !updated.getDisarmCommand().trim().isEmpty()) {
            existing.setDisarmCommand(updated.getDisarmCommand().trim());
        }
        if (updated.getArmCommand() != null && !updated.getArmCommand().trim().isEmpty()) {
            existing.setArmCommand(updated.getArmCommand().trim());
        }
        if (updated.getSirenStopCommand() != null && !updated.getSirenStopCommand().trim().isEmpty()) {
            existing.setSirenStopCommand(updated.getSirenStopCommand().trim());
        }

        if (updated.getStatus() != null && !updated.getStatus().equalsIgnoreCase(existing.getStatus())) {
            existing.setStatus(updated.getStatus());
            existing.setLastStatusChangedAt(LocalDateTime.now());
        }

        AlarmSystem saved = alarmSystemRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/systems/{id}/status")
    public ResponseEntity<?> toggleSystemStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AlarmSystem> existingOpt = alarmSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String newStatus = payload.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Status parameter is required");
        }

        if (!newStatus.equalsIgnoreCase("ACTIVE") && !newStatus.equalsIgnoreCase("INACTIVE")) {
            return ResponseEntity.badRequest().body("Status must be ACTIVE or INACTIVE");
        }

        AlarmSystem existing = existingOpt.get();
        if (!newStatus.equalsIgnoreCase(existing.getStatus())) {
            existing.setStatus(newStatus.toUpperCase());
            existing.setLastStatusChangedAt(LocalDateTime.now());
            alarmSystemRepository.save(existing);
        }

        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/systems/{id}")
    public ResponseEntity<?> deleteSystem(@PathVariable Long id) {
        Optional<AlarmSystem> existingOpt = alarmSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            alarmZoneRepository.deleteBySystemId(id);
            alarmSystemRepository.deleteById(id);
            return ResponseEntity.ok("System deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete system: " + e.getMessage());
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

    // ===== HELPER: Get Admin Username (from session/context) =====
    private String getAdminUsername() {
        // In a real implementation, get from SecurityContext
        // For now, return a placeholder
        return "admin";
    }
}