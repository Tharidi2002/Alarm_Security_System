package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_registration_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegistrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", nullable = false)
    private String adminUsername;

    @Column(name = "registered_by", nullable = false)
    private String registeredBy;

    @Column(name = "registered_from_ip")
    private String registeredFromIp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}