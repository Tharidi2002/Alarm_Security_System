package com.bank.atm.repository;

import com.bank.atm.entity.AlertLog;
import com.bank.atm.entity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    
    List<AlertLog> findAllByOrderByReceivedAtDesc();
    
    Page<AlertLog> findAll(Pageable pageable);
    
    Page<AlertLog> findByStatus(AlertStatus status, Pageable pageable);
    
    Page<AlertLog> findByAtmMachineId(Long atmId, Pageable pageable);
    
    Page<AlertLog> findByStatusAndAtmMachineId(AlertStatus status, Long atmId, Pageable pageable);
    
    long countByStatus(AlertStatus status);
    
    long countByStatusIn(List<AlertStatus> statuses);
    
    @Query("SELECT a FROM AlertLog a WHERE a.receivedAt BETWEEN :start AND :end")
    List<AlertLog> findAlertsBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = :status AND a.atmMachine.id = :atmId")
    long countByStatusAndAtmId(@Param("status") AlertStatus status, @Param("atmId") Long atmId);
    
    @Query("SELECT a FROM AlertLog a WHERE a.alertType LIKE %:type%")
    List<AlertLog> findByAlertTypeContaining(@Param("type") String alertType);
}