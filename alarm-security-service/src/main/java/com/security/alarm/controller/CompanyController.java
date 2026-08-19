package com.security.alarm.controller;

import com.security.alarm.entity.Company;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.User;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.service.PermissionService;
import com.security.alarm.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/companies")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class CompanyController {

    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyRepository companyRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    public CompanyController(CompanyRepository companyRepository,
                             AlarmSystemRepository alarmSystemRepository,
                             UserRepository userRepository,
                             PermissionService permissionService,
                             NotificationService notificationService) {
        this.companyRepository = companyRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.notificationService = notificationService;
    }

    private String generateCompanyCode() {
        List<Company> companies = companyRepository.findAll();
        if (companies.isEmpty()) {
            return "COMP-001";
        }
        int maxNum = 0;
        for (Company c : companies) {
            String code = c.getCompanyCode();
            if (code != null && code.startsWith("COMP-")) {
                try {
                    int num = Integer.parseInt(code.substring(5));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("COMP-%03d", maxNum + 1);
    }

    // ============================================================
    // GET ALL COMPANIES
    // ============================================================
    @GetMapping
    public ResponseEntity<?> getAllCompanies(@RequestParam(required = false) String username) {
        try {
            System.out.println("📌 GET /api/admin/companies - username: " + username);
            
            List<Company> companies;
            
            if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
                Company userCompany = permissionService.getUserCompany(username);
                if (userCompany == null) {
                    return ResponseEntity.ok(Collections.emptyList());
                }
                companies = List.of(userCompany);
            } else {
                companies = companyRepository.findAllByOrderByCompanyNameAsc();
            }
            
            System.out.println("📌 Returning " + companies.size() + " companies");
            return ResponseEntity.ok(companies);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // ============================================================
    // GET COMPANY BY ID - WITH FULL DETAILS
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id,
                                            @RequestParam(required = false) String username) {
        try {
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Company company = companyOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", company.getId());
            response.put("companyCode", company.getCompanyCode());
            response.put("companyName", company.getCompanyName());
            response.put("address", company.getAddress());
            response.put("contactPerson", company.getContactPerson());
            response.put("contactEmail", company.getContactEmail());
            response.put("contactPhone", company.getContactPhone());
            response.put("status", company.getStatus());
            response.put("notes", company.getNotes());
            response.put("createdAt", company.getCreatedAt());
            response.put("updatedAt", company.getUpdatedAt());
            
            // ===== INACTIVE DETAILS - MUST BE INCLUDED =====
            response.put("statusChangedAt", company.getStatusChangedAt());
            response.put("inactivatedAt", company.getInactivatedAt());
            response.put("inactivatedBy", company.getInactivatedBy());
            response.put("inactivationReason", company.getInactivationReason());
            response.put("inactivationDescription", company.getInactivationDescription());
            response.put("reactivatedAt", company.getReactivatedAt());
            response.put("reactivatedBy", company.getReactivatedBy());
            
            // Counts
            response.put("systemCount", alarmSystemRepository.countByCompanyId(id));
            response.put("userCount", userRepository.countByCompanyId(id));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET SYSTEMS BY COMPANY
    // ============================================================
    @GetMapping("/{id}/systems")
    public ResponseEntity<?> getCompanySystems(@PathVariable Long id,
                                               @RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canAccessCompany(username, id)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(id);
            return ResponseEntity.ok(systems);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET USERS BY COMPANY
    // ============================================================
    @GetMapping("/{id}/users")
    public ResponseEntity<?> getCompanyUsers(@PathVariable Long id,
                                             @RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canAccessCompany(username, id)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            List<User> users = userRepository.findByCompanyId(id);
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // CREATE COMPANY
    // ============================================================
    @PostMapping
    public ResponseEntity<?> createCompany(@RequestBody Company company,
                                           @RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username)) {
                    return ResponseEntity.status(403).body("Access denied: Only Admin can create companies");
                }
            }
            
            if (company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Company name is required");
            }
            
            if (companyRepository.findByCompanyName(company.getCompanyName().trim()).isPresent()) {
                return ResponseEntity.badRequest().body("Company name already exists");
            }
            
            company.setCompanyCode(generateCompanyCode());
            company.setStatus(company.getStatus() != null ? company.getStatus() : "ACTIVE");
            company.setCreatedAt(LocalDateTime.now());
            company.setUpdatedAt(LocalDateTime.now());
            
            Company saved = companyRepository.save(company);
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating company: " + e.getMessage());
        }
    }

    // ============================================================
    // UPDATE COMPANY
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id,
                                           @RequestBody Map<String, Object> payload,
                                           @RequestParam(required = false) String username) {
        try {
            System.out.println("📌 PUT /api/admin/companies/" + id + " - username: " + username);
            
            Optional<Company> existingOpt = companyRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Company existing = existingOpt.get();
            
            // Permission check
            if (username != null && !username.isEmpty()) {
                if (permissionService.isUser(username)) {
                    Long userCompanyId = permissionService.getUserCompanyId(username);
                    if (userCompanyId == null || !userCompanyId.equals(id)) {
                        return ResponseEntity.status(403).body("Access denied");
                    }
                }
            }
            
            // Update fields from payload
            if (payload.containsKey("companyName") && payload.get("companyName") != null) {
                String name = payload.get("companyName").toString().trim();
                if (!name.isEmpty()) {
                    existing.setCompanyName(name);
                }
            }
            
            if (payload.containsKey("address") && payload.get("address") != null) {
                existing.setAddress(payload.get("address").toString());
            }
            
            if (payload.containsKey("contactPerson") && payload.get("contactPerson") != null) {
                existing.setContactPerson(payload.get("contactPerson").toString());
            }
            
            if (payload.containsKey("contactEmail") && payload.get("contactEmail") != null) {
                existing.setContactEmail(payload.get("contactEmail").toString());
            }
            
            if (payload.containsKey("contactPhone") && payload.get("contactPhone") != null) {
                existing.setContactPhone(payload.get("contactPhone").toString());
            }
            
            if (payload.containsKey("notes") && payload.get("notes") != null) {
                existing.setNotes(payload.get("notes").toString());
            }
            
            existing.setUpdatedAt(LocalDateTime.now());
            
            Company saved = companyRepository.save(existing);
            System.out.println("✅ Company updated: " + saved.getCompanyName());
            
            // Return updated company
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("companyCode", saved.getCompanyCode());
            response.put("companyName", saved.getCompanyName());
            response.put("address", saved.getAddress());
            response.put("contactPerson", saved.getContactPerson());
            response.put("contactEmail", saved.getContactEmail());
            response.put("contactPhone", saved.getContactPhone());
            response.put("status", saved.getStatus());
            response.put("notes", saved.getNotes());
            
            // Get counts
            try {
                response.put("systemCount", alarmSystemRepository.countByCompanyId(id));
            } catch (Exception e) {
                response.put("systemCount", 0);
            }
            try {
                response.put("userCount", userRepository.countByCompanyId(id));
            } catch (Exception e) {
                response.put("userCount", 0);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating company: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating company: " + e.getMessage());
        }
    }

    // ============================================================
    // DEACTIVATE COMPANY (ADMIN ONLY)
    // ============================================================
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateCompany(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestParam String username) {
        
        try {
            // Only Admin can deactivate
            if (!permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied: Only Admin can deactivate companies");
            }
            
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Company company = companyOpt.get();
            
            // Check if already inactive
            if ("INACTIVE".equals(company.getStatus())) {
                return ResponseEntity.badRequest().body("Company is already INACTIVE");
            }
            
            // Validate reason
            String reason = payload.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Reason is required to deactivate company");
            }
            
            String description = payload.get("description");
            
            // Update status
            company.setStatus("INACTIVE");
            company.setStatusChangedAt(LocalDateTime.now());
            company.setInactivatedAt(LocalDateTime.now());
            company.setInactivatedBy(username);
            company.setInactivationReason(reason.trim());
            company.setInactivationDescription(description != null ? description.trim() : null);
            
            Company saved = companyRepository.save(company);
            
            // Send notification to all company users
            try {
                notificationService.sendCompanyDeactivatedNotification(saved, username);
            } catch (Exception e) {
                logger.error("Failed to send deactivation notification: {}", e.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Company deactivated successfully");
            response.put("company", saved);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error deactivating company: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error deactivating company: " + e.getMessage());
        }
    }

    // ============================================================
    // REACTIVATE COMPANY (ADMIN ONLY)
    // ============================================================
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivateCompany(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestParam String username) {
        
        try {
            // Only Admin can reactivate
            if (!permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied: Only Admin can reactivate companies");
            }
            
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Company company = companyOpt.get();
            
            // Check if already active
            if ("ACTIVE".equals(company.getStatus())) {
                return ResponseEntity.badRequest().body("Company is already ACTIVE");
            }
            
            String note = payload.get("note");
            
            // Update status
            company.setStatus("ACTIVE");
            company.setStatusChangedAt(LocalDateTime.now());
            company.setReactivatedAt(LocalDateTime.now());
            company.setReactivatedBy(username);
            
            Company saved = companyRepository.save(company);
            
            // Send notification to all company users
            try {
                notificationService.sendCompanyReactivatedNotification(saved, username);
            } catch (Exception e) {
                logger.error("Failed to send reactivation notification: {}", e.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Company reactivated successfully");
            response.put("company", saved);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error reactivating company: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error reactivating company: " + e.getMessage());
        }
    }

    // ============================================================
    // DELETE COMPANY (ADMIN ONLY)
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id,
                                           @RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username)) {
                    return ResponseEntity.status(403).body("Access denied: Only Admin can delete companies");
                }
            }
            
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            long systemCount = alarmSystemRepository.countByCompanyId(id);
            if (systemCount > 0) {
                return ResponseEntity.badRequest().body(
                    "Cannot delete company. It has " + systemCount + " system(s) assigned."
                );
            }
            
            long userCount = userRepository.countByCompanyId(id);
            if (userCount > 0) {
                return ResponseEntity.badRequest().body(
                    "Cannot delete company. It has " + userCount + " user(s) assigned."
                );
            }
            
            companyRepository.deleteById(id);
            return ResponseEntity.ok("Company deleted successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting company: " + e.getMessage());
        }
    }

    // ============================================================
    // GET COMPANY STATS (ADMIN ONLY)
    // ============================================================
    @GetMapping("/stats")
    public ResponseEntity<?> getCompanyStats(@RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            List<Company> companies = companyRepository.findAll();
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCompanies", companies.size());
            stats.put("activeCompanies", companyRepository.countActiveCompanies());
            stats.put("inactiveCompanies", companyRepository.countInactiveCompanies());
            
            long totalSystems = 0;
            long totalUsers = 0;
            for (Company c : companies) {
                totalSystems += alarmSystemRepository.countByCompanyId(c.getId());
                totalUsers += userRepository.countByCompanyId(c.getId());
            }
            stats.put("totalSystems", totalSystems);
            stats.put("totalUsers", totalUsers);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}