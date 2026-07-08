package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           UserSystemRepository userSystemRepository,
                           AlarmSystemRepository alarmSystemRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== USER MANAGEMENT ==========

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            
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
    public ResponseEntity<?> createUser(@RequestBody User newUser) {
        if (newUser.getUsername() == null || newUser.getPassword() == null || newUser.getRole() == null) {
            return ResponseEntity.badRequest().body("Username, password and role are required");
        }
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        User saved = userRepository.save(newUser);
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

    // ========== RESET USER PASSWORD ==========
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

    // ========== ALARM SYSTEM MANAGEMENT ==========

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
        } catch (NumberFormatException e) {
            // If parsing fails, start from 01
        }
        return "ALARM-Z8B-01";
    }

    @GetMapping("/systems")
    public ResponseEntity<List<AlarmSystem>> getSystems() {
        return ResponseEntity.ok(alarmSystemRepository.findAll());
    }

    @PostMapping("/systems")
    public ResponseEntity<?> createSystem(@RequestBody AlarmSystem system) {
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
        newSystem.setSimNumber(system.getSimNumber().trim());
        newSystem.setStatus(system.getStatus() != null ? system.getStatus() : "ACTIVE");
        newSystem.setLastStatusChangedAt(LocalDateTime.now());

        AlarmSystem saved = alarmSystemRepository.save(newSystem);
        return ResponseEntity.ok(saved);
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

        if (updated.getSimNumber() != null && !updated.getSimNumber().trim().isEmpty()) {
            String newSim = updated.getSimNumber().trim();
            Optional<AlarmSystem> simCheck = alarmSystemRepository.findBySimNumber(newSim);
            if (simCheck.isPresent() && !simCheck.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("SIM number already registered to another system");
            }
            existing.setSimNumber(newSim);
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
        
        alarmSystemRepository.deleteById(id);
        return ResponseEntity.ok("System deleted successfully");
    }
}