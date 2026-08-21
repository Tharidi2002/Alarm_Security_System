package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_alert_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportAlertMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false, length = 100)
    private String reportId;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Column(name = "marked_at")
    private LocalDateTime markedAt = LocalDateTime.now();
}