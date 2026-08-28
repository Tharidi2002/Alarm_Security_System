package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_view_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "viewed_by", nullable = false, length = 50)
    private String viewedBy;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "viewed_from_ip", length = 50)
    private String viewedFromIp;

    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}