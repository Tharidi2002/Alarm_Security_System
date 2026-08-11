package com.security.alarm.repository;

import com.security.alarm.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ============================================================
    // GET NOTIFICATIONS
    // ============================================================
    
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndTypeInOrderByCreatedAtDesc(Long userId, List<String> types, Pageable pageable);
    
    // ============================================================
    // COUNT METHODS
    // ============================================================
    
    long countByUserIdAndIsReadFalse(Long userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.severity = 'CRITICAL'")
    long countCriticalUnread(@Param("userId") Long userId);
    
    // ============================================================
    // FIND BY ID WITH USER
    // ============================================================
    
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.user.id = :userId")
    Optional<Notification> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    // ============================================================
    // BULK UPDATE - MARK AS READ
    // ============================================================
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id IN :ids AND n.user.id = :userId")
    int markAsReadByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
    
    // ============================================================
    // AUTO-DELETE EXPIRED
    // ============================================================
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
    
    // ============================================================
    // COMPANY-BASED
    // ============================================================
    
    List<Notification> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
    
    long countByCompanyIdAndIsReadFalse(Long companyId);
    
    // ============================================================
    // SYSTEM-BASED
    // ============================================================
    
    List<Notification> findBySystemIdOrderByCreatedAtDesc(Long systemId, Pageable pageable);
    
    // ============================================================
    // ARCHIVE
    // ============================================================
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isArchived = true WHERE n.createdAt < :cutoff")
    int archiveOldNotifications(@Param("cutoff") LocalDateTime cutoff);
}