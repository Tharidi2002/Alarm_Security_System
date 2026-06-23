package com.bank.atm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "atm_machines", indexes = {
    @Index(name = "idx_sim_number", columnList = "sim_number"),
    @Index(name = "idx_bank_id", columnList = "bank_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtmMachine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atm_code", unique = true, nullable = false, length = 50)
    private String atmCode;

    @Column(name = "atm_name", length = 100)
    private String atmName;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "sim_number", nullable = false, unique = true, length = 20)
    private String simNumber;

    @Column(name = "gsm_signal")
    @Builder.Default
    private Integer gsmSignal = 0;

    @Column(name = "battery_level")
    @Builder.Default
    private Integer batteryLevel = 100;

    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @OneToMany(mappedBy = "atmMachine", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AtmZone> zones = new ArrayList<>();

    @Column(name = "firmware_version")
    @Builder.Default
    private String firmwareVersion = "1.0.0";

    @Column(name = "installation_date")
    @Builder.Default
    private LocalDateTime installationDate = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void addZone(AtmZone zone) {
        zones.add(zone);
        zone.setAtmMachine(this);
    }

    public void removeZone(AtmZone zone) {
        zones.remove(zone);
        zone.setAtmMachine(null);
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    public boolean isOnline() {
        if (lastHeartbeat == null) return false;
        return LocalDateTime.now().minusMinutes(5).isBefore(lastHeartbeat);
    }
}