package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    // ===== COMPANY RELATIONSHIP =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "users"})
    private Company company;

    // ============================================================
    // ADMIN ACCESS CONTROL FIELDS
    // ============================================================
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_super_admin")
    private Boolean isSuperAdmin = false;
    
    @Column(name = "registration_method")
    private String registrationMethod; // "FORM" or "ADMIN_PANEL"

    // ============================================================
    // NEW: INACTIVE DETAILS FIELDS
    // ============================================================
    
    @Column(name = "inactivated_at")
    private LocalDateTime inactivatedAt;
    
    @Column(name = "inactivated_by")
    private String inactivatedBy;
    
    @Column(name = "inactivation_reason")
    private String inactivationReason;
    
    @Column(name = "inactivation_description")
    private String inactivationDescription;
    
    @Column(name = "reactivated_at")
    private LocalDateTime reactivatedAt;
    
    @Column(name = "reactivated_by")
    private String reactivatedBy;
}