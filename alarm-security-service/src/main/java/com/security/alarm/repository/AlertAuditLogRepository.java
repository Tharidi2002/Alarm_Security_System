package com.security.alarm.repository;

import com.security.alarm.entity.AlertAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertAuditLogRepository extends JpaRepository<AlertAuditLog, Long> {
}
