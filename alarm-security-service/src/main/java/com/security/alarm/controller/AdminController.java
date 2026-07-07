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

    // 1. Get all users
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            
            // Fetch assigned system codes
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

    // 2. Create user
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User newUser) {
        if (newUser.getUsername() == null || newUser.getPassword() == null || newUser.getRole() == null) {
            return ResponseEntity.badRequest().body("Username, password and role are required");
        }
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        // Hash the password before saving
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        User saved = userRepository.save(newUser);
        return ResponseEntity.ok(saved);
    }

    // 3. Get all alarm systems
    @GetMapping("/systems")
    public ResponseEntity<List<AlarmSystem>> getSystems() {
        return ResponseEntity.ok(alarmSystemRepository.findAll());
    }

    // 4. Assign systems to a user
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

        // Delete old assignments
        userSystemRepository.deleteByUserId(userId);

        // Add new assignments
        List<UserSystem> newAssignments = systemIds.stream().map(sysId -> {
            UserSystem mapping = new UserSystem();
            mapping.setUserId(userId);
            mapping.setSystemId(sysId);
            return mapping;
        }).collect(Collectors.toList());

        userSystemRepository.saveAll(newAssignments);

        return ResponseEntity.ok("Systems assigned successfully");
    }

    // 5. Create Alarm System
    @PostMapping("/systems")
    public ResponseEntity<?> createSystem(@RequestBody AlarmSystem system) {
        if (system.getSystemCode() == null || system.getLocation() == null || system.getSimNumber() == null) {
            return ResponseEntity.badRequest().body("System code, location and sim number are required");
        }
        if (alarmSystemRepository.findBySimNumber(system.getSimNumber()).isPresent()) {
            return ResponseEntity.badRequest().body("Sim number already registered");
        }
        system.setLastStatusChangedAt(java.time.LocalDateTime.now());
        AlarmSystem saved = alarmSystemRepository.save(system);
        return ResponseEntity.ok(saved);
    }

    // 6. Update Alarm System
    @PutMapping("/systems/{id}")
    public ResponseEntity<?> updateSystem(@PathVariable Long id, @RequestBody AlarmSystem updated) {
        Optional<AlarmSystem> existingOpt = alarmSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AlarmSystem existing = existingOpt.get();
        existing.setLocation(updated.getLocation());
        existing.setSimNumber(updated.getSimNumber());

        if (updated.getStatus() != null && !updated.getStatus().equalsIgnoreCase(existing.getStatus())) {
            existing.setStatus(updated.getStatus());
            existing.setLastStatusChangedAt(java.time.LocalDateTime.now());
        }

        AlarmSystem saved = alarmSystemRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // 7. Toggle Alarm System Status
    @PatchMapping("/systems/{id}/status")
    public ResponseEntity<?> toggleSystemStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AlarmSystem> existingOpt = alarmSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String newStatus = payload.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body("Status parameter is required");
        }

        AlarmSystem existing = existingOpt.get();
        if (!newStatus.equalsIgnoreCase(existing.getStatus())) {
            existing.setStatus(newStatus);
            existing.setLastStatusChangedAt(java.time.LocalDateTime.now());
            alarmSystemRepository.save(existing);
        }

        return ResponseEntity.ok(existing);
    }

    // 8. Delete Alarm System
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
