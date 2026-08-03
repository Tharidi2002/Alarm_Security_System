package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "alarm_systems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AlarmSystem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_code", unique = true, nullable = false)
    private String systemCode;

    @Column(nullable = false)
    private String location;

    @Column(length = 1024)
    private String description;

    @Column(name = "sim_number", nullable = false)
    private String simNumber;

    private String status = "ACTIVE";

    @Column(name = "last_status_changed_at")
    private LocalDateTime lastStatusChangedAt = LocalDateTime.now();

    // ===== COMPANY RELATIONSHIP =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "alarmSystems", "users"})
    private Company company;

    // ===== Z8B PANEL FIELDS =====
    @Column(name = "panel_sim_number")
    private String panelSimNumber;

    @Column(name = "panel_password")
    private String panelPassword = "8888";

    @Column(name = "disarm_command")
    private String disarmCommand = "8888#2A";

    @Column(name = "arm_command")
    private String armCommand = "8888#1A";

    @Column(name = "siren_stop_command")
    private String sirenStopCommand = "8888#5A";

    @Column(name = "siren_status")
    private String sirenStatus = "OFF";
}