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
    private String status = "PENDING"; // PENDING, RESOLVED, ARMED

    // ========== NEW FIELDS FOR RESOLVE FEATURE ==========
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private String resolvedBy; // username who resolved
    
    @Column(name = "pending_duration_seconds")
    private Long pendingDurationSeconds; // how long it was pending
    
    @Column(name = "resolution_description", columnDefinition = "TEXT")
    private String resolutionDescription; // optional description
    
    @Column(name = "resolved_from_ip")
    private String resolvedFromIp; // IP address of resolver
}