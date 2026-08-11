package com.security.alarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.alarm.entity.*;
import com.security.alarm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    // ============================================================
    // NOTIFICATION TYPES
    // ============================================================
    public static final String TYPE_NEW_ALERT = "NEW_ALERT";
    public static final String TYPE_ALERT_RESOLVED = "ALERT_RESOLVED";
    public static final String TYPE_SIREN_STOP = "SIREN_STOP";
    public static final String TYPE_SYSTEM_DISARM = "SYSTEM_DISARM";
    public static final String TYPE_SYSTEM_ARMED = "SYSTEM_ARMED";
    public static final String TYPE_SYSTEM_STATUS_CHANGE = "SYSTEM_STATUS_CHANGE";
    public static final String TYPE_SYSTEM_DELETED = "SYSTEM_DELETED";
    public static final String TYPE_SYSTEM_RESTORED = "SYSTEM_RESTORED";
    public static final String TYPE_USER_CREATED = "USER_CREATED";
    public static final String TYPE_USER_DELETED = "USER_DELETED";
    public static final String TYPE_HEARTBEAT_LOST = "HEARTBEAT_LOST";
    public static final String TYPE_HEARTBEAT_RESTORED = "HEARTBEAT_RESTORED";
    public static final String TYPE_ZONE_UPDATED = "ZONE_UPDATED";

    // ============================================================
    // SEVERITY LEVELS
    // ============================================================
    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               AlarmSystemRepository alarmSystemRepository,
                               CompanyRepository companyRepository,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // CREATE NOTIFICATION FOR SINGLE USER
    // ============================================================
    
    @Transactional
    public Notification createNotification(Long userId, String type, String title, String message,
                                           String severity, Long systemId, Long alertId,
                                           String actionBy, Map<String, Object> metadata) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setCompany(user.getCompany());
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setSeverity(severity != null ? severity : SEVERITY_INFO);
            notification.setActionBy(actionBy);

            if (systemId != null) {
                alarmSystemRepository.findById(systemId).ifPresent(notification::setSystem);
            }
            notification.setAlertId(alertId);

            if (metadata != null && !metadata.isEmpty()) {
                try {
                    notification.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    logger.warn("Failed to serialize metadata: {}", e.getMessage());
                    notification.setMetadata("{}");
                }
            }

            Notification saved = notificationRepository.save(notification);
            logger.info("📬 Notification created for user {}: {}", user.getUsername(), type);
            return saved;

        } catch (Exception e) {
            logger.error("Failed to create notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    // ============================================================
    // CREATE NOTIFICATION FOR ALL USERS IN COMPANY
    // ============================================================
    
    @Transactional
    public List<Notification> createCompanyNotifications(Long companyId, String type, String title, 
                                                         String message, String severity, 
                                                         Long systemId, Long alertId,
                                                         String actionBy, Map<String, Object> metadata) {
        List<User> users = userRepository.findByCompanyId(companyId);
        List<Notification> notifications = new ArrayList<>();

        for (User user : users) {
            try {
                Notification n = createNotification(
                    user.getId(), type, title, message, severity,
                    systemId, alertId, actionBy, metadata
                );
                notifications.add(n);
            } catch (Exception e) {
                logger.error("Failed to create notification for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        logger.info("📬 Created {} notifications for company {}", notifications.size(), companyId);
        return notifications;
    }

    // ============================================================
    // CREATE NOTIFICATION FOR ALL ADMINS
    // ============================================================
    
    @Transactional
    public List<Notification> createAdminNotifications(String type, String title, String message,
                                                       String severity, Long systemId, 
                                                       Long alertId, String actionBy,
                                                       Map<String, Object> metadata) {
        List<User> admins = userRepository.findAllAdmins();
        List<Notification> notifications = new ArrayList<>();

        for (User admin : admins) {
            try {
                Notification n = createNotification(
                    admin.getId(), type, title, message, severity,
                    systemId, alertId, actionBy, metadata
                );
                notifications.add(n);
            } catch (Exception e) {
                logger.error("Failed to create notification for admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        logger.info("📬 Created {} notifications for admins", notifications.size());
        return notifications;
    }

    // ============================================================
    // GET NOTIFICATIONS
    // ============================================================
    
    public List<Notification> getUserNotifications(Long userId, Boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<Notification> getUserNotificationsByType(Long userId, List<String> types, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(userId, types, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public long getCriticalUnreadCount(Long userId) {
        return notificationRepository.countCriticalUnread(userId);
    }

    public Optional<Notification> getNotification(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId);
    }

    // ============================================================
    // MARK AS READ
    // ============================================================
    
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public int markMultipleAsRead(List<Long> ids, Long userId) {
        return notificationRepository.markAsReadByIds(ids, userId);
    }

    // ============================================================
    // DELETE NOTIFICATION
    // ============================================================
    
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
        logger.info("🗑️ Deleted notification: {}", notificationId);
    }

    // ============================================================
    // AUTO-DELETE EXPIRED NOTIFICATIONS (SCHEDULED)
    // ============================================================
    
    @Scheduled(cron = "0 0 2 * * ?")  // Daily at 2:00 AM
    @Transactional
    public int deleteExpiredNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = notificationRepository.deleteByCreatedAtBefore(cutoff);
        logger.info("🗑️ Auto-deleted {} expired notifications (older than 7 days)", deleted);
        return deleted;
    }

    @Scheduled(cron = "0 0 3 * * ?")  // Daily at 3:00 AM
    @Transactional
    public int deleteByExpiresAt() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = notificationRepository.deleteExpired(now);
        logger.info("🗑️ Auto-deleted {} notifications by expires_at", deleted);
        return deleted;
    }

    // ============================================================
    // NOTIFICATION FOR SYSTEM RESTORED (for DeletedSystemsController)
    // ============================================================
    
    @Transactional
    public void sendSystemRestoredNotification(AlarmSystem system) {
        if (system == null) return;
        
        String title = "✅ System Restored - " + system.getSystemCode();
        String message = "System " + system.getSystemCode() + " has been restored by " + 
                         (system.getDeletedBy() != null ? system.getDeletedBy() : "Unknown") + ".\n" +
                         "Location: " + system.getLocation() + "\n" +
                         "Status: " + system.getStatus();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("systemId", system.getId());
        metadata.put("systemCode", system.getSystemCode());
        metadata.put("action", "RESTORE");
        
        // Notify all admins
        createAdminNotifications(
            TYPE_SYSTEM_RESTORED,
            title,
            message,
            SEVERITY_INFO,
            system.getId(),
            null,
            system.getDeletedBy(),
            metadata
        );
        
        // Notify company users
        if (system.getCompany() != null && system.getCompany().getId() != null) {
            createCompanyNotifications(
                system.getCompany().getId(),
                TYPE_SYSTEM_RESTORED,
                title,
                message,
                SEVERITY_INFO,
                system.getId(),
                null,
                system.getDeletedBy(),
                metadata
            );
        }
        
        logger.info("📬 Sent system restored notification for: {}", system.getSystemCode());
    }

    // ============================================================
    // NOTIFICATION FOR AUTO-DELETE (for ScheduledTaskService)
    // ============================================================
    
    public void sendAutoDeleteNotification(List<AlarmSystem> expiringSystems) {
        if (expiringSystems == null || expiringSystems.isEmpty()) {
            return;
        }

        String subject = "⚠️ System Deletion Notice - " + 
                         LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        StringBuilder body = new StringBuilder();
        body.append("The following systems have been deleted for 30 days and will be AUTO-DELETED today at 11:59 PM:\n\n");
        
        for (AlarmSystem system : expiringSystems) {
            body.append("  • ").append(system.getSystemCode())
                .append(" (Deleted on: ").append(system.getDeletedAt() != null ? system.getDeletedAt().toString() : "Unknown")
                .append(")\n");
        }
        
        body.append("\n⚠️ To prevent deletion, restore the system or permanently delete it manually.\n");
        body.append("Action required by: ").append(LocalDateTime.now().plusDays(1)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // Notify all admins
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("systems", expiringSystems.stream()
            .map(s -> s.getSystemCode())
            .collect(java.util.stream.Collectors.toList()));
        metadata.put("count", expiringSystems.size());

        createAdminNotifications(
            TYPE_SYSTEM_DELETED,
            subject,
            body.toString(),
            SEVERITY_WARNING,
            null,
            null,
            "SYSTEM_AUTO_DELETE",
            metadata
        );

        logger.info("📧 Sent auto-delete notification for {} systems", expiringSystems.size());
    }

    // ============================================================
    // HELPER: Build notification messages
    // ============================================================
    
    public String buildAlertMessage(AlertLog alert) {
        if (alert == null) return "New alert received";
        String systemCode = alert.getAlarmSystem() != null ? 
            alert.getAlarmSystem().getSystemCode() : "Unknown";
        String zoneInfo = alert.getZoneNames() != null ? 
            alert.getZoneNames() : "Zone " + alert.getZoneNumber();
        return String.format("🚨 %s - %s triggered on %s", 
            systemCode, zoneInfo, alert.getAlertType());
    }

    public String buildResolveMessage(AlertLog alert, String resolvedBy) {
        if (alert == null) return "Alert resolved";
        String systemCode = alert.getAlarmSystem() != null ? 
            alert.getAlarmSystem().getSystemCode() : "Unknown";
        return String.format("✅ Alert #%d on %s resolved by %s", 
            alert.getId(), systemCode, resolvedBy);
    }

    public String buildDisarmMessage(AlarmSystem system, String triggeredBy) {
        return String.format("🔓 System %s disarmed by %s", 
            system.getSystemCode(), triggeredBy != null ? triggeredBy : "system");
    }

    public String buildArmMessage(AlarmSystem system, String triggeredBy) {
        return String.format("🔐 System %s armed by %s", 
            system.getSystemCode(), triggeredBy != null ? triggeredBy : "system");
    }

    // ============================================================
    // Helper: Get metadata map
    // ============================================================
    
    public Map<String, Object> createMetadata(String key, Object value) {
        Map<String, Object> meta = new HashMap<>();
        meta.put(key, value);
        return meta;
    }

    public Map<String, Object> createMetadata(Map<String, Object> extra) {
        Map<String, Object> meta = new HashMap<>();
        if (extra != null) {
            meta.putAll(extra);
        }
        return meta;
    }
}