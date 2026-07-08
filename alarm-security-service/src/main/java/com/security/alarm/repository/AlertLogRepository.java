package com.security.alarm.repository;

import com.security.alarm.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    
    List<AlertLog> findAllByOrderByReceivedAtDesc();
    
    List<AlertLog> findAllByAlarmSystemIdInOrderByReceivedAtDesc(List<Long> systemIds);
    
    // Get pending alerts count
    long countByStatus(String status);
    
    // Get resolved alerts count
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = 'RESOLVED'")
    long countResolved();
    
    // Get pending alerts for a system
    List<AlertLog> findByAlarmSystemIdAndStatusOrderByReceivedAtDesc(Long systemId, String status);
    
    // Get alert by ID with system details
    @Query("SELECT a FROM AlertLog a LEFT JOIN FETCH a.alarmSystem WHERE a.id = :id")
    AlertLog findByIdWithSystem(@Param("id") Long id);
}