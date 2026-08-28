package com.security.alarm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_download_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDownloadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "downloaded_by", nullable = false, length = 50)
    private String downloadedBy;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "downloaded_from_ip", length = 50)
    private String downloadedFromIp;

    @Column(name = "file_format", length = 10)
    private String fileFormat;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "download_status", length = 20)
    private String downloadStatus;

    @PrePersist
    protected void onCreate() {
        downloadedAt = LocalDateTime.now();
        if (downloadStatus == null) downloadStatus = "SUCCESS";
    }
}