package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
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
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AuthController {

    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, 
                          UserSystemRepository userSystemRepository, 
                          AlarmSystemRepository alarmSystemRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== HANDLE OPTIONS REQUEST =====
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Max-Age", "3600")
            .build();
    }

    // ===== NEW: CHECK IF ADMIN EXISTS =====
    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Boolean>> checkAdmin() {
        boolean hasAdmin = userRepository.existsByRole("ADMIN");
        Map<String, Boolean> response = new HashMap<>();
        response.put("hasAdmin", hasAdmin);
        return ResponseEntity.ok(response);
    }

    // ========== LOGIN ENDPOINT ==========
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = userOpt.get();
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        List<String> assignedSystems = List.of();

        if ("USER".equalsIgnoreCase(user.getRole())) {
            List<UserSystem> mappings = userSystemRepository.findAllByUserId(user.getId());
            assignedSystems = mappings.stream()
                .map(m -> alarmSystemRepository.findById(m.getSystemId()))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getSystemCode())
                .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("assignedSystems", assignedSystems);

        return ResponseEntity.ok(response);
    }

    // ========== REGISTER ENDPOINT - UPDATED ==========
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registrationData) {
        String username = registrationData.get("username");
        String password = registrationData.get("password");
        String confirmPassword = registrationData.get("confirmPassword");
        String role = registrationData.get("role");

        // Validation
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Password is required");
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Please confirm your password");
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }
        if (role == null || role.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Role is required");
        }
        if (!role.equals("ADMIN") && !role.equals("USER")) {
            return ResponseEntity.badRequest().body("Invalid role. Must be ADMIN or USER");
        }

        // ===== CHECK: Admin already exists? =====
        boolean hasAdmin = userRepository.existsByRole("ADMIN");

        // If trying to register as ADMIN but admin already exists
        if ("ADMIN".equalsIgnoreCase(role) && hasAdmin) {
            return ResponseEntity.badRequest().body("Admin account already exists. Please login.");
        }

        // If trying to register as USER but no admin exists (first user must be admin)
        if ("USER".equalsIgnoreCase(role) && !hasAdmin) {
            return ResponseEntity.badRequest().body("First account must be ADMIN. Please register as Admin.");
        }

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        // Create new user with hashed password
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role.toUpperCase());

        User savedUser = userRepository.save(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("role", savedUser.getRole());
        response.put("message", "User registered successfully");

        return ResponseEntity.ok(response);
    }
}