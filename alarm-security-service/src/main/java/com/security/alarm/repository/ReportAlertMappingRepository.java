package com.security.alarm.repository;

import com.security.alarm.entity.ReportAlertMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportAlertMappingRepository extends JpaRepository<ReportAlertMapping, Long> {
}
