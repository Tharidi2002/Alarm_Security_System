package com.bank.atm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_logs", indexes = {
    @Index(name = "idx_atm_id", columnList = "atm_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_received_at", columnList = "received_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atm_id")
    private AtmMachine atmMachine;

    @Column(name = "zone_number")
    @Builder.Default
    private Integer zoneNumber = 0;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "alert_type", nullable = false)
    @Builder.Default
    private String alertType = "UNKNOWN";

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "confidence_score")
    @Builder.Default
    private Double confidenceScore = 0.0;

    @Column(name = "received_at")
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertStatus status = AlertStatus.PENDING;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "severity")
    @Builder.Default
    private Integer severity = 1;

    @Column(name = "is_escalated")
    @Builder.Default
    private Boolean escalated = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void acknowledge(String user) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = user;
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve(String user, String notes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = user;
        this.resolutionNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public void escalate() {
        this.status = AlertStatus.ESCALATED;
        this.escalated = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void ignore() {
        this.status = AlertStatus.IGNORED;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}