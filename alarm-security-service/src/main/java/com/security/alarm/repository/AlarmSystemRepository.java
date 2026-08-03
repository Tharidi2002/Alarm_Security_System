package com.security.alarm.repository;

import com.security.alarm.entity.AlarmSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AlarmSystemRepository extends JpaRepository<AlarmSystem, Long> {
    Optional<AlarmSystem> findBySimNumber(String simNumber);
    Optional<AlarmSystem> findBySystemCode(String systemCode);
    
    // ===== COMPANY METHODS =====
    List<AlarmSystem> findByCompanyId(Long companyId);
    List<AlarmSystem> findByCompanyIdAndStatus(Long companyId, String status);
    long countByCompanyId(Long companyId);
    
    @Query(value = "SELECT system_code FROM alarm_systems WHERE system_code LIKE 'ALARM-Z8B-%' ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLatestSystemCode();
}