package com.security.alarm.repository;

import com.security.alarm.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    
    // ============================================================
    // EXISTING QUERIES - Keep as is
    // ============================================================
    List<AlertLog> findAllByOrderByReceivedAtDesc();
    
    List<AlertLog> findAllByAlarmSystemIdInOrderByReceivedAtDesc(List<Long> systemIds);
    
    List<AlertLog> findByAlarmSystemIdAndStatusOrderByReceivedAtDesc(Long systemId, String status);
    
    long countByAlarmSystemIdAndStatus(Long systemId, String status);
    
    long countByStatus(String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = 'RESOLVED'")
    long countResolved();
    
    @Query("SELECT a FROM AlertLog a LEFT JOIN FETCH a.alarmSystem WHERE a.id = :id")
    AlertLog findByIdWithSystem(Long id);
    
    List<AlertLog> findByReceivedAtBetween(LocalDateTime from, LocalDateTime to);
    
    List<AlertLog> findByAlarmSystemIdInAndReceivedAtBetween(List<Long> systemIds, LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT a FROM AlertLog a WHERE a.receivedAt BETWEEN :from AND :to " +
           "AND (:systemCode IS NULL OR a.alarmSystem.systemCode = :systemCode) " +
           "AND (:status IS NULL OR a.status = :status)")
    List<AlertLog> findDetailedAlerts(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("systemCode") String systemCode,
                                      @Param("status") String status);
    
    List<AlertLog> findByAlarmSystemId(Long systemId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM AlertLog a WHERE a.alarmSystem.id = :systemId")
    void deleteByAlarmSystemId(@Param("systemId") Long systemId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.id = :systemId ORDER BY a.receivedAt DESC")
    List<AlertLog> findByAlarmSystemIdOrderByReceivedAtDesc(@Param("systemId") Long systemId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = :status ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = 'PENDING'")
    long countPendingByCompanyId(@Param("companyId") Long companyId);

    // ============================================================
    // NEW: EXCLUDE REJECTED ALERTS
    // ============================================================
    
    // Get all alerts EXCEPT REJECTED
    @Query("SELECT a FROM AlertLog a WHERE a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findAllActiveAlerts();
    
    // Get alerts by system IDs EXCEPT REJECTED
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.id IN :systemIds AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findAllByAlarmSystemIdInAndNotRejected(@Param("systemIds") List<Long> systemIds);
    
    // Count pending alerts EXCEPT REJECTED
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = 'PENDING' AND a.status != 'REJECTED'")
    long countPendingActive();
    
    // Count by system and status EXCEPT REJECTED
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.alarmSystem.id = :systemId AND a.status = :status AND a.status != 'REJECTED'")
    long countByAlarmSystemIdAndStatusActive(@Param("systemId") Long systemId, @Param("status") String status);
    
    // Get pending alerts EXCEPT REJECTED
    @Query("SELECT a FROM AlertLog a WHERE a.status = 'PENDING' AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findPendingAlerts();
    
    // Get alerts by status EXCEPT REJECTED
    @Query("SELECT a FROM AlertLog a WHERE a.status = :status AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findByStatusActive(@Param("status") String status);
    
    // Company-based EXCEPT REJECTED
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdActive(@Param("companyId") Long companyId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = :status AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdAndStatusActive(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = 'PENDING' AND a.status != 'REJECTED'")
    long countPendingByCompanyIdActive(@Param("companyId") Long companyId);
}