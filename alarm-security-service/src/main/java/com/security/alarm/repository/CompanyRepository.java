package com.security.alarm.repository;

import com.security.alarm.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCompanyCode(String companyCode);
    Optional<Company> findByCompanyName(String companyName);
    List<Company> findByStatus(String status);
    List<Company> findAllByOrderByCompanyNameAsc();
    
    @Query("SELECT COUNT(s) FROM AlarmSystem s WHERE s.company.id = :companyId")
    long countSystemsByCompanyId(Long companyId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId")
    long countUsersByCompanyId(Long companyId);
    
    @Query("SELECT c FROM Company c WHERE c.status = 'ACTIVE' ORDER BY c.companyName ASC")
    List<Company> findActiveCompanies();
}