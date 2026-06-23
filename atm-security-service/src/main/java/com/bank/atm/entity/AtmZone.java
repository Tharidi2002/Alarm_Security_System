package com.bank.atm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_zones", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"atm_id", "zone_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtmZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atm_id", nullable = false)
    private AtmMachine atmMachine;

    @Column(name = "zone_number", nullable = false)
    private Integer zoneNumber;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "zone_type")
    @Builder.Default
    private String zoneType = "SECURITY";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}