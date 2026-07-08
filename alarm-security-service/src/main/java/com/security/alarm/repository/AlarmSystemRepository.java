package com.security.alarm.repository;

import com.security.alarm.entity.AlarmSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface AlarmSystemRepository extends JpaRepository<AlarmSystem, Long> {
    Optional<AlarmSystem> findBySimNumber(String simNumber);
    
    Optional<AlarmSystem> findBySystemCode(String systemCode);
    
    // Get the latest system code to generate next number
    @Query("SELECT a.systemCode FROM AlarmSystem a WHERE a.systemCode LIKE 'ALARM-Z8B-%' ORDER BY a.id DESC")
    Optional<String> findLatestSystemCode();
}