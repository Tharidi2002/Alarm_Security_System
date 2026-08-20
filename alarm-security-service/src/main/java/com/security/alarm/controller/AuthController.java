package com.security.alarm.controller;

import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.SystemConfig;
import com.security.alarm.entity.RegistrationAuditLog;
import com.security.alarm.entity.Company;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.SystemConfigRepository;
import com.security.alarm.repository.RegistrationAuditLogRepository;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.service.PermissionService;
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
    private final RegistrationAuditLogRepository auditLogRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    // Rate limiting
    private final Map<String, AttemptInfo> attemptTracker = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;
    private static final String SECRET_CODE_KEY = "MASTER_SECRET_CODE";

    public AuthController(UserRepository userRepository,
                          UserSystemRepository userSystemRepository,
                          AlarmSystemRepository alarmSystemRepository,
                          SystemConfigRepository systemConfigRepository,
                          RegistrationAuditLogRepository auditLogRepository,
                          CompanyRepository companyRepository,
                          PasswordEncoder passwordEncoder,
                          PermissionService permissionService) {
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.auditLogRepository = auditLogRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    // ============================================================
    // HANDLE OPTIONS REQUEST
    // ============================================================
    
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Max-Age", "3600")
            .build();
    }

    // ============================================================
    // CHECK ADMIN STATUS
    // ============================================================
    
    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Object>> checkAdmin(HttpServletRequest request) {
        boolean hasAdmin = userRepository.existsByRole("ADMIN");
        long adminCount = userRepository.countAdmins();
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasAdmin", hasAdmin);
        response.put("adminCount", adminCount);
        
        HttpSession session = request.getSession(false);
        boolean isUnlocked = session != null && session.getAttribute("REGISTER_UNLOCKED") != null;
        response.put("isUnlocked", isUnlocked);
        
        Optional<User> firstAdmin = userRepository.findFirstByRoleOrderByIdAsc("ADMIN");
        firstAdmin.ifPresent(admin -> {
            response.put("firstAdminId", admin.getId());
            response.put("firstAdminUsername", admin.getUsername());
        });
        
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // VERIFY SECRET CODE
    // ============================================================
    
    @PostMapping("/verify-secret")
    public ResponseEntity<?> verifySecret(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String providedCode = request.get("secretCode");
        String clientIp = getClientIp(httpRequest);

        boolean hasAdmin = userRepository.existsByRole("ADMIN");
        if (!hasAdmin) {
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "No admin exists. You can register directly."
            ));
        }

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

        Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(SECRET_CODE_KEY);
        if (configOpt.isEmpty()) {
            return ResponseEntity.status(500).body(Map.of(
                "valid", false,
                "message", "System configuration error. Contact administrator."
            ));
        }

        String storedCode = configOpt.get().getConfigValue();

        if (providedCode != null && providedCode.equals(storedCode)) {
            attemptTracker.remove(clientIp);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("REGISTER_UNLOCKED", true);
            session.setAttribute("UNLOCKED_BY_IP", clientIp);
            session.setMaxInactiveInterval(900);

            return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "Code verified. Registration unlocked for 15 minutes."
            ));
        } else {
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

    // ============================================================
    // LOGIN ENDPOINT - UPDATED WITH COMPANY INFO
    // ============================================================
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

        // ============================================================
        // FIX: Check if user is inactive - Return 200 OK with isActive: false
        // ============================================================
        if (user.getIsActive() != null && !user.getIsActive()) {
            Map<String, Object> inactiveResponse = new HashMap<>();
            inactiveResponse.put("success", false);
            inactiveResponse.put("isActive", false);
            inactiveResponse.put("message", "Your account is inactive");
            inactiveResponse.put("id", user.getId());
            inactiveResponse.put("username", user.getUsername());
            inactiveResponse.put("role", user.getRole());
            inactiveResponse.put("registrationMethod", user.getRegistrationMethod());
            inactiveResponse.put("isSuperAdmin", false);
            inactiveResponse.put("inactivatedAt", user.getInactivatedAt());
            inactiveResponse.put("inactivatedBy", user.getInactivatedBy());
            inactiveResponse.put("inactivationReason", user.getInactivationReason());
            inactiveResponse.put("inactivationDescription", user.getInactivationDescription());
            
            if (user.getCompany() != null) {
                inactiveResponse.put("companyId", user.getCompany().getId());
                inactiveResponse.put("companyName", user.getCompany().getCompanyName());
                inactiveResponse.put("companyCode", user.getCompany().getCompanyCode());
            }
            
            // Return 200 OK so frontend can handle gracefully
            return ResponseEntity.ok(inactiveResponse);
        }

        // ============================================================
        // ACTIVE USER - Normal login response
        // ============================================================
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("isActive", user.getIsActive() != null ? user.getIsActive() : true);
        response.put("registrationMethod", permissionService.getRegistrationMethod(username));
        response.put("isSuperAdmin", "FORM".equals(permissionService.getRegistrationMethod(username)));
        
        // Add inactive details (in case they were reactivated but we keep history)
        response.put("inactivatedAt", user.getInactivatedAt());
        response.put("inactivatedBy", user.getInactivatedBy());
        response.put("inactivationReason", user.getInactivationReason());
        response.put("inactivationDescription", user.getInactivationDescription());
        
        if (user.getCompany() != null) {
            response.put("companyId", user.getCompany().getId());
            response.put("companyName", user.getCompany().getCompanyName());
            response.put("companyCode", user.getCompany().getCompanyCode());
        }

        // Get accessible systems
        List<String> accessibleSystems;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            accessibleSystems = alarmSystemRepository.findAll().stream()
                .map(s -> s.getSystemCode())
                .collect(Collectors.toList());
        } else {
            Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
            if (companyId != null) {
                accessibleSystems = alarmSystemRepository.findByCompanyId(companyId).stream()
                    .map(s -> s.getSystemCode())
                    .collect(Collectors.toList());
            } else {
                accessibleSystems = List.of();
            }
        }
        response.put("accessibleSystems", accessibleSystems);

        // Also keep assignedSystems for backward compatibility
        if ("USER".equalsIgnoreCase(user.getRole())) {
            List<UserSystem> mappings = userSystemRepository.findAllByUserId(user.getId());
            List<String> assignedSystems = mappings.stream()
                .map(m -> alarmSystemRepository.findById(m.getSystemId()))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getSystemCode())
                .collect(Collectors.toList());
            response.put("assignedSystems", assignedSystems);
        } else {
            response.put("assignedSystems", accessibleSystems);
        }

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // REGISTER ENDPOINT - UPDATED WITH COMPANY SUPPORT
    // ============================================================
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registrationData, HttpServletRequest request) {
        String username = registrationData.get("username");
        String password = registrationData.get("password");
        String confirmPassword = registrationData.get("confirmPassword");
        String role = registrationData.get("role");
        String secretCode = registrationData.get("secretCode");
        String companyIdStr = registrationData.get("companyId");
        String clientIp = getClientIp(request);

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
        if (!role.equals("ADMIN") && !role.equals("USER")) {
            return ResponseEntity.badRequest().body("Invalid role. Must be ADMIN or USER");
        }

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        boolean hasAdmin = userRepository.existsByRole("ADMIN");
        
        // ============================================================
        // NEW: Validate company for USER role
        // ============================================================
        Long companyId = null;
        Company company = null;
        
        if ("USER".equalsIgnoreCase(role)) {
            if (companyIdStr == null || companyIdStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Company is required for USER role");
            }
            try {
                companyId = Long.parseLong(companyIdStr.trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Invalid company ID");
            }
            
            Optional<Company> companyOpt = companyRepository.findById(companyId);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Company not found");
            }
            company = companyOpt.get();
        }

        // ADMIN role validation
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (hasAdmin) {
                if (secretCode == null || secretCode.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Secret code is required to register admin.");
                    errorResponse.put("requiresCode", true);
                    return ResponseEntity.status(403).body(errorResponse);
                }

                Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(SECRET_CODE_KEY);
                if (configOpt.isEmpty()) {
                    return ResponseEntity.status(500).body("System configuration error. Contact administrator.");
                }

                String storedCode = configOpt.get().getConfigValue();
                
                if (!secretCode.equals(storedCode)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid secret code.");
                    errorResponse.put("requiresCode", true);
                    return ResponseEntity.status(403).body(errorResponse);
                }

                // Log admin registration
                RegistrationAuditLog log = new RegistrationAuditLog();
                log.setUsername(username.trim());
                log.setRole("ADMIN");
                log.setRegisteredBy("SECRET_CODE (" + clientIp + ")");
                log.setRegisteredFromIp(clientIp);
                log.setMethod("FORM");
                log.setNotes("Admin registered using secret code.");
                auditLogRepository.save(log);
                
            } else {
                // First admin - no code required
                RegistrationAuditLog log = new RegistrationAuditLog();
                log.setUsername(username.trim());
                log.setRole("ADMIN");
                log.setRegisteredBy("FIRST_ADMIN");
                log.setRegisteredFromIp(clientIp);
                log.setMethod("FORM");
                log.setNotes("First admin account created. System initialized.");
                auditLogRepository.save(log);
            }
        }

        // USER role validation
        if ("USER".equalsIgnoreCase(role)) {
            if (!hasAdmin) {
                return ResponseEntity.badRequest().body("First account must be ADMIN. Please register as Admin.");
            }
            
            RegistrationAuditLog log = new RegistrationAuditLog();
            log.setUsername(username.trim());
            log.setRole("USER");
            log.setRegisteredBy("REGISTER_FORM (" + clientIp + ")");
            log.setRegisteredFromIp(clientIp);
            log.setMethod("FORM");
            log.setNotes("User registered via registration form. Company: " + (company != null ? company.getCompanyName() : "None"));
            auditLogRepository.save(log);
        }

        // Create user
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role.toUpperCase());
        newUser.setCompany(company); // NEW: Set company
        newUser.setIsActive(true); // NEW: Default active

        // NEW: Set registration method based on how user was registered
        if ("ADMIN".equalsIgnoreCase(role) && !hasAdmin) {
            // First admin - FORM method
            newUser.setRegistrationMethod("FORM");
        } else if ("ADMIN".equalsIgnoreCase(role) && hasAdmin) {
            // Additional admin via FORM with secret code - still FORM method
            newUser.setRegistrationMethod("FORM");
        } else if ("USER".equalsIgnoreCase(role)) {
            // USER via FORM - FORM method
            newUser.setRegistrationMethod("FORM");
        }

        User savedUser = userRepository.save(newUser);

        // Clear unlock session if exists
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("REGISTER_UNLOCKED");
            session.removeAttribute("UNLOCKED_BY_IP");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("role", savedUser.getRole());
        if (company != null) {
            response.put("companyId", company.getId());
            response.put("companyName", company.getCompanyName());
        }
        response.put("message", "User registered successfully");

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET REGISTRATION AUDIT LOGS
    // ============================================================
    
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) String role) {
        List<RegistrationAuditLog> logs;
        if (role != null && !role.trim().isEmpty()) {
            logs = auditLogRepository.findByRoleOrderByCreatedAtDesc(role.toUpperCase());
        } else {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("totalAdminRegistrations", auditLogRepository.countAdminRegistrations());
        response.put("totalUserRegistrations", auditLogRepository.countUserRegistrations());
        response.put("totalRegistrations", auditLogRepository.count());
        
        return ResponseEntity.ok(response);
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

    // ============================================================
    // INNER CLASS: Attempt Info
    // ============================================================
    
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