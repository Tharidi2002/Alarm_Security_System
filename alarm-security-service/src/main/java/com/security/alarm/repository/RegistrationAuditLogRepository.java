package com.security.alarm.repository;

import com.security.alarm.entity.RegistrationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RegistrationAuditLogRepository extends JpaRepository<RegistrationAuditLog, Long> {
    
    List<RegistrationAuditLog> findAllByOrderByCreatedAtDesc();
    List<RegistrationAuditLog> findByRoleOrderByCreatedAtDesc(String role);
    long countByRole(String role);
    
    @Query("SELECT COUNT(l) FROM RegistrationAuditLog l WHERE l.role = 'ADMIN'")
    long countAdminRegistrations();
    
    @Query("SELECT COUNT(l) FROM RegistrationAuditLog l WHERE l.role = 'USER'")
    long countUserRegistrations();
    
    // ============================================================
    // NEW QUERY METHODS FOR ADMIN ACCESS CONTROL
    // ============================================================
    
    @Query("SELECT l FROM RegistrationAuditLog l WHERE l.username = :username ORDER BY l.createdAt DESC")
    Optional<RegistrationAuditLog> findLatestByUsername(@Param("username") String username);
    
    @Query("SELECT l FROM RegistrationAuditLog l WHERE l.username = :username AND l.method = 'FORM'")
    Optional<RegistrationAuditLog> findByUsernameAndMethodForm(@Param("username") String username);
    
    @Query("SELECT l FROM RegistrationAuditLog l WHERE l.username = :username AND l.method = 'ADMIN_PANEL'")
    Optional<RegistrationAuditLog> findByUsernameAndMethodAdminPanel(@Param("username") String username);
    
    @Query("SELECT COUNT(l) FROM RegistrationAuditLog l WHERE l.method = 'FORM' AND l.role = 'ADMIN'")
    long countFormAdmins();
}