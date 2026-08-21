package com.security.alarm.repository;

import com.security.alarm.entity.ReportAlertMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportAlertMappingRepository extends JpaRepository<ReportAlertMapping, Long> {

    List<ReportAlertMapping> findByReportId(String reportId);

    @Query("SELECT m.alertId FROM ReportAlertMapping m WHERE m.reportId = :reportId")
    List<Long> findAlertIdsByReportId(@Param("reportId") String reportId);
}