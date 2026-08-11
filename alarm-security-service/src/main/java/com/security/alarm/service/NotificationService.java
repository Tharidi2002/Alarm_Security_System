package com.security.alarm.service;

import com.security.alarm.entity.AlarmSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    // ============================================================
    // SEND AUTO-DELETE NOTIFICATION
    // ============================================================
    public void sendAutoDeleteNotification(List<AlarmSystem> expiringSystems) {
        if (expiringSystems.isEmpty()) {
            return;
        }

        String subject = "⚠️ System Deletion Notice - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        StringBuilder body = new StringBuilder();
        body.append("The following systems have been deleted for 30 days and will be AUTO-DELETED today at 11:59 PM:\n\n");
        
        for (AlarmSystem system : expiringSystems) {
            body.append("  • ").append(system.getSystemCode())
                .append(" (Deleted on: ").append(system.getDeletedAt() != null ? system.getDeletedAt().toString() : "Unknown")
                .append(")\n");
        }
        
        body.append("\n⚠️ To prevent deletion, restore the system or permanently delete it manually.\n");
        body.append("Action required by: ").append(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // Log notification
        logger.info("📧 NOTIFICATION: {}", subject);
        logger.info("📧 Body: \n{}", body);

        // TODO: Implement actual notification (Email, SMS, or System Alert)
        // For now, we'll just log it and create a system notification
        
        // Create system notification (to be displayed in UI)
        createSystemNotification(subject, body.toString(), expiringSystems.size());
    }

    // ============================================================
    // CREATE SYSTEM NOTIFICATION (For UI)
    // ============================================================
    private void createSystemNotification(String title, String message, int count) {
        // This will be stored in a database table for UI display
        // For now, we log it
        logger.info("🔔 SYSTEM ALERT: {} systems will be auto-deleted today", count);
        logger.info("   Title: {}", title);
        logger.info("   Message: {}", message);
        
        // TODO: Save to notification table
        // notificationRepository.save(new SystemNotification(title, message, "AUTO_DELETE", LocalDateTime.now()));
    }

    // ============================================================
    // SEND RESTORED NOTIFICATION (Optional)
    // ============================================================
    public void sendSystemRestoredNotification(AlarmSystem system) {
        String subject = "✅ System Restored - " + system.getSystemCode();
        String body = "System " + system.getSystemCode() + " has been restored by " + 
                      (system.getDeletedBy() != null ? system.getDeletedBy() : "Unknown") + ".\n" +
                      "Location: " + system.getLocation() + "\n" +
                      "Status: " + system.getStatus();
        
        logger.info("📧 RESTORE NOTIFICATION: {}", subject);
        logger.info("📧 Body: {}", body);
    }
}