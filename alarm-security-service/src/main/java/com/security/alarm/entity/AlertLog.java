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

    // ===== Zone Names (Transient) =====
    @Transient
    private String zoneNames;

    // ============================================================
    // NEW: RETENTION FIELDS
    // ============================================================
    
    @Column(name = "is_exported")
    private Boolean isExported = false;

    @Column(name = "exported_at")
    private LocalDateTime exportedAt;

    @Column(name = "exported_by")
    private String exportedBy;

    @Column(name = "report_id")
    private String reportId;

    @Column(name = "deletion_pending")
    private Boolean deletionPending = false;

    @Column(name = "deletion_pending_at")
    private LocalDateTime deletionPendingAt;

    @Column(name = "scheduled_delete_at")
    private LocalDateTime scheduledDeleteAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "retention_status")
    private String retentionStatus = "ACTIVE"; // ACTIVE, PENDING_DELETE, DELETED
}