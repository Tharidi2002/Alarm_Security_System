package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // EXPORTED, DELETED, PENDING_DELETE, AUTO_EXPORTED, REACTIVATED

    @Column(name = "performed_by", nullable = false, length = 50)
    private String performedBy;

    @Column(name = "performed_at")
    private LocalDateTime performedAt = LocalDateTime.now();

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "system_action")
    private Boolean systemAction = false;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "alert_count")
    private Integer alertCount;

    @Column(name = "archive_path", length = 500)
    private String archivePath;

    @Column(name = "report_id", length = 100)
    private String reportId;
}