package com.security.alarm.repository;

import com.security.alarm.entity.ReportViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportViewHistoryRepository extends JpaRepository<ReportViewHistory, Long> {

    List<ReportViewHistory> findByReportIdOrderByViewedAtDesc(Long reportId);
    
    List<ReportViewHistory> findByViewedByOrderByViewedAtDesc(String viewedBy);
    
    @Query("SELECT COUNT(v) FROM ReportViewHistory v WHERE v.reportId = :reportId")
    long countByReportId(@Param("reportId") Long reportId);
}