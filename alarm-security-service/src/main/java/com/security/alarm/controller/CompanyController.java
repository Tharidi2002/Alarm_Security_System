package com.security.alarm.controller;

import com.security.alarm.entity.Company;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.User;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/companies")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public CompanyController(CompanyRepository companyRepository,
                             AlarmSystemRepository alarmSystemRepository,
                             UserRepository userRepository,
                             PermissionService permissionService) {
        this.companyRepository = companyRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
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
    // GET ALL COMPANIES - FIXED
    // ============================================================
    
    @GetMapping
    public ResponseEntity<?> getAllCompanies(@RequestParam(required = false) String username) {
        try {
            // If USER, return only their company
            if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
                Company userCompany = permissionService.getUserCompany(username);
                if (userCompany == null) {
                    return ResponseEntity.ok(List.of());
                }
                return ResponseEntity.ok(List.of(userCompany));
            }
            
            // Admin - all companies
            List<Company> companies = companyRepository.findAllByOrderByCompanyNameAsc();
            return ResponseEntity.ok(companies);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching companies: " + e.getMessage());
        }
    }

    // ============================================================
    // GET COMPANY BY ID
    // ============================================================
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id,
                                            @RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canAccessCompany(username, id)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Company company = companyOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("company", company);
            response.put("systemCount", companyRepository.countSystemsByCompanyId(id));
            response.put("userCount", companyRepository.countUsersByCompanyId(id));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET COMPANY SYSTEMS
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
            
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(id);
            return ResponseEntity.ok(systems);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET COMPANY USERS
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
            
            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<User> users = userRepository.findByCompanyId(id);
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // CREATE COMPANY - ADMIN ONLY
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
    // UPDATE COMPANY - ADMIN ONLY
    // ============================================================
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id,
                                           @RequestBody Company updatedCompany,
                                           @RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username)) {
                    return ResponseEntity.status(403).body("Access denied: Only Admin can update companies");
                }
            }
            
            Optional<Company> existingOpt = companyRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Company existing = existingOpt.get();
            
            if (updatedCompany.getCompanyName() != null && !updatedCompany.getCompanyName().trim().isEmpty()) {
                Optional<Company> dupCheck = companyRepository.findByCompanyName(updatedCompany.getCompanyName().trim());
                if (dupCheck.isPresent() && !dupCheck.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("Company name already exists");
                }
                existing.setCompanyName(updatedCompany.getCompanyName().trim());
            }
            
            if (updatedCompany.getAddress() != null) existing.setAddress(updatedCompany.getAddress());
            if (updatedCompany.getContactPerson() != null) existing.setContactPerson(updatedCompany.getContactPerson());
            if (updatedCompany.getContactEmail() != null) existing.setContactEmail(updatedCompany.getContactEmail());
            if (updatedCompany.getContactPhone() != null) existing.setContactPhone(updatedCompany.getContactPhone());
            if (updatedCompany.getRegistrationNumber() != null) existing.setRegistrationNumber(updatedCompany.getRegistrationNumber());
            if (updatedCompany.getTaxNumber() != null) existing.setTaxNumber(updatedCompany.getTaxNumber());
            if (updatedCompany.getStatus() != null && !updatedCompany.getStatus().isEmpty()) {
                existing.setStatus(updatedCompany.getStatus());
            }
            if (updatedCompany.getNotes() != null) existing.setNotes(updatedCompany.getNotes());
            
            existing.setUpdatedAt(LocalDateTime.now());
            
            Company saved = companyRepository.save(existing);
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating company: " + e.getMessage());
        }
    }

    // ============================================================
    // DELETE COMPANY - ADMIN ONLY
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
            
            long systemCount = companyRepository.countSystemsByCompanyId(id);
            if (systemCount > 0) {
                return ResponseEntity.badRequest().body(
                    "Cannot delete company. It has " + systemCount + " system(s) assigned."
                );
            }
            
            long userCount = companyRepository.countUsersByCompanyId(id);
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
    // GET COMPANY STATS - ADMIN ONLY
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
            stats.put("activeCompanies", companies.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
            
            long totalSystems = 0;
            long totalUsers = 0;
            for (Company c : companies) {
                totalSystems += companyRepository.countSystemsByCompanyId(c.getId());
                totalUsers += companyRepository.countUsersByCompanyId(c.getId());
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