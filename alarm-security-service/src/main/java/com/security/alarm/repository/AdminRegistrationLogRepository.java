package com.security.alarm.repository;

import com.security.alarm.entity.AdminRegistrationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminRegistrationLogRepository extends JpaRepository<AdminRegistrationLog, Long> {
    List<AdminRegistrationLog> findAllByOrderByCreatedAtDesc();
    long countByAdminUsername(String adminUsername);
}