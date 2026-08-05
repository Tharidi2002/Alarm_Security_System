package com.security.alarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.alarm.entity.*;
import com.security.alarm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveService.class);

    private final SystemArchiveRepository systemArchiveRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlarmZoneRepository alarmZoneRepository;
    private final AlertLogRepository alertLogRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    public ArchiveService(SystemArchiveRepository systemArchiveRepository,
                          AlarmSystemRepository alarmSystemRepository,
                          AlarmZoneRepository alarmZoneRepository,
                          AlertLogRepository alertLogRepository,
                          CompanyRepository companyRepository,
                          ObjectMapper objectMapper) {
        this.systemArchiveRepository = systemArchiveRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alarmZoneRepository = alarmZoneRepository;
        this.alertLogRepository = alertLogRepository;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // ARCHIVE SYSTEM BEFORE DELETE
    // ============================================================
    @Transactional
    public SystemArchive archiveSystem(Long systemId, String deletedBy) {
        logger.info("Archiving system {} by {}", systemId, deletedBy);

        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
        if (systemOpt.isEmpty()) {
            throw new IllegalArgumentException("System not found: " + systemId);
        }

        AlarmSystem system = systemOpt.get();

        // 1. Get all related data
        List<AlarmZone> zones = alarmZoneRepository.findByAlarmSystemIdOrderByZoneNumberAsc(systemId);
        List<AlertLog> alerts = alertLogRepository.findByAlarmSystemIdOrderByReceivedAtDesc(systemId);

        // 2. Build archive data JSON
        Map<String, Object> archiveData = new HashMap<>();
        archiveData.put("system", Map.of(
            "systemCode", system.getSystemCode(),
            "location", system.getLocation(),
            "description", system.getDescription(),
            "simNumber", system.getSimNumber(),
            "panelSimNumber", system.getPanelSimNumber(),
            "sirenStatus", system.getSirenStatus(),
            "status", system.getStatus()
        ));
        archiveData.put("zones", zones);
        archiveData.put("alerts", alerts);

        String archiveDataJson;
        try {
            archiveDataJson = objectMapper.writeValueAsString(archiveData);
        } catch (Exception e) {
            logger.error("Failed to serialize archive data", e);
            archiveDataJson = "{}";
        }

        // 3. Create archive record
        SystemArchive archive = new SystemArchive();
        archive.setSystemId(systemId);
        archive.setSystemCode(system.getSystemCode());
        archive.setLocation(system.getLocation());
        archive.setSimNumber(system.getSimNumber());
        archive.setPanelSimNumber(system.getPanelSimNumber());
        archive.setAlertCount(alerts.size());
        archive.setZoneCount(zones.size());
        archive.setArchiveData(archiveDataJson);
        archive.setArchivedBy(deletedBy);
        archive.setDeletedBy(deletedBy);
        archive.setDeletedAt(LocalDateTime.now());
        archive.setRetentionUntil(LocalDateTime.now().plusMonths(6)); // 6 months retention

        // Set company info
        if (system.getCompany() != null) {
            archive.setCompanyId(system.getCompany().getId());
            archive.setCompanyName(system.getCompany().getCompanyName());
        }

        SystemArchive saved = systemArchiveRepository.save(archive);
        logger.info("System archived with ID: {}", saved.getId());

        return saved;
    }

    // ============================================================
    // SOFT DELETE SYSTEM
    // ============================================================
    @Transactional
    public void softDeleteSystem(Long systemId, String deletedBy) {
        logger.info("Soft deleting system {} by {}", systemId, deletedBy);

        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
        if (systemOpt.isEmpty()) {
            throw new IllegalArgumentException("System not found: " + systemId);
        }

        AlarmSystem system = systemOpt.get();
        system.setDeleted(true);
        system.setDeletedAt(LocalDateTime.now());
        system.setDeletedBy(deletedBy);
        system.setStatus("DELETED");

        alarmSystemRepository.save(system);
        logger.info("System {} marked as deleted", system.getSystemCode());
    }

    // ============================================================
    // ARCHIVE AND DELETE (COMPLETE)
    // ============================================================
    @Transactional
    public SystemArchive archiveAndDeleteSystem(Long systemId, String deletedBy) {
        logger.info("Archive and delete system {} by {}", systemId, deletedBy);

        // 1. Archive the system
        SystemArchive archive = archiveSystem(systemId, deletedBy);

        // 2. Delete zones (foreign key constraint)
        alarmZoneRepository.deleteBySystemId(systemId);

        // 3. Delete alerts
        alertLogRepository.deleteByAlarmSystemId(systemId);

        // 4. Soft delete the system (instead of hard delete)
        softDeleteSystem(systemId, deletedBy);

        logger.info("System {} archived and deleted", systemId);
        return archive;
    }

    // ============================================================
    // GET ARCHIVE
    // ============================================================
    public Optional<SystemArchive> getArchive(Long archiveId) {
        return systemArchiveRepository.findById(archiveId);
    }

    public List<SystemArchive> getAllArchives() {
        return systemArchiveRepository.findAllByOrderByArchivedAtDesc();
    }

    public List<SystemArchive> getArchivesByCompany(Long companyId) {
        return systemArchiveRepository.findByCompanyIdOrderByArchivedAtDesc(companyId);
    }

    // ============================================================
    // AUTO DELETE EXPIRED ARCHIVES (CALL BY SCHEDULER)
    // ============================================================
    @Transactional
    public int deleteExpiredArchives() {
        LocalDateTime cutoff = LocalDateTime.now();
        List<SystemArchive> expired = systemArchiveRepository.findExpiredArchives(cutoff);

        int deletedCount = 0;
        for (SystemArchive archive : expired) {
            try {
                // Update status
                archive.setStatus("DELETED_PERMANENTLY");
                systemArchiveRepository.save(archive);

                // Actually delete the archive record if needed
                // systemArchiveRepository.delete(archive);
                deletedCount++;
            } catch (Exception e) {
                logger.error("Failed to delete expired archive: {}", archive.getId(), e);
            }
        }

        logger.info("Deleted {} expired archives", deletedCount);
        return deletedCount;
    }

    // ============================================================
    // GENERATE ARCHIVE REPORT DATA
    // ============================================================
    public Map<String, Object> getArchiveReportData(Long archiveId) {
        Optional<SystemArchive> archiveOpt = systemArchiveRepository.findById(archiveId);
        if (archiveOpt.isEmpty()) {
            throw new IllegalArgumentException("Archive not found: " + archiveId);
        }

        SystemArchive archive = archiveOpt.get();

        Map<String, Object> report = new HashMap<>();
        report.put("archive", archive);
        report.put("archiveData", archive.getArchiveData());

        return report;
    }

    // ============================================================
    // CHECK IF SYSTEM CAN BE DELETED
    // ============================================================
    public DeletionCheckResult checkDeletionEligibility(Long systemId) {
        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
        if (systemOpt.isEmpty()) {
            return new DeletionCheckResult(false, "System not found");
        }

        AlarmSystem system = systemOpt.get();

        // Check if already deleted
        if (Boolean.TRUE.equals(system.getDeleted())) {
            return new DeletionCheckResult(false, "System is already deleted");
        }

        // Count related data
        long alertCount = alertLogRepository.countByAlarmSystemIdAndStatus(systemId, "PENDING");
        long totalAlerts = alertLogRepository.findByAlarmSystemId(systemId).size();
        long zoneCount = alarmZoneRepository.countByAlarmSystemId(systemId);

        Map<String, Long> dataSummary = Map.of(
            "pendingAlerts", alertCount,
            "totalAlerts", totalAlerts,
            "zones", zoneCount
        );

        boolean canDelete = true; // Always can delete, but with warnings
        String message = "System has " + totalAlerts + " alerts and " + zoneCount + " zones. They will be archived.";

        return new DeletionCheckResult(canDelete, message, dataSummary);
    }

    public static class DeletionCheckResult {
        private final boolean canDelete;
        private final String message;
        private final Map<String, Long> dataSummary;

        public DeletionCheckResult(boolean canDelete, String message) {
            this(canDelete, message, null);
        }

        public DeletionCheckResult(boolean canDelete, String message, Map<String, Long> dataSummary) {
            this.canDelete = canDelete;
            this.message = message;
            this.dataSummary = dataSummary;
        }

        public boolean isCanDelete() { return canDelete; }
        public String getMessage() { return message; }
        public Map<String, Long> getDataSummary() { return dataSummary; }
    }
}