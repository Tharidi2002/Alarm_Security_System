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
}