package com.security.alarm.repository;

import com.security.alarm.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    
    List<AlertLog> findAllByOrderByReceivedAtDesc();
    
    List<AlertLog> findAllByAlarmSystemIdInOrderByReceivedAtDesc(List<Long> systemIds);
    
    // FOR SIREN CONTROL
    List<AlertLog> findByAlarmSystemIdAndStatusOrderByReceivedAtDesc(Long systemId, String status);
    
    long countByAlarmSystemIdAndStatus(Long systemId, String status);
    
    long countByStatus(String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = 'RESOLVED'")
    long countResolved();
    
    @Query("SELECT a FROM AlertLog a LEFT JOIN FETCH a.alarmSystem WHERE a.id = :id")
    AlertLog findByIdWithSystem(Long id);
    
    // Report queries
    List<AlertLog> findByReceivedAtBetween(LocalDateTime from, LocalDateTime to);
    
    List<AlertLog> findByAlarmSystemIdInAndReceivedAtBetween(List<Long> systemIds, LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT a FROM AlertLog a WHERE a.receivedAt BETWEEN :from AND :to " +
           "AND (:systemCode IS NULL OR a.alarmSystem.systemCode = :systemCode) " +
           "AND (:status IS NULL OR a.status = :status)")
    List<AlertLog> findDetailedAlerts(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("systemCode") String systemCode,
                                      @Param("status") String status);
    
    // ============================================================
    // NEW: Get alerts by company (via system company)
    // ============================================================
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = :status ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = 'PENDING'")
    long countPendingByCompanyId(@Param("companyId") Long companyId);
}