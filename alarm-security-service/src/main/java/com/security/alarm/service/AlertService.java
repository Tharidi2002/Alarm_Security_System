package com.security.alarm.service;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.AlarmZone;
import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.AlarmZoneRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertLogRepository alertLogRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlarmZoneRepository alarmZoneRepository;
    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final SmsService smsService;

    public AlertService(AlertLogRepository alertLogRepository, 
                        AlarmSystemRepository alarmSystemRepository,
                        AlarmZoneRepository alarmZoneRepository,
                        UserRepository userRepository,
                        UserSystemRepository userSystemRepository,
                        SmsService smsService) {
        this.alertLogRepository = alertLogRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alarmZoneRepository = alarmZoneRepository;
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.smsService = smsService;
    }

    // ============================================================
    // PROCESS INCOMING SMS
    // ============================================================
    public AlertLog processIncomingSMS(String fromSimNumber, String smsContent, String atmCode) {
        AlertLog alertLog = new AlertLog();
        alertLog.setReceivedAt(LocalDateTime.now());
        
        String cleanMessage = smsContent;
        if (fromSimNumber != null && !fromSimNumber.isEmpty()) {
            cleanMessage = cleanMessage.replace(fromSimNumber, "").trim();
        }

        Optional<AlarmSystem> machineOpt = findSystem(atmCode, fromSimNumber);

        // ============================================================
        // 1. SIREN_STOP - Send SMS to panel and stop siren
        // ============================================================
        if (cleanMessage != null && cleanMessage.toUpperCase().contains("SIREN_STOP")) {
            if (machineOpt.isPresent()) {
                AlarmSystem system = machineOpt.get();
                
                // Send SMS to panel to stop siren
                boolean smsSent = smsService.sendSirenStopCommand(
                    system.getPanelSimNumber(),
                    system.getPanelPassword()
                );
                
                if (!smsSent) {
                    // Log error but continue - maybe panel is offline
                    alertLog.setResolutionDescription("SMS to panel failed: " + 
                        (system.getPanelSimNumber() != null ? system.getPanelSimNumber() : "No panel SIM"));
                }
                
                // Update siren status
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
                alertLog.setStatus("PENDING");
                alertLog.setAlertType("SIREN_STOP");
                alertLog.setRawMessage(smsContent);
                alertLog.setZoneNumber(0);
                alertLog.setZoneNumbers("00");
                alertLog.setZoneNames("No Zone");
                alertLog.setAlarmSystem(system);
                alertLog.setResolutionDescription("Siren stopped. Alert still pending.");
                return alertLogRepository.save(alertLog);
            }
        }

        // ============================================================
        // 2. DISARM - Send SMS to panel + Resolve all alerts
        // ============================================================
        if (cleanMessage != null && (cleanMessage.toUpperCase().contains("DISARM") || 
            cleanMessage.toUpperCase().contains("8888#2A"))) {
            if (machineOpt.isPresent()) {
                AlarmSystem system = machineOpt.get();
                
                // Send SMS to panel to disarm
                boolean smsSent = smsService.sendDisarmCommand(
                    system.getPanelSimNumber(),
                    system.getPanelPassword()
                );
                
                if (!smsSent) {
                    alertLog.setResolutionDescription("Disarm SMS to panel failed");
                }
                
                // SIREN OFF
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
                // RESOLVE ALL PENDING ALERTS
                resolveAllPendingAlerts(system.getId(), "SYSTEM-DISARM", "System disarmed by user");
                
                alertLog.setStatus("RESOLVED");
                alertLog.setAlertType("DISARM");
                alertLog.setRawMessage(smsContent);
                alertLog.setZoneNumber(0);
                alertLog.setZoneNumbers("00");
                alertLog.setZoneNames("No Zone");
                alertLog.setAlarmSystem(system);
                alertLog.setResolutionDescription("System disarmed via SMS command");
                return alertLogRepository.save(alertLog);
            }
        }

        // ============================================================
        // 3. ARM - Send SMS to panel
        // ============================================================
        if (cleanMessage != null && (cleanMessage.trim().equalsIgnoreCase("ARM") || 
            cleanMessage.toUpperCase().contains("8888#1A"))) {
            if (machineOpt.isPresent()) {
                AlarmSystem system = machineOpt.get();
                
                // Send SMS to panel to arm
                boolean smsSent = smsService.sendArmCommand(
                    system.getPanelSimNumber(),
                    system.getPanelPassword()
                );
                
                if (!smsSent) {
                    alertLog.setResolutionDescription("Arm SMS to panel failed");
                }
                
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                alertLog.setAlarmSystem(system);
            }
            alertLog.setStatus("ARMED");
        }
        // ============================================================
        // 4. CALL
        // ============================================================
        else if (cleanMessage != null && cleanMessage.toLowerCase().contains("call incoming")) {
            alertLog.setStatus("CALL");
            if (machineOpt.isPresent()) {
                alertLog.setAlarmSystem(machineOpt.get());
            }
        }
        // ============================================================
        // 5. ZONE ALARM - SIREN ON
        // ============================================================
        else if (cleanMessage != null && 
                 (cleanMessage.toLowerCase().contains("zone") || 
                  cleanMessage.toLowerCase().contains("alarm"))) {
            alertLog.setStatus("PENDING");
            if (machineOpt.isPresent()) {
                AlarmSystem system = machineOpt.get();
                // SIREN ON (panel will handle this automatically)
                system.setSirenStatus("ON");
                alarmSystemRepository.save(system);
                alertLog.setAlarmSystem(system);
            }
        }
        // ============================================================
        // 6. DEFAULT
        // ============================================================
        else {
            alertLog.setStatus("PENDING");
            if (machineOpt.isPresent()) {
                alertLog.setAlarmSystem(machineOpt.get());
            }
        }

        // Extract zone numbers
        String zoneNumbers = extractZoneNumbers(smsContent);
        
        if (!zoneNumbers.isEmpty()) {
            alertLog.setZoneNumbers(zoneNumbers);
            String firstZone = zoneNumbers.split(",")[0].trim();
            try {
                alertLog.setZoneNumber(Integer.parseInt(firstZone));
            } catch (NumberFormatException e) {
                alertLog.setZoneNumber(0);
            }
            
            if (machineOpt.isPresent()) {
                String zoneNames = getZoneNames(machineOpt.get().getId(), zoneNumbers);
                alertLog.setZoneNames(zoneNames);
            }
        } else {
            alertLog.setZoneNumber(0);
            alertLog.setZoneNumbers("00");
            alertLog.setZoneNames("No Zone");
        }

        alertLog.setAlertType(cleanMessage);
        alertLog.setRawMessage(smsContent);
        
        return alertLogRepository.save(alertLog);
    }

    // ============================================================
    // RESOLVE ALL PENDING ALERTS + SIREN OFF
    // ============================================================
    @Transactional
    public List<AlertLog> resolveAllPendingAlerts(Long systemId, String resolvedBy, String description) {
        List<AlertLog> pendingAlerts = alertLogRepository
            .findByAlarmSystemIdAndStatusOrderByReceivedAtDesc(systemId, "PENDING");
        
        if (pendingAlerts.isEmpty()) {
            return pendingAlerts;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
        if (systemOpt.isPresent()) {
            AlarmSystem system = systemOpt.get();
            system.setSirenStatus("OFF");
            alarmSystemRepository.save(system);
            
            // Send SMS to panel to ensure siren is off
            smsService.sendSirenStopCommand(
                system.getPanelSimNumber(),
                system.getPanelPassword()
            );
        }
        
        for (AlertLog alert : pendingAlerts) {
            Duration duration = Duration.between(alert.getReceivedAt(), now);
            alert.setStatus("RESOLVED");
            alert.setResolvedAt(now);
            alert.setResolvedBy(resolvedBy);
            alert.setPendingDurationSeconds(duration.getSeconds());
            alert.setResolutionDescription(description);
            alertLogRepository.save(alert);
        }
        
        return pendingAlerts;
    }

    // ============================================================
    // RESOLVE SINGLE ALERT + SIREN OFF
    // ============================================================
    @Transactional
    public AlertLog resolveAlert(Long alertId, String resolvedBy, String clientIp, String description) {
        Optional<AlertLog> alertOpt = alertLogRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            throw new RuntimeException("Alert not found with ID: " + alertId);
        }

        AlertLog alert = alertOpt.get();
        
        if (!"PENDING".equals(alert.getStatus())) {
            throw new RuntimeException("Only PENDING alerts can be resolved. Current status: " + alert.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(alert.getReceivedAt(), now);
        
        // ===== UPDATE SIREN STATUS =====
        if (alert.getAlarmSystem() != null) {
            AlarmSystem system = alert.getAlarmSystem();
            long pendingCount = alertLogRepository.countByAlarmSystemIdAndStatus(system.getId(), "PENDING");
            
            if (pendingCount <= 1) {
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
                // Send SMS to panel to stop siren
                smsService.sendSirenStopCommand(
                    system.getPanelSimNumber(),
                    system.getPanelPassword()
                );
            }
        }
        
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(now);
        alert.setResolvedBy(resolvedBy);
        alert.setPendingDurationSeconds(duration.getSeconds());
        alert.setResolvedFromIp(clientIp);
        
        if (description != null && !description.trim().isEmpty()) {
            alert.setResolutionDescription(description.trim());
        }

        AlertLog savedAlert = alertLogRepository.save(alert);
        
        if (savedAlert.getAlarmSystem() != null && savedAlert.getZoneNumbers() != null) {
            savedAlert.setZoneNames(getZoneNames(savedAlert.getAlarmSystem().getId(), savedAlert.getZoneNumbers()));
        }
        
        return savedAlert;
    }

    // ============================================================
    // FIND SYSTEM
    // ============================================================
    private Optional<AlarmSystem> findSystem(String atmCode, String simNumber) {
        Optional<AlarmSystem> machineOpt = Optional.empty();
        
        if (atmCode != null && !atmCode.trim().isEmpty()) {
            machineOpt = alarmSystemRepository.findBySystemCode(atmCode.trim());
        }
        
        if (machineOpt.isEmpty() && simNumber != null && !simNumber.isEmpty()) {
            String rawSim = simNumber.trim();
            machineOpt = alarmSystemRepository.findBySimNumber(rawSim);

            if (machineOpt.isEmpty()) {
                String digits = rawSim.replaceAll("\\D+", "");
                if (!digits.isEmpty()) {
                    machineOpt = alarmSystemRepository.findBySimNumber(digits);
                }
            }

            if (machineOpt.isEmpty()) {
                String digits = rawSim.replaceAll("\\D+", "");
                if (digits.startsWith("94") && digits.length() > 2) {
                    String local = "0" + digits.substring(2);
                    machineOpt = alarmSystemRepository.findBySimNumber(local);
                }
            }
        }

        return machineOpt;
    }

    // ============================================================
    // GET ZONE NAMES
    // ============================================================
    private String getZoneNames(Long systemId, String zoneNumbers) {
        if (zoneNumbers == null || zoneNumbers.isEmpty() || zoneNumbers.equals("00")) {
            return "No Zone";
        }
        
        String[] zoneArray = zoneNumbers.split(",");
        List<String> zoneNames = new ArrayList<>();
        
        for (String zoneStr : zoneArray) {
            try {
                int zoneNum = Integer.parseInt(zoneStr.trim());
                Optional<AlarmZone> zoneOpt = alarmZoneRepository.findByAlarmSystemIdAndZoneNumber(systemId, zoneNum);
                if (zoneOpt.isPresent()) {
                    zoneNames.add(zoneOpt.get().getZoneName());
                } else {
                    zoneNames.add("Zone " + zoneStr.trim());
                }
            } catch (NumberFormatException e) {
                zoneNames.add("Zone " + zoneStr.trim());
            }
        }
        
        return String.join(", ", zoneNames);
    }

    // ============================================================
    // EXTRACT ZONE NUMBERS
    // ============================================================
    private String extractZoneNumbers(String smsContent) {
        List<String> zones = new ArrayList<>();
        
        if (smsContent == null || smsContent.isEmpty()) {
            return "";
        }
        
        Pattern pattern1 = Pattern.compile("Zone:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(smsContent);
        while (matcher1.find()) {
            zones.add(matcher1.group(1));
        }
        
        Pattern pattern2 = Pattern.compile("ZONE\\s*(\\d+)\\s+ALARM!", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(smsContent);
        while (matcher2.find()) {
            zones.add(matcher2.group(1));
        }
        
        Pattern pattern3 = Pattern.compile("ZONE\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pattern3.matcher(smsContent);
        while (matcher3.find()) {
            String zone = matcher3.group(1);
            if (!zones.contains(zone)) {
                zones.add(zone);
            }
        }
        
        List<String> uniqueZones = zones.stream().distinct().collect(Collectors.toList());
        return uniqueZones.isEmpty() ? "" : String.join(",", uniqueZones);
    }

    // ============================================================
    // GET ALL ALERTS
    // ============================================================
    public List<AlertLog> getAllAlerts(String username) {
        try {
            List<AlertLog> alerts;
            
            if (username != null && !username.trim().isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                    List<UserSystem> userSystems = userSystemRepository.findAllByUserId(userOpt.get().getId());
                    List<Long> systemIds = userSystems.stream()
                        .map(UserSystem::getSystemId)
                        .collect(Collectors.toList());
                    
                    if (systemIds.isEmpty()) {
                        return new ArrayList<>();
                    }
                    alerts = alertLogRepository.findAllByAlarmSystemIdInOrderByReceivedAtDesc(systemIds);
                } else {
                    alerts = alertLogRepository.findAllByOrderByReceivedAtDesc();
                }
            } else {
                alerts = alertLogRepository.findAllByOrderByReceivedAtDesc();
            }
            
            for (AlertLog alert : alerts) {
                if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null && !alert.getZoneNumbers().isEmpty()) {
                    try {
                        String zoneNames = getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers());
                        alert.setZoneNames(zoneNames);
                    } catch (Exception e) {
                        alert.setZoneNames("No Zone");
                    }
                } else {
                    alert.setZoneNames("No Zone");
                }
            }
            
            return alerts;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ============================================================
    // GET ALERT WITH DETAILS
    // ============================================================
    public AlertLog getAlertWithDetails(Long alertId) {
        try {
            AlertLog alert = alertLogRepository.findByIdWithSystem(alertId);
            if (alert != null && alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                alert.setZoneNames(getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers()));
            }
            return alert;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ============================================================
    // REGISTER HEARTBEAT
    // ============================================================
    public void registerHeartbeat(String atmCode, String simNumber) {
        Optional<AlarmSystem> machineOpt = Optional.empty();
        if (atmCode != null && !atmCode.trim().isEmpty()) {
            machineOpt = alarmSystemRepository.findBySystemCode(atmCode.trim());
        }
        if (machineOpt.isEmpty() && simNumber != null && !simNumber.trim().isEmpty()) {
            String rawSim = simNumber.trim();
            machineOpt = alarmSystemRepository.findBySimNumber(rawSim);
            if (machineOpt.isEmpty()) {
                String digits = rawSim.replaceAll("\\D+", "");
                if (!digits.isEmpty()) machineOpt = alarmSystemRepository.findBySimNumber(digits);
                if (machineOpt.isEmpty() && digits.startsWith("94") && digits.length() > 2) {
                    String local = "0" + digits.substring(2);
                    machineOpt = alarmSystemRepository.findBySimNumber(local);
                }
            }
        }

        if (machineOpt.isPresent()) {
            AlarmSystem sys = machineOpt.get();
            sys.setLastStatusChangedAt(java.time.LocalDateTime.now());
            alarmSystemRepository.save(sys);
        } else {
            if (atmCode != null && !atmCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid ATM Code: " + atmCode);
            }
        }
    }

    // ============================================================
    // COUNT METHODS
    // ============================================================
    public long getPendingCount() {
        return alertLogRepository.countByStatus("PENDING");
    }

    public long getResolvedCount() {
        return alertLogRepository.countResolved();
    }

    public List<AlertLog> getPendingAlerts() {
        try {
            List<AlertLog> alerts = alertLogRepository.findAllByOrderByReceivedAtDesc()
                .stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .collect(Collectors.toList());
            
            for (AlertLog alert : alerts) {
                if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                    alert.setZoneNames(getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers()));
                }
            }
            
            return alerts;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<AlertLog> getAlertsByStatus(String status) {
        try {
            List<AlertLog> alerts = alertLogRepository.findAllByOrderByReceivedAtDesc()
                .stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
            
            for (AlertLog alert : alerts) {
                if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                    alert.setZoneNames(getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers()));
                }
            }
            
            return alerts;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ============================================================
    // DISARM SYSTEM (API/Dashboard)
    // ============================================================
    @Transactional
    public DisarmResult disarmSystem(String systemCode, String triggeredBy) {
        Optional<AlarmSystem> machineOpt = alarmSystemRepository.findBySystemCode(systemCode);
        if (machineOpt.isEmpty()) {
            throw new IllegalArgumentException("System not found: " + systemCode);
        }
        AlarmSystem system = machineOpt.get();
        
        // Send SMS to panel
        boolean smsSent = smsService.sendDisarmCommand(
            system.getPanelSimNumber(),
            system.getPanelPassword()
        );
        
        system.setSirenStatus("OFF");
        alarmSystemRepository.save(system);

        List<AlertLog> resolved = resolveAllPendingAlerts(system.getId(), triggeredBy != null ? triggeredBy : "SYSTEM", "System disarmed");

        // Create DISARM log
        AlertLog disarmLog = new AlertLog();
        disarmLog.setAlarmSystem(system);
        disarmLog.setStatus("RESOLVED");
        disarmLog.setAlertType("DISARM");
        disarmLog.setRawMessage("System disarmed via API/Dashboard" + (smsSent ? "" : " (SMS failed)"));
        disarmLog.setReceivedAt(LocalDateTime.now());
        disarmLog.setResolvedAt(LocalDateTime.now());
        disarmLog.setResolvedBy(triggeredBy != null ? triggeredBy : "SYSTEM");
        disarmLog.setResolutionDescription("System disarmed" + (smsSent ? " via SMS" : " (SMS to panel failed)"));
        disarmLog.setZoneNumber(0);
        disarmLog.setZoneNumbers("00");
        disarmLog.setZoneNames("No Zone");
        alertLogRepository.save(disarmLog);

        return new DisarmResult(resolved.size(), smsSent);
    }

    // ============================================================
    // STOP SIREN ONLY (API/Dashboard)
    // ============================================================
    @Transactional
    public SirenStopResult stopSirenOnly(String systemCode, String triggeredBy) {
        Optional<AlarmSystem> machineOpt = alarmSystemRepository.findBySystemCode(systemCode);
        if (machineOpt.isEmpty()) {
            throw new IllegalArgumentException("System not found: " + systemCode);
        }
        AlarmSystem system = machineOpt.get();
        
        // Send SMS to panel
        boolean smsSent = smsService.sendSirenStopCommand(
            system.getPanelSimNumber(),
            system.getPanelPassword()
        );
        
        system.setSirenStatus("OFF");
        alarmSystemRepository.save(system);

        long pendingCount = alertLogRepository.countByAlarmSystemIdAndStatus(system.getId(), "PENDING");

        // Create SIREN_STOP log
        AlertLog stopLog = new AlertLog();
        stopLog.setAlarmSystem(system);
        stopLog.setStatus("PENDING");
        stopLog.setAlertType("SIREN_STOP");
        stopLog.setRawMessage("Siren stopped via API/Dashboard" + (smsSent ? "" : " (SMS failed)"));
        stopLog.setReceivedAt(LocalDateTime.now());
        stopLog.setZoneNumber(0);
        stopLog.setZoneNumbers("00");
        stopLog.setZoneNames("No Zone");
        stopLog.setResolutionDescription("Siren stopped by " + (triggeredBy != null ? triggeredBy : "user") + 
            (smsSent ? " via SMS" : " (SMS to panel failed)") + ". Alert still pending.");
        alertLogRepository.save(stopLog);

        return new SirenStopResult((int) pendingCount, smsSent);
    }

    public static class DisarmResult {
        private final int resolvedCount;
        private final boolean smsSent;
        
        public DisarmResult(int resolvedCount, boolean smsSent) {
            this.resolvedCount = resolvedCount;
            this.smsSent = smsSent;
        }
        
        public int getResolvedCount() {
            return resolvedCount;
        }
        
        public boolean isSmsSent() {
            return smsSent;
        }
    }

    public static class SirenStopResult {
        private final int pendingCount;
        private final boolean smsSent;
        
        public SirenStopResult(int pendingCount, boolean smsSent) {
            this.pendingCount = pendingCount;
            this.smsSent = smsSent;
        }
        
        public int getPendingCount() {
            return pendingCount;
        }
        
        public boolean isSmsSent() {
            return smsSent;
        }
    }
}