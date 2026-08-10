package com.security.alarm.repository;

import com.security.alarm.entity.AlarmSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlarmSystemRepository extends JpaRepository<AlarmSystem, Long> {
    
    Optional<AlarmSystem> findBySimNumber(String simNumber);
    
    Optional<AlarmSystem> findBySystemCode(String systemCode);
    
    // ============================================================
    // COMPANY METHODS
    // ============================================================
    List<AlarmSystem> findByCompanyId(Long companyId);
    
    List<AlarmSystem> findByCompanyIdAndStatus(Long companyId, String status);
    
    // ⚠️ මෙය පමණක් තබන්න - duplicate එකක් නැති බවට වග බලාගන්න
    @Query("SELECT COUNT(s) FROM AlarmSystem s WHERE s.company.id = :companyId AND (s.deleted = false OR s.deleted IS NULL)")
    long countByCompanyId(@Param("companyId") Long companyId);
    
    List<AlarmSystem> findByCompanyIdOrderBySystemCodeAsc(Long companyId);
    
    @Query(value = "SELECT system_code FROM alarm_systems WHERE system_code LIKE 'ALARM-Z8B-%' ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLatestSystemCode();
    
    // ============================================================
    // SOFT DELETE METHODS
    // ============================================================
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = false OR s.deleted IS NULL")
    List<AlarmSystem> findAllActive();
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = false AND s.company.id = :companyId")
    List<AlarmSystem> findActiveByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = false AND s.company.id = :companyId AND s.status = :status")
    List<AlarmSystem> findActiveByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = false AND s.systemCode = :systemCode")
    Optional<AlarmSystem> findActiveBySystemCode(@Param("systemCode") String systemCode);
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = true")
    List<AlarmSystem> findAllDeleted();
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = true AND s.company.id = :companyId")
    List<AlarmSystem> findDeletedByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT s FROM AlarmSystem s WHERE s.deleted = true AND s.deletedAt < :cutoff")
    List<AlarmSystem> findDeletedBefore(@Param("cutoff") LocalDateTime cutoff);
    
    @Query("SELECT COUNT(s) FROM AlarmSystem s WHERE s.deleted = false")
    long countActive();
    
    @Modifying
    @Transactional
    @Query("DELETE FROM AlarmSystem s WHERE s.id = :id AND s.deleted = true")
    void permanentDeleteById(@Param("id") Long id);
}