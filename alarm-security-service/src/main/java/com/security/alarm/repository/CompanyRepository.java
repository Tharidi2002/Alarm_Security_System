package com.security.alarm.repository;

import com.security.alarm.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    // ============================================================
    // BASIC FINDERS
    // ============================================================
    Optional<Company> findByCompanyCode(String companyCode);
    Optional<Company> findByCompanyName(String companyName);
    List<Company> findByStatus(String status);
    List<Company> findAllByOrderByCompanyNameAsc();
    
    // ============================================================
    // COMPANY STATS
    // ============================================================
    @Query("SELECT COUNT(s) FROM AlarmSystem s WHERE s.company.id = :companyId")
    long countSystemsByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId")
    long countUsersByCompanyId(@Param("companyId") Long companyId);
    
    // ============================================================
    // ACTIVE/INACTIVE COMPANIES
    // ============================================================
    @Query("SELECT c FROM Company c WHERE c.status = 'ACTIVE' ORDER BY c.companyName ASC")
    List<Company> findActiveCompanies();
    
    @Query("SELECT c FROM Company c WHERE c.status = 'INACTIVE' ORDER BY c.companyName ASC")
    List<Company> findInactiveCompanies();
    
    @Query("SELECT COUNT(c) FROM Company c WHERE c.status = 'ACTIVE'")
    long countActiveCompanies();
    
    @Query("SELECT COUNT(c) FROM Company c WHERE c.status = 'INACTIVE'")
    long countInactiveCompanies();
    
    // ============================================================
    // STATUS CHANGE HISTORY
    // ============================================================
    @Query("SELECT c FROM Company c WHERE c.statusChangedAt IS NOT NULL ORDER BY c.statusChangedAt DESC")
    List<Company> findRecentlyChanged();
    
    @Query("SELECT c FROM Company c WHERE c.inactivatedAt IS NOT NULL ORDER BY c.inactivatedAt DESC")
    List<Company> findRecentlyDeactivated();
    
    @Query("SELECT c FROM Company c WHERE c.reactivatedAt IS NOT NULL ORDER BY c.reactivatedAt DESC")
    List<Company> findRecentlyReactivated();
}