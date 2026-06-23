package com.bank.atm.service;

import com.bank.atm.entity.AlertLog;
import com.bank.atm.entity.AlertStatus;
import com.bank.atm.entity.AtmMachine;
import com.bank.atm.entity.AtmZone;
import com.bank.atm.repository.AlertLogRepository;
import com.bank.atm.repository.AtmMachineRepository;
import com.bank.atm.repository.AtmZoneRepository;
import com.bank.atm.util.Z8BSmsParser;
import com.bank.atm.util.Z8BSmsParser.ParsedAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class AlertService {

    private final AlertLogRepository alertLogRepository;
    private final AtmMachineRepository atmMachineRepository;
    private final AtmZoneRepository atmZoneRepository;
    private final Z8BSmsParser smsParser;

    public AlertService(AlertLogRepository alertLogRepository, 
                        AtmMachineRepository atmMachineRepository,
                        AtmZoneRepository atmZoneRepository,
                        Z8BSmsParser smsParser) {
        this.alertLogRepository = alertLogRepository;
        this.atmMachineRepository = atmMachineRepository;
        this.atmZoneRepository = atmZoneRepository;
        this.smsParser = smsParser;
    }

    @Transactional
    public AlertLog processIncomingSMS(String fromSimNumber, String smsContent) {
        log.info("Processing SMS from SIM: {}, Content: {}", fromSimNumber, smsContent);
        
        // 1. Parse the SMS
        ParsedAlert parsed = smsParser.parse(smsContent);
        
        // 2. Find ATM by SIM number
        Optional<AtmMachine> machineOpt = atmMachineRepository.findBySimNumber(fromSimNumber);
        
        // 3. Build AlertLog
        AlertLog alertLog = AlertLog.builder()
            .rawMessage(smsContent)
            .zoneNumber(parsed.getZoneNumber())
            .alertType(parsed.getAlertType())
            .confidenceScore(parsed.getConfidence())
            .receivedAt(LocalDateTime.now())
            .status(AlertStatus.PENDING)
            .severity(determineSeverity(parsed.getAlertType()))
            .build();
        
        if (machineOpt.isPresent()) {
            AtmMachine atm = machineOpt.get();
            alertLog.setAtmMachine(atm);
            atm.updateHeartbeat();
            atmMachineRepository.save(atm);
            
            // 4. Try to get zone name
            if (parsed.getZoneNumber() > 0) {
                Optional<AtmZone> zoneOpt = atmZoneRepository
                    .findByAtmMachineAndZoneNumber(atm, parsed.getZoneNumber());
                if (zoneOpt.isPresent()) {
                    alertLog.setZoneName(zoneOpt.get().getZoneName());
                } else if (parsed.getZoneName() != null && !parsed.getZoneName().isEmpty()) {
                    alertLog.setZoneName(parsed.getZoneName());
                } else {
                    alertLog.setZoneName("Zone " + parsed.getZoneNumber());
                }
            }
            
            // 5. Auto-escalate for critical alerts
            if (alertLog.getSeverity() >= 3) {
                alertLog.escalate();
            }
            
        } else {
            log.warn("ATM not found for SIM: {}", fromSimNumber);
            alertLog.setZoneName("Unknown ATM - SIM: " + fromSimNumber);
        }
        
        // 6. Save alert
        AlertLog saved = alertLogRepository.save(alertLog);
        log.info("Alert saved with ID: {}", saved.getId());
        
        return saved;
    }

    public List<AlertLog> getAllAlerts() {
        return alertLogRepository.findAllByOrderByReceivedAtDesc();
    }

    // getAlertsPaginated method එකට null check එකක් add කරන්න
    public Page<AlertLog> getAlertsPaginated(int page, int size, String status, Long atmId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        
        try {
            if (status != null && !status.isEmpty() && atmId != null) {
                AlertStatus alertStatus = AlertStatus.valueOf(status.toUpperCase());
                return alertLogRepository.findByStatusAndAtmMachineId(alertStatus, atmId, pageable);
            }
            
            if (status != null && !status.isEmpty()) {
                AlertStatus alertStatus = AlertStatus.valueOf(status.toUpperCase());
                return alertLogRepository.findByStatus(alertStatus, pageable);
            }
            
            if (atmId != null) {
                return alertLogRepository.findByAtmMachineId(atmId, pageable);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: {}", status);
            // Return empty page if status is invalid
            return Page.empty(pageable);
        }
        
        return alertLogRepository.findAll(pageable);
    }

    public AlertLog getAlertById(Long id) {
        return alertLogRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
    }

    @Transactional
    public AlertLog acknowledgeAlert(Long alertId, String username) {
        AlertLog alert = alertLogRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        alert.acknowledge(username);
        log.info("Alert {} acknowledged by {}", alertId, username);
        
        return alertLogRepository.save(alert);
    }

    @Transactional
    public AlertLog resolveAlert(Long alertId, String username, String notes) {
        AlertLog alert = alertLogRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        alert.resolve(username, notes);
        log.info("Alert {} resolved by {} with notes: {}", alertId, username, notes);
        
        return alertLogRepository.save(alert);
    }

    @Transactional
    public AlertLog ignoreAlert(Long alertId) {
        AlertLog alert = alertLogRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        alert.ignore();
        log.info("Alert {} ignored", alertId);
        
        return alertLogRepository.save(alert);
    }

    private Integer determineSeverity(String alertType) {
        if (alertType == null) return 1;
        
        String type = alertType.toUpperCase();
        
        if (type.contains("SOS") || type.contains("PANIC") || type.contains("EMERGENCY")) {
            return 4;
        }
        
        if (type.contains("FIRE") || type.contains("SMOKE") || type.contains("BURGLARY") || 
            type.contains("INTRUSION") || type.contains("TAMPER") || type.contains("24HOUR")) {
            return 3;
        }
        
        if (type.contains("POWER_FAILURE") || type.contains("DOOR") || type.contains("WINDOW") ||
            type.contains("MOTION") || type.contains("PERIMETER")) {
            return 2;
        }
        
        return 1;
    }

    public long getPendingAlertsCount() {
        return alertLogRepository.countByStatus(AlertStatus.PENDING);
    }

    public long getUnresolvedAlertsCount() {
        return alertLogRepository.countByStatusIn(List.of(AlertStatus.PENDING, AlertStatus.ACKNOWLEDGED));
    }
}