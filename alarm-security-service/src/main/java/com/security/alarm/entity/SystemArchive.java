package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_archives")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    @Column(name = "system_code", nullable = false, length = 50)
    private String systemCode;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "sim_number", length = 20)
    private String simNumber;

    @Column(name = "panel_sim_number", length = 20)
    private String panelSimNumber;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "alert_count")
    private Integer alertCount = 0;

    @Column(name = "zone_count")
    private Integer zoneCount = 0;

    @Column(name = "archive_data", columnDefinition = "JSON")
    private String archiveData;  // JSON with all related data

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by", length = 50)
    private String archivedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;  // 6 months from deletion

    @Column(name = "status", length = 20)
    private String status = "ARCHIVED";  // ARCHIVED, DELETED_PERMANENTLY

    @PrePersist
    protected void onCreate() {
        archivedAt = LocalDateTime.now();
        // Retention: 6 months
        retentionUntil = LocalDateTime.now().plusMonths(6);
        status = "ARCHIVED";
    }
}