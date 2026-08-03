package com.security.alarm.controller;

import com.security.alarm.entity.Company;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.User;
import com.security.alarm.repository.CompanyRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.UserRepository;
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

    public CompanyController(CompanyRepository companyRepository,
                             AlarmSystemRepository alarmSystemRepository,
                             UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.userRepository = userRepository;
    }

    // ===== GENERATE COMPANY CODE =====
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

    // ===== GET ALL COMPANIES =====
    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyRepository.findAllByOrderByCompanyNameAsc());
    }

    // ===== GET COMPANY BY ID =====
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
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
    }

    // ===== GET COMPANY SYSTEMS =====
    @GetMapping("/{id}/systems")
    public ResponseEntity<?> getCompanySystems(@PathVariable Long id) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(id);
        return ResponseEntity.ok(systems);
    }

    // ===== GET COMPANY USERS =====
    @GetMapping("/{id}/users")
    public ResponseEntity<?> getCompanyUsers(@PathVariable Long id) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<User> users = userRepository.findByCompanyId(id);
        return ResponseEntity.ok(users);
    }

    // ===== CREATE COMPANY =====
    @PostMapping
    public ResponseEntity<?> createCompany(@RequestBody Company company) {
        // Validation
        if (company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Company name is required");
        }
        
        // Check duplicate name
        if (companyRepository.findByCompanyName(company.getCompanyName().trim()).isPresent()) {
            return ResponseEntity.badRequest().body("Company name already exists");
        }
        
        // Set company code
        company.setCompanyCode(generateCompanyCode());
        company.setStatus(company.getStatus() != null ? company.getStatus() : "ACTIVE");
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        
        Company saved = companyRepository.save(company);
        return ResponseEntity.ok(saved);
    }

    // ===== UPDATE COMPANY =====
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody Company updatedCompany) {
        Optional<Company> existingOpt = companyRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Company existing = existingOpt.get();
        
        // Update fields
        if (updatedCompany.getCompanyName() != null && !updatedCompany.getCompanyName().trim().isEmpty()) {
            // Check duplicate name (except self)
            Optional<Company> dupCheck = companyRepository.findByCompanyName(updatedCompany.getCompanyName().trim());
            if (dupCheck.isPresent() && !dupCheck.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("Company name already exists");
            }
            existing.setCompanyName(updatedCompany.getCompanyName().trim());
        }
        
        if (updatedCompany.getAddress() != null) {
            existing.setAddress(updatedCompany.getAddress());
        }
        if (updatedCompany.getContactPerson() != null) {
            existing.setContactPerson(updatedCompany.getContactPerson());
        }
        if (updatedCompany.getContactEmail() != null) {
            existing.setContactEmail(updatedCompany.getContactEmail());
        }
        if (updatedCompany.getContactPhone() != null) {
            existing.setContactPhone(updatedCompany.getContactPhone());
        }
        if (updatedCompany.getRegistrationNumber() != null) {
            existing.setRegistrationNumber(updatedCompany.getRegistrationNumber());
        }
        if (updatedCompany.getTaxNumber() != null) {
            existing.setTaxNumber(updatedCompany.getTaxNumber());
        }
        if (updatedCompany.getStatus() != null && !updatedCompany.getStatus().isEmpty()) {
            existing.setStatus(updatedCompany.getStatus());
        }
        if (updatedCompany.getNotes() != null) {
            existing.setNotes(updatedCompany.getNotes());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        Company saved = companyRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // ===== DELETE COMPANY =====
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Company company = companyOpt.get();
        
        // Check if company has systems
        long systemCount = companyRepository.countSystemsByCompanyId(id);
        if (systemCount > 0) {
            return ResponseEntity.badRequest().body(
                "Cannot delete company. It has " + systemCount + " system(s) assigned. " +
                "Please reassign or delete them first."
            );
        }
        
        // Check if company has users
        long userCount = companyRepository.countUsersByCompanyId(id);
        if (userCount > 0) {
            return ResponseEntity.badRequest().body(
                "Cannot delete company. It has " + userCount + " user(s) assigned. " +
                "Please reassign or delete them first."
            );
        }
        
        companyRepository.deleteById(id);
        return ResponseEntity.ok("Company deleted successfully");
    }

    // ===== GET COMPANY STATS =====
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCompanyStats() {
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
    }
}