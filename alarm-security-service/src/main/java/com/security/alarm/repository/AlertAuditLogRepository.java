package com.security.alarm.repository;

import com.security.alarm.entity.AlertAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertAuditLogRepository extends JpaRepository<AlertAuditLog, Long> {

    List<AlertAuditLog> findByCompanyIdOrderByPerformedAtDesc(Long companyId);

    List<AlertAuditLog> findByActionOrderByPerformedAtDesc(String action);

    @Query("SELECT a FROM AlertAuditLog a WHERE a.alertId = :alertId ORDER BY a.performedAt DESC")
    List<AlertAuditLog> findByAlertId(@Param("alertId") Long alertId);

    List<AlertAuditLog> findAllByOrderByPerformedAtDesc();
}