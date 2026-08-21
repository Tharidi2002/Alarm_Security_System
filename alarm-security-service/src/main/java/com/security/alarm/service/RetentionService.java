package com.security.alarm.service;

import com.security.alarm.entity.*;
import com.security.alarm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RetentionService {

    private static final Logger logger = LoggerFactory.getLogger(RetentionService.class);

    private final AlertLogRepository alertLogRepository;
    private final ReportLogRepository reportLogRepository;
    private final ReportAlertMappingRepository reportAlertMappingRepository;
    private final AlertAuditLogRepository alertAuditLogRepository;
    private final RetentionConfigRepository retentionConfigRepository;
    private final NotificationService notificationService;
    private final ReportService reportService;

    private static final int DEFAULT_RETENTION_DAYS = 90;
    private static final int DEFAULT_GRACE_PERIOD_DAYS = 5;

    public RetentionService(AlertLogRepository alertLogRepository,
                            ReportLogRepository reportLogRepository,
                            ReportAlertMappingRepository reportAlertMappingRepository,
                            AlertAuditLogRepository alertAuditLogRepository,
                            RetentionConfigRepository retentionConfigRepository,
                            NotificationService notificationService,
                            ReportService reportService) {
        this.alertLogRepository = alertLogRepository;
        this.reportLogRepository = reportLogRepository;
        this.reportAlertMappingRepository = reportAlertMappingRepository;
        this.alertAuditLogRepository = alertAuditLogRepository;
        this.retentionConfigRepository = retentionConfigRepository;
        this.notificationService = notificationService;
        this.reportService = reportService;
    }

    // ============================================================
    // GET CONFIG VALUES
    // ============================================================

    private int getRetentionDays() {
        try {
            return Integer.parseInt(retentionConfigRepository.findConfigValueByConfigKey("RETENTION_DAYS"));
        } catch (Exception e) {
            return DEFAULT_RETENTION_DAYS;
        }
    }

    private int getGracePeriodDays() {
        try {
            return Integer.parseInt(retentionConfigRepository.findConfigValueByConfigKey("GRACE_PERIOD_DAYS"));
        } catch (Exception e) {
            return DEFAULT_GRACE_PERIOD_DAYS;
        }
    }

    private boolean isAutoExportEnabled() {
        try {
            return Boolean.parseBoolean(retentionConfigRepository.findConfigValueByConfigKey("AUTO_EXPORT_ENABLED"));
        } catch (Exception e) {
            return true;
        }
    }

    // ============================================================
    // SCHEDULED JOB 1: 90-Day Check & Notification (12:00 AM)
    // ============================================================

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkRetentionAndNotify() {
        logger.info("🔄 Running 90-Day Retention Check at 12:00 AM");

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(getRetentionDays());
            LocalDateTime now = LocalDateTime.now();

            // ===== CHECK 1: UNEXPORTED ALERTS =====
            List<AlertLog> unexportedAlerts = alertLogRepository.findUnexportedOlderThan(cutoff);
            
            if (!unexportedAlerts.isEmpty()) {
                logger.info("⚠️ Found {} unexported alerts older than {} days", unexportedAlerts.size(), getRetentionDays());
                
                Map<String, Object> details = buildAlertDetails(unexportedAlerts);
                details.put("type", "UNEXPORTED");
                
                notificationService.createAdminNotifications(
                    NotificationService.TYPE_RETENTION_WARNING,
                    "⚠️ Alerts Expiring - No Report Generated",
                    buildUnexportedMessage(unexportedAlerts),
                    NotificationService.SEVERITY_WARNING,
                    null,
                    null,
                    "SYSTEM",
                    details
                );
            }

            // ===== CHECK 2: EXPORTED ALERTS (>90 days) =====
            List<AlertLog> exportedAlerts = alertLogRepository.findExportedOlderThan(cutoff);
            
            if (!exportedAlerts.isEmpty()) {
                logger.info("⚠️ Found {} exported alerts older than {} days - marking for deletion", exportedAlerts.size(), getRetentionDays());
                
                List<Long> alertIds = exportedAlerts.stream()
                    .map(AlertLog::getId)
                    .collect(Collectors.toList());
                
                if (!alertIds.isEmpty()) {
                    LocalDateTime deleteAt = now.plusDays(getGracePeriodDays());
                    
                    alertLogRepository.markForDeletion(alertIds, now, deleteAt);
                    
                    createAuditLog(alertIds, "PENDING_DELETE", "SYSTEM", 
                        "Marked for deletion with " + getGracePeriodDays() + " days grace period", true);
                    
                    Map<String, Object> details = buildAlertDetails(exportedAlerts);
                    details.put("type", "EXPORTED");
                    details.put("deleteDate", deleteAt);
                    details.put("gracePeriod", getGracePeriodDays());
                    
                    notificationService.createAdminNotifications(
                        NotificationService.TYPE_RETENTION_WARNING,
                        "⚠️ Alerts Scheduled for Deletion",
                        buildExportedMessage(exportedAlerts, deleteAt),
                        NotificationService.SEVERITY_CRITICAL,
                        null,
                        null,
                        "SYSTEM",
                        details
                    );
                }
            }

            logger.info("✅ Retention check completed");

        } catch (Exception e) {
            logger.error("❌ Error in retention check: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // SCHEDULED JOB 2: Auto-Export (1:00 AM)
    // ============================================================

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoExportAlerts() {
        if (!isAutoExportEnabled()) {
            logger.info("Auto-export is disabled");
            return;
        }

        logger.info("🔄 Running Auto-Export at 1:00 AM");

        try {
            LocalDateTime now = LocalDateTime.now();
            
            List<AlertLog> pendingAlerts = alertLogRepository.findPendingDeletion(now);
            
            List<AlertLog> toExport = pendingAlerts.stream()
                .filter(a -> a != null && !a.getIsExported())
                .collect(Collectors.toList());

            if (toExport.isEmpty()) {
                logger.info("✅ No alerts to auto-export");
                return;
            }

            logger.info("📊 Auto-exporting {} alerts", toExport.size());

            Map<Long, List<AlertLog>> byCompany = toExport.stream()
                .filter(a -> a.getAlarmSystem() != null && a.getAlarmSystem().getCompany() != null)
                .collect(Collectors.groupingBy(
                    a -> a.getAlarmSystem().getCompany().getId()
                ));

            String reportId = "AUTO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            for (Map.Entry<Long, List<AlertLog>> entry : byCompany.entrySet()) {
                Long companyId = entry.getKey();
                List<AlertLog> alerts = entry.getValue();
                
                try {
                    LocalDateTime from = alerts.stream()
                        .map(AlertLog::getReceivedAt)
                        .min(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());
                    LocalDateTime to = alerts.stream()
                        .map(AlertLog::getReceivedAt)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

                    String reportIdWithCompany = reportId + "-" + companyId;
                    
                    // Mark as exported
                    List<Long> alertIds = alerts.stream()
                        .map(AlertLog::getId)
                        .collect(Collectors.toList());
                    
                    if (!alertIds.isEmpty()) {
                        alertLogRepository.markAsExported(alertIds, now, "SYSTEM", reportIdWithCompany);
                        
                        for (Long alertId : alertIds) {
                            ReportAlertMapping mapping = new ReportAlertMapping();
                            mapping.setReportId(reportIdWithCompany);
                            mapping.setAlertId(alertId);
                            reportAlertMappingRepository.save(mapping);
                        }

                        createAuditLog(alertIds, "AUTO_EXPORTED", "SYSTEM", 
                            "Auto-exported " + alerts.size() + " alerts before deletion", true);

                        logger.info("✅ Auto-exported {} alerts for company {}", alerts.size(), companyId);
                    }

                } catch (Exception e) {
                    logger.error("❌ Failed to auto-export for company {}: {}", companyId, e.getMessage(), e);
                }
            }

            notificationService.createAdminNotifications(
                NotificationService.TYPE_AUTO_EXPORTED,
                "✅ Alerts Auto-Exported & Archived",
                "System has auto-exported " + toExport.size() + " alerts and saved them to archives.",
                NotificationService.SEVERITY_INFO,
                null,
                null,
                "SYSTEM",
                Map.of("count", toExport.size(), "reportId", reportId)
            );

        } catch (Exception e) {
            logger.error("❌ Error in auto-export: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // SCHEDULED JOB 3: Auto-Delete (2:00 AM)
    // ============================================================

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoDeleteAlerts() {
        logger.info("🔄 Running Auto-Delete at 2:00 AM");

        try {
            LocalDateTime now = LocalDateTime.now();
            
            List<AlertLog> toDelete = alertLogRepository.findReadyForDeletion(now);

            if (toDelete.isEmpty()) {
                logger.info("✅ No alerts to delete");
                return;
            }

            logger.info("🗑️ Deleting {} alerts", toDelete.size());

            List<Long> alertIds = toDelete.stream()
                .map(AlertLog::getId)
                .collect(Collectors.toList());

            // Archive before deletion
            for (AlertLog alert : toDelete) {
                try {
                    createAuditLog(List.of(alert.getId()), "DELETED", "SYSTEM", 
                        "Alert deleted by system. Alert ID: " + alert.getId() + 
                        ", System: " + (alert.getAlarmSystem() != null ? alert.getAlarmSystem().getSystemCode() : "UNKNOWN"),
                        true);
                } catch (Exception e) {
                    logger.error("Failed to archive alert {}: {}", alert.getId(), e.getMessage());
                }
            }

            if (!alertIds.isEmpty()) {
                alertLogRepository.markAsDeleted(alertIds, now);
                
                // Hard delete after marking
                alertLogRepository.hardDeleteByIds(alertIds);
            }

            Map<String, Object> details = new HashMap<>();
            details.put("count", toDelete.size());

            notificationService.createAdminNotifications(
                NotificationService.TYPE_DELETION_COMPLETED,
                "🗑️ Alert Deletion Completed",
                "System has permanently deleted " + toDelete.size() + " alerts as per retention policy.",
                NotificationService.SEVERITY_INFO,
                null,
                null,
                "SYSTEM",
                details
            );

            logger.info("✅ Deleted {} alerts", toDelete.size());

        } catch (Exception e) {
            logger.error("❌ Error in auto-delete: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // SCHEDULED JOB 4: Archive Cleanup (3:00 AM)
    // ============================================================

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupArchives() {
        logger.info("🔄 Running Archive Cleanup at 3:00 AM");

        try {
            int retentionMonths = 6;
            try {
                retentionMonths = Integer.parseInt(retentionConfigRepository.findConfigValueByConfigKey("ARCHIVE_RETENTION_MONTHS"));
            } catch (Exception e) {
                retentionMonths = 6;
            }

            LocalDateTime cutoff = LocalDateTime.now().minusMonths(retentionMonths);
            
            List<ReportLog> oldReports = reportLogRepository.findAutoGeneratedOlderThan(cutoff);
            
            if (oldReports.isEmpty()) {
                logger.info("✅ No old archives to cleanup");
                return;
            }

            logger.info("🗑️ Cleaning up {} old archives (older than {} months)", oldReports.size(), retentionMonths);

            for (ReportLog report : oldReports) {
                report.setStatus("DELETED");
                reportLogRepository.save(report);
                logger.info("Archived report {} marked for deletion", report.getReportId());
            }

            logger.info("✅ Archive cleanup completed");

        } catch (Exception e) {
            logger.error("❌ Error in archive cleanup: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private void createAuditLog(List<Long> alertIds, String action, String performedBy, String details, boolean systemAction) {
        if (alertIds == null || alertIds.isEmpty()) {
            return;
        }
        
        AlertAuditLog auditLog = new AlertAuditLog();
        auditLog.setAlertId(alertIds.get(0));
        auditLog.setAction(action);
        auditLog.setPerformedBy(performedBy);
        auditLog.setDetails(details);
        auditLog.setSystemAction(systemAction);
        auditLog.setAlertCount(alertIds.size());
        alertAuditLogRepository.save(auditLog);
    }

    private Map<String, Object> buildAlertDetails(List<AlertLog> alerts) {
        Map<String, Object> details = new HashMap<>();
        details.put("count", alerts.size());
        
        Map<String, Long> bySystem = alerts.stream()
            .filter(a -> a.getAlarmSystem() != null)
            .collect(Collectors.groupingBy(
                a -> a.getAlarmSystem().getSystemCode(),
                Collectors.counting()
            ));
        details.put("bySystem", bySystem);
        
        return details;
    }

    private String buildUnexportedMessage(List<AlertLog> alerts) {
        StringBuilder msg = new StringBuilder();
        msg.append("You have ").append(alerts.size()).append(" alerts older than ")
           .append(getRetentionDays()).append(" days that were never exported to a report.\n\n");
        
        if (!alerts.isEmpty()) {
            msg.append("📋 Details:\n");
            alerts.stream()
                .filter(a -> a.getAlarmSystem() != null)
                .limit(10)
                .forEach(a -> msg.append("  • ").append(a.getAlarmSystem().getSystemCode())
                    .append(" - ").append(a.getReceivedAt().toLocalDate()).append("\n"));
            if (alerts.size() > 10) {
                msg.append("  • ... and ").append(alerts.size() - 10).append(" more\n");
            }
        }
        
        msg.append("\n⏰ These alerts will be AUTO-DELETED in ")
           .append(getGracePeriodDays()).append(" days.\n");
        msg.append("📌 Generate a report to keep them, or ignore - they will be auto-exported.");
        
        return msg.toString();
    }

    private String buildExportedMessage(List<AlertLog> alerts, LocalDateTime deleteAt) {
        StringBuilder msg = new StringBuilder();
        msg.append("The following ").append(alerts.size()).append(" alerts were exported and are now ")
           .append(getRetentionDays()).append("+ days old.\n\n");
        
        msg.append("📋 Details:\n");
        alerts.stream()
            .filter(a -> a.getAlarmSystem() != null)
            .limit(10)
            .forEach(a -> msg.append("  • ").append(a.getAlarmSystem().getSystemCode())
                .append(" - Exported on: ").append(a.getExportedAt().toLocalDate()).append("\n"));
        if (alerts.size() > 10) {
            msg.append("  • ... and ").append(alerts.size() - 10).append(" more\n");
        }
        
        msg.append("\n⏰ These alerts will be DELETED on: ")
           .append(deleteAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        msg.append("📌 Generate a NEW report to keep them.");
        
        return msg.toString();
    }

    // ============================================================
    // MANUAL METHODS
    // ============================================================

    @Transactional
    public void postponeDeletion(Long alertId, String username) {
        AlertLog alert = alertLogRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        
        alert.setDeletionPending(false);
        alert.setDeletionPendingAt(null);
        alert.setScheduledDeleteAt(null);
        alert.setRetentionStatus("ACTIVE");
        alertLogRepository.save(alert);
        
        createAuditLog(List.of(alertId), "REACTIVATED", username, "User postponed deletion", false);
    }

    public List<AlertLog> getPendingDeletionAlerts(String username) {
        // If username is provided, filter by company
        // For now, return all
        return alertLogRepository.findPendingDeletion(LocalDateTime.now());
    }

    public long getPendingDeletionCount() {
        return alertLogRepository.findPendingDeletion(LocalDateTime.now()).size();
    }

    public Map<String, Object> getRetentionStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(getRetentionDays());
        
        stats.put("unexportedOlderThan90", alertLogRepository.countUnexportedOlderThan(cutoff));
        stats.put("exportedOlderThan90", alertLogRepository.countExportedOlderThan(cutoff));
        stats.put("pendingDeletion", getPendingDeletionCount());
        stats.put("retentionDays", getRetentionDays());
        stats.put("gracePeriodDays", getGracePeriodDays());
        stats.put("autoExportEnabled", isAutoExportEnabled());
        
        return stats;
    }
}