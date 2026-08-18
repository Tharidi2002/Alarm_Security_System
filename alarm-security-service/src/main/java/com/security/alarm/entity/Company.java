package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", unique = true, nullable = false, length = 50)
    private String companyCode;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ============================================================
    // INACTIVE/ACTIVE STATUS CHANGE FIELDS
    // ============================================================
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

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

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<AlarmSystem> alarmSystems;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<User> users;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}