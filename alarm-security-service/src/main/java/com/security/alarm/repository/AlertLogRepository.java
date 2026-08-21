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
    // EXISTING QUERIES
    // ============================================================
    List<AlertLog> findAllByOrderByReceivedAtDesc();
    List<AlertLog> findAllByAlarmSystemIdInOrderByReceivedAtDesc(List<Long> systemIds);
    List<AlertLog> findByAlarmSystemIdAndStatusOrderByReceivedAtDesc(Long systemId, String status);
    long countByAlarmSystemIdAndStatus(Long systemId, String status);
    long countByStatus(String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = 'RESOLVED'")
    long countResolved();
    
    @Query("SELECT a FROM AlertLog a LEFT JOIN FETCH a.alarmSystem WHERE a.id = :id")
    AlertLog findByIdWithSystem(@Param("id") Long id);
    
    List<AlertLog> findByReceivedAtBetween(LocalDateTime from, LocalDateTime to);
    List<AlertLog> findByAlarmSystemIdInAndReceivedAtBetween(List<Long> systemIds, LocalDateTime from, LocalDateTime to);
    List<AlertLog> findByAlarmSystemId(Long systemId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM AlertLog a WHERE a.alarmSystem.id = :systemId")
    void deleteByAlarmSystemId(@Param("systemId") Long systemId);
    
    List<AlertLog> findByAlarmSystemIdOrderByReceivedAtDesc(Long systemId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = :status ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = 'PENDING'")
    long countPendingByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT a FROM AlertLog a WHERE a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findAllActiveAlerts();
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.id IN :systemIds AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findAllByAlarmSystemIdInAndNotRejected(@Param("systemIds") List<Long> systemIds);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = 'PENDING' AND a.status != 'REJECTED'")
    long countPendingActive();
    
    @Query("SELECT a FROM AlertLog a WHERE a.status = 'PENDING' AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findPendingAlerts();
    
    @Query("SELECT a FROM AlertLog a WHERE a.status = :status AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findByStatusActive(@Param("status") String status);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdActive(@Param("companyId") Long companyId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = :status AND a.status != 'REJECTED' ORDER BY a.receivedAt DESC")
    List<AlertLog> findByCompanyIdAndStatusActive(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.alarmSystem.company.id = :companyId AND a.status = 'PENDING' AND a.status != 'REJECTED'")
    long countPendingByCompanyIdActive(@Param("companyId") Long companyId);

    // ============================================================
    // NEW: RETENTION QUERIES
    // ============================================================
    
    @Query("SELECT a FROM AlertLog a WHERE a.isExported = false AND a.receivedAt < :cutoff AND a.retentionStatus = 'ACTIVE'")
    List<AlertLog> findUnexportedOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a FROM AlertLog a WHERE a.isExported = true AND a.exportedAt < :cutoff AND a.deletionPending = false AND a.retentionStatus = 'ACTIVE'")
    List<AlertLog> findExportedOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a FROM AlertLog a WHERE a.deletionPending = true AND a.scheduledDeleteAt < :now AND a.retentionStatus = 'PENDING_DELETE'")
    List<AlertLog> findReadyForDeletion(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM AlertLog a WHERE a.retentionStatus = 'PENDING_DELETE' AND a.scheduledDeleteAt > :now")
    List<AlertLog> findPendingDeletion(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.isExported = false AND a.receivedAt < :cutoff AND a.retentionStatus = 'ACTIVE'")
    long countUnexportedOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.isExported = true AND a.exportedAt < :cutoff AND a.deletionPending = false AND a.retentionStatus = 'ACTIVE'")
    long countExportedOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a FROM AlertLog a WHERE a.reportId = :reportId")
    List<AlertLog> findByReportId(@Param("reportId") String reportId);

    @Modifying
    @Transactional
    @Query("UPDATE AlertLog a SET a.retentionStatus = 'DELETED', a.deletedAt = :now WHERE a.id IN :ids")
    void markAsDeleted(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM AlertLog a WHERE a.id IN :ids AND a.retentionStatus = 'DELETED'")
    void hardDeleteByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("UPDATE AlertLog a SET a.deletionPending = true, a.deletionPendingAt = :now, a.scheduledDeleteAt = :deleteAt, a.retentionStatus = 'PENDING_DELETE' WHERE a.id IN :ids")
    void markForDeletion(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now, @Param("deleteAt") LocalDateTime deleteAt);

    @Modifying
    @Transactional
    @Query("UPDATE AlertLog a SET a.isExported = true, a.exportedAt = :now, a.exportedBy = :by, a.reportId = :reportId WHERE a.id IN :ids")
    void markAsExported(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now, @Param("by") String by, @Param("reportId") String reportId);

    @Query("SELECT a FROM AlertLog a WHERE a.retentionStatus = 'ACTIVE' AND a.isExported = true")
    List<AlertLog> findActiveExportedAlerts();
}