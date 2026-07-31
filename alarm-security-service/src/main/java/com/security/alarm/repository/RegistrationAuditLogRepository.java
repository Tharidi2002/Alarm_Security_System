package com.security.alarm.repository;

import com.security.alarm.entity.RegistrationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RegistrationAuditLogRepository extends JpaRepository<RegistrationAuditLog, Long> {
    List<RegistrationAuditLog> findAllByOrderByCreatedAtDesc();
    List<RegistrationAuditLog> findByRoleOrderByCreatedAtDesc(String role);
    long countByRole(String role);
    
    @Query("SELECT COUNT(l) FROM RegistrationAuditLog l WHERE l.role = 'ADMIN'")
    long countAdminRegistrations();
    
    @Query("SELECT COUNT(l) FROM RegistrationAuditLog l WHERE l.role = 'USER'")
    long countUserRegistrations();
}