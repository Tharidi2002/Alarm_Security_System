package com.security.alarm.service;

import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.SystemArchive;
import com.security.alarm.entity.AlarmZone;
import com.security.alarm.entity.AlertLog;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.AlarmZoneRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.repository.SystemArchiveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    private static final int RETENTION_DAYS = 30;

    private final AlarmSystemRepository alarmSystemRepository;
    private final AlarmZoneRepository alarmZoneRepository;
    private final AlertLogRepository alertLogRepository;
    private final UserSystemRepository userSystemRepository;
    private final SystemArchiveRepository systemArchiveRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ScheduledTaskService(AlarmSystemRepository alarmSystemRepository,
                                AlarmZoneRepository alarmZoneRepository,
                                AlertLogRepository alertLogRepository,
                                UserSystemRepository userSystemRepository,
                                SystemArchiveRepository systemArchiveRepository,
                                NotificationService notificationService,
                                ObjectMapper objectMapper) {
        this.alarmSystemRepository = alarmSystemRepository;
        this.alarmZoneRepository = alarmZoneRepository;
        this.alertLogRepository = alertLogRepository;
        this.userSystemRepository = userSystemRepository;
        this.systemArchiveRepository = systemArchiveRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // RUN EVERY DAY AT 3:00 AM - SEND NOTIFICATION
    // ============================================================
    @Scheduled(cron = "0 0 3 * * ?")
    public void sendDeletionNotification() {
        logger.info("⏰ Running deletion notification check at 3:00 AM");
        
        try {
            // Get systems that will expire today (deleted_at + RETENTION_DAYS = today)
            LocalDateTime today = LocalDateTime.now();
            LocalDateTime expiryDate = today.minusDays(RETENTION_DAYS);
            
            // Get systems that expired today (deleted_at is exactly RETENTION_DAYS ago)
            List<AlarmSystem> expiringToday = alarmSystemRepository.findDeletedOnDate(expiryDate);
            
            if (!expiringToday.isEmpty()) {
                logger.info("📧 Found {} systems expiring today", expiringToday.size());
                notificationService.sendAutoDeleteNotification(expiringToday);
            } else {
                logger.info("✅ No systems expiring today");
            }
            
        } catch (Exception e) {
            logger.error("❌ Error in deletion notification: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // RUN EVERY DAY AT 11:59 PM - AUTO DELETE EXPIRED SYSTEMS
    // ============================================================
    @Scheduled(cron = "0 59 23 * * ?")
    @Transactional
    public void autoDeleteExpiredSystems() {
        logger.info("🗑️ Running auto-delete at 11:59 PM");
        
        try {
            // Get systems that have been deleted for more than RETENTION_DAYS
            LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
            List<AlarmSystem> expiredSystems = alarmSystemRepository.findDeletedBefore(cutoff);
            
            if (expiredSystems.isEmpty()) {
                logger.info("✅ No expired systems to delete");
                return;
            }
            
            logger.info("🗑️ Found {} expired systems to permanently delete", expiredSystems.size());
            
            int deletedCount = 0;
            for (AlarmSystem system : expiredSystems) {
                try {
                    // 1. Archive the system first
                    archiveSystem(system);
                    
                    // 2. Delete related data
                    alarmZoneRepository.deleteBySystemId(system.getId());
                    alertLogRepository.deleteByAlarmSystemId(system.getId());
                    userSystemRepository.deleteBySystemId(system.getId());
                    
                    // 3. Delete the system
                    alarmSystemRepository.deleteById(system.getId());
                    
                    deletedCount++;
                    logger.info("✅ Auto-deleted system: {} (ID: {})", system.getSystemCode(), system.getId());
                    
                } catch (Exception e) {
                    logger.error("❌ Failed to auto-delete system {}: {}", system.getSystemCode(), e.getMessage(), e);
                }
            }
            
            logger.info("✅ Auto-delete completed. Deleted {} systems.", deletedCount);
            
        } catch (Exception e) {
            logger.error("❌ Error in auto-delete: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // ARCHIVE SYSTEM BEFORE DELETION
    // ============================================================
    private void archiveSystem(AlarmSystem system) {
        try {
            // Get related data
            List<AlarmZone> zones = alarmZoneRepository
                .findByAlarmSystemIdOrderByZoneNumberAsc(system.getId());
            List<AlertLog> alerts = alertLogRepository
                .findByAlarmSystemIdOrderByReceivedAtDesc(system.getId());

            // Build archive data JSON
            Map<String, Object> archiveData = new HashMap<>();
            archiveData.put("system", Map.of(
                "systemCode", system.getSystemCode(),
                "location", system.getLocation(),
                "description", system.getDescription(),
                "simNumber", system.getSimNumber(),
                "panelSimNumber", system.getPanelSimNumber(),
                "sirenStatus", system.getSirenStatus(),
                "status", system.getStatus()
            ));
            archiveData.put("zones", zones);
            archiveData.put("alerts", alerts);

            String archiveDataJson = objectMapper.writeValueAsString(archiveData);

            // Create archive record
            SystemArchive archive = new SystemArchive();
            archive.setSystemId(system.getId());
            archive.setSystemCode(system.getSystemCode());
            archive.setLocation(system.getLocation());
            archive.setSimNumber(system.getSimNumber());
            archive.setPanelSimNumber(system.getPanelSimNumber());
            archive.setAlertCount(alerts.size());
            archive.setZoneCount(zones.size());
            archive.setArchiveData(archiveDataJson);
            archive.setArchivedBy("SYSTEM_AUTO_DELETE");
            archive.setDeletedBy("SYSTEM_AUTO_DELETE");
            archive.setDeletedAt(LocalDateTime.now());
            archive.setRetentionUntil(LocalDateTime.now().plusMonths(6));

            if (system.getCompany() != null) {
                archive.setCompanyId(system.getCompany().getId());
                archive.setCompanyName(system.getCompany().getCompanyName());
            }

            systemArchiveRepository.save(archive);
            logger.info("📦 Archived system {} before auto-delete", system.getSystemCode());
            
        } catch (Exception e) {
            logger.error("❌ Failed to archive system {}: {}", system.getSystemCode(), e.getMessage(), e);
        }
    }

    // ============================================================
    // GET SYSTEMS EXPIRING SOON (For dashboard)
    // ============================================================
    public List<AlarmSystem> getExpiringSystems() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime expiryDate = today.minusDays(RETENTION_DAYS);
        return alarmSystemRepository.findDeletedOnDate(expiryDate);
    }

    public long getExpiringCount() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime expiryDate = today.minusDays(RETENTION_DAYS);
        return alarmSystemRepository.countDeletedOnDate(expiryDate);
    }
}