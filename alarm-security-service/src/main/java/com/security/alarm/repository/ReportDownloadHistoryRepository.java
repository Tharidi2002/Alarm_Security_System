package com.security.alarm.repository;

import com.security.alarm.entity.ReportDownloadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportDownloadHistoryRepository extends JpaRepository<ReportDownloadHistory, Long> {

    List<ReportDownloadHistory> findByReportIdOrderByDownloadedAtDesc(Long reportId);
    
    List<ReportDownloadHistory> findByDownloadedByOrderByDownloadedAtDesc(String downloadedBy);
    
    @Query("SELECT COUNT(d) FROM ReportDownloadHistory d WHERE d.reportId = :reportId")
    long countByReportId(@Param("reportId") Long reportId);
    
    @Query("SELECT COUNT(d) FROM ReportDownloadHistory d WHERE d.downloadedBy = :username")
    long countByDownloadedBy(@Param("username") String username);
}