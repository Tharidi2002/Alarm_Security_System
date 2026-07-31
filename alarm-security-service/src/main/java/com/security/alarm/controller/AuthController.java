package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.SystemConfig;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.SystemConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AuthController {

    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final PasswordEncoder passwordEncoder;

    // Rate limiting
    private final Map<String, AttemptInfo> attemptTracker = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;
    private static final String SECRET_CODE_KEY = "MASTER_SECRET_CODE";

    public AuthController(UserRepository userRepository,
                          UserSystemRepository userSystemRepository,
                          AlarmSystemRepository alarmSystemRepository,
                          SystemConfigRepository systemConfigRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.systemConfigRepository = systemConfigRepository;
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

    // ===== CHECK ADMIN STATUS =====
    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Object>> checkAdmin(HttpServletRequest request) {
        boolean hasAdmin = userRepository.existsByRole("ADMIN");
        boolean hasSuperAdmin = userRepository.existsByRole("SUPER_ADMIN");
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasAdmin", hasAdmin);
        response.put("hasSuperAdmin", hasSuperAdmin);
        
        // Check if unlock session exists
        HttpSession session = request.getSession(false);
        boolean isUnlocked = session != null && session.getAttribute("REGISTER_UNLOCKED") != null;
        response.put("isUnlocked", isUnlocked);
        
        return ResponseEntity.ok(response);
    }

    // ===== VERIFY SECRET CODE =====
    @PostMapping("/verify-secret")
    public ResponseEntity<?> verifySecret(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String providedCode = request.get("secretCode");
        String clientIp = getClientIp(httpRequest);

        // Check if admin exists
        boolean hasAdmin = userRepository.existsByRole("ADMIN");
        if (!hasAdmin) {
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "No admin exists. You can register directly."
            ));
        }

        // Rate limiting check
        AttemptInfo attemptInfo = attemptTracker.get(clientIp);
        if (attemptInfo != null && attemptInfo.isLockedOut()) {
            long remainingMinutes = attemptInfo.getRemainingLockoutMinutes();
            return ResponseEntity.status(429).body(Map.of(
                "valid", false,
                "message", "Too many attempts. Try again in " + remainingMinutes + " minutes.",
                "remainingAttempts", 0,
                "lockedOut", true,
                "remainingMinutes", remainingMinutes
            ));
        }

        // Get stored code from DB
        Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(SECRET_CODE_KEY);
        if (configOpt.isEmpty()) {
            return ResponseEntity.status(500).body(Map.of(
                "valid", false,
                "message", "System configuration error. Contact administrator."
            ));
        }

        String storedCode = configOpt.get().getConfigValue();

        // Check if codes match
        if (providedCode != null && providedCode.equals(storedCode)) {
            // Success - clear attempts
            attemptTracker.remove(clientIp);

            // Create unlock session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("REGISTER_UNLOCKED", true);
            session.setMaxInactiveInterval(900); // 15 minutes

            return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "Code verified. Registration unlocked for 15 minutes."
            ));
        } else {
            // Failed attempt
            AttemptInfo info = attemptTracker.getOrDefault(clientIp, new AttemptInfo());
            info.incrementAttempts();
            attemptTracker.put(clientIp, info);

            int remaining = info.getRemainingAttempts();
            boolean lockedOut = info.isLockedOut();

            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Invalid code");
            response.put("remainingAttempts", remaining);
            response.put("lockedOut", lockedOut);

            if (lockedOut) {
                response.put("remainingMinutes", info.getRemainingLockoutMinutes());
                response.put("message", "Too many attempts. Try again in " + info.getRemainingLockoutMinutes() + " minutes.");
            }

            return ResponseEntity.status(401).body(response);
        }
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
    public ResponseEntity<?> register(@RequestBody Map<String, String> registrationData, HttpServletRequest request) {
        String username = registrationData.get("username");
        String password = registrationData.get("password");
        String confirmPassword = registrationData.get("confirmPassword");
        String role = registrationData.get("role");
        String secretCode = registrationData.get("secretCode");

        // Basic validation
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
        if (!role.equals("ADMIN") && !role.equals("USER") && !role.equals("SUPER_ADMIN")) {
            return ResponseEntity.badRequest().body("Invalid role. Must be ADMIN, USER, or SUPER_ADMIN");
        }

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        boolean hasAdmin = userRepository.existsByRole("ADMIN");

        // ===== ADMIN role validation =====
        if ("ADMIN".equalsIgnoreCase(role) && hasAdmin) {
            // Check if unlock session exists
            HttpSession session = request.getSession(false);
            boolean isUnlocked = session != null && session.getAttribute("REGISTER_UNLOCKED") != null;

            if (!isUnlocked) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Admin registration is locked. Please verify secret code first.");
                errorResponse.put("requiresCode", true);
                return ResponseEntity.status(403).body(errorResponse);
            }

            // Also check if secret code is provided
            if (secretCode == null || secretCode.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Secret code is required to register admin.");
                errorResponse.put("requiresCode", true);
                return ResponseEntity.status(403).body(errorResponse);
            }

            // Verify code again
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(SECRET_CODE_KEY);
            if (configOpt.isEmpty() || !secretCode.equals(configOpt.get().getConfigValue())) {
                // Invalidate session
                if (session != null) {
                    session.removeAttribute("REGISTER_UNLOCKED");
                }
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid secret code.");
                errorResponse.put("requiresCode", true);
                return ResponseEntity.status(403).body(errorResponse);
            }
        }

        // ===== USER role validation - First user must be ADMIN =====
        if ("USER".equalsIgnoreCase(role) && !hasAdmin) {
            return ResponseEntity.badRequest().body("First account must be ADMIN. Please register as Admin.");
        }

        // ===== SUPER_ADMIN role validation =====
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
            if (secretCode == null || secretCode.trim().isEmpty()) {
                return ResponseEntity.status(403).body("Secret code is required to create SUPER_ADMIN.");
            }
            Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(SECRET_CODE_KEY);
            if (configOpt.isEmpty() || !secretCode.equals(configOpt.get().getConfigValue())) {
                return ResponseEntity.status(403).body("Invalid secret code.");
            }
        }

        // Create user
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role.toUpperCase());

        User savedUser = userRepository.save(newUser);

        // Invalidate unlock session after successful registration
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("REGISTER_UNLOCKED");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("role", savedUser.getRole());
        response.put("message", "User registered successfully");

        return ResponseEntity.ok(response);
    }

    // ===== HELPER: Get Client IP =====
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // ===== INNER CLASS: Attempt Info =====
    private static class AttemptInfo {
        private int attempts = 0;
        private LocalDateTime firstAttemptTime = LocalDateTime.now();
        private LocalDateTime lockoutTime = null;

        public void incrementAttempts() {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                lockoutTime = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            }
        }

        public boolean isLockedOut() {
            if (lockoutTime == null) return false;
            return LocalDateTime.now().isBefore(lockoutTime);
        }

        public int getRemainingAttempts() {
            if (isLockedOut()) return 0;
            return Math.max(0, MAX_ATTEMPTS - attempts);
        }

        public long getRemainingLockoutMinutes() {
            if (lockoutTime == null) return 0;
            long minutes = java.time.Duration.between(LocalDateTime.now(), lockoutTime).toMinutes();
            return Math.max(0, minutes);
        }
    }
}