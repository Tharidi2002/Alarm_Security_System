package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "system_id")
    private AlarmSystem alarmSystem;

    @Column(name = "zone_number")
    private Integer zoneNumber;

    @Column(name = "zone_numbers")
    private String zoneNumbers;

    @Column(name = "alert_type", nullable = false, length = 255)
    private String alertType;

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "received_at")
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(length = 20)
    private String status = "PENDING";

    // Resolve fields
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private String resolvedBy;
    
    @Column(name = "pending_duration_seconds")
    private Long pendingDurationSeconds;
    
    @Column(name = "resolution_description", columnDefinition = "TEXT")
    private String resolutionDescription;
    
    @Column(name = "resolved_from_ip")
    private String resolvedFromIp;

    // ===== NEW: Zone Names (Not stored in DB, computed at runtime) =====
    @Transient
    private String zoneNames;

    // ===== REPORT & ARCHIVE FIELDS =====
    @Column(name = "is_reported")
    private Boolean isReported = false;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_reason")
    private String archivedReason;

    @Column(name = "delete_notification_sent")
    private Boolean deleteNotificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;
}