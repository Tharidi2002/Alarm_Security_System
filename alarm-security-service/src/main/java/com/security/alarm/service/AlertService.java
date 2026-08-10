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
    // PROCESS INCOMING SMS - WITH ACTIVE/INACTIVE CHECK
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
        // ✅ FIXED: REJECT if system is INACTIVE or NOT FOUND
        // ============================================================
        if (machineOpt.isEmpty()) {
            alertLog.setStatus("REJECTED");
            alertLog.setAlertType("SYSTEM_INACTIVE_OR_NOT_FOUND");
            alertLog.setRawMessage("System INACTIVE or not found. Alert rejected: " + smsContent);
            alertLog.setZoneNumber(0);
            alertLog.setZoneNumbers("00");
            alertLog.setZoneNames("No Zone");
            alertLog.setResolutionDescription("System is INACTIVE or not found. Alert was not processed.");
            
            AlertLog rejectedLog = alertLogRepository.save(alertLog);
            System.out.println("🚫 ALERT REJECTED: System " + 
                            (atmCode != null ? atmCode : fromSimNumber) + 
                            " is INACTIVE or not found.");
            return rejectedLog;
        }

        // ============================================================
        // System is ACTIVE - Continue processing
        // ============================================================
        AlarmSystem system = machineOpt.get();

        // ============================================================
        // 1. SIREN_STOP - Send SMS to panel and stop siren
        // ============================================================
        if (cleanMessage != null && cleanMessage.toUpperCase().contains("SIREN_STOP")) {
            // Send SMS to panel to stop siren
            boolean smsSent = smsService.sendSirenStopCommand(
                system.getPanelSimNumber(),
                system.getPanelPassword()
            );
            
            // Update siren status
            system.setSirenStatus("OFF");
            alarmSystemRepository.save(system);
            
            alertLog.setStatus("SIREN_STOP");  // ← PENDING වෙනුවට SIREN_STOP
            alertLog.setAlertType("SIREN_STOP");
            alertLog.setRawMessage(smsContent);
            alertLog.setZoneNumber(0);
            alertLog.setZoneNumbers("00");
            alertLog.setZoneNames("No Zone");
            alertLog.setAlarmSystem(system);
            alertLog.setResolutionDescription(
                smsSent ? "Siren stopped via SMS" : "SMS to panel failed"
            );
            return alertLogRepository.save(alertLog);
        }

        // ============================================================
        // 2. DISARM - Send SMS to panel + Resolve all alerts
        // ============================================================
        if (cleanMessage != null && (cleanMessage.toUpperCase().contains("DISARM") || 
            cleanMessage.toUpperCase().contains("8888#2A"))) {
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

        // ============================================================
        // 3. ARM - Send SMS to panel
        // ============================================================
        if (cleanMessage != null && (cleanMessage.trim().equalsIgnoreCase("ARM") || 
            cleanMessage.toUpperCase().contains("8888#1A"))) {
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
            alertLog.setStatus("ARMED");
            alertLog.setAlertType("ARM");
            alertLog.setRawMessage(smsContent);
            alertLog.setZoneNumber(0);
            alertLog.setZoneNumbers("00");
            alertLog.setZoneNames("No Zone");
            return alertLogRepository.save(alertLog);
        }
        // ============================================================
        // 4. CALL
        // ============================================================
        else if (cleanMessage != null && cleanMessage.toLowerCase().contains("call incoming")) {
            alertLog.setStatus("CALL");
            alertLog.setAlertType("CALL");
            alertLog.setRawMessage(smsContent);
            alertLog.setAlarmSystem(system);
            alertLog.setZoneNumber(0);
            alertLog.setZoneNumbers("00");
            alertLog.setZoneNames("No Zone");
            return alertLogRepository.save(alertLog);
        }
        // ============================================================
        // 5. ZONE ALARM - SIREN ON
        // ============================================================
        else if (cleanMessage != null && 
                (cleanMessage.toLowerCase().contains("zone") || 
                cleanMessage.toLowerCase().contains("alarm"))) {
            // SIREN ON (panel will handle this automatically)
            system.setSirenStatus("ON");
            alarmSystemRepository.save(system);
            
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
                String zoneNames = getZoneNames(system.getId(), zoneNumbers);
                alertLog.setZoneNames(zoneNames);
            } else {
                alertLog.setZoneNumber(0);
                alertLog.setZoneNumbers("00");
                alertLog.setZoneNames("No Zone");
            }
            
            alertLog.setStatus("PENDING");
            alertLog.setAlertType(cleanMessage);
            alertLog.setRawMessage(smsContent);
            alertLog.setAlarmSystem(system);
            return alertLogRepository.save(alertLog);
        }
        // ============================================================
        // 6. DEFAULT
        // ============================================================
        else {
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
                String zoneNames = getZoneNames(system.getId(), zoneNumbers);
                alertLog.setZoneNames(zoneNames);
            } else {
                alertLog.setZoneNumber(0);
                alertLog.setZoneNumbers("00");
                alertLog.setZoneNames("No Zone");
            }

            alertLog.setStatus("PENDING");
            alertLog.setAlertType(cleanMessage);
            alertLog.setRawMessage(smsContent);
            alertLog.setAlarmSystem(system);
            return alertLogRepository.save(alertLog);
        }
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
        
        // ============================================================
        // NEW: Cannot resolve SIREN_STOP or REJECTED alerts
        // ============================================================
        if ("SIREN_STOP".equals(alert.getStatus())) {
            throw new RuntimeException("SIREN_STOP is a system action, not a pending alert. Alert ID: " + alertId);
        }
        
        if ("REJECTED".equals(alert.getStatus())) {
            throw new RuntimeException("Cannot resolve REJECTED alert. Alert ID: " + alertId);
        }

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
    // FIND SYSTEM - WITH ACTIVE CHECK (NEW)
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

        // ============================================================
        // NEW: Only return ACTIVE systems
        // ============================================================
        if (machineOpt.isPresent()) {
            AlarmSystem system = machineOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(system.getStatus())) {
                System.out.println("🚫 System " + system.getSystemCode() + 
                                   " is " + system.getStatus() + ". Rejecting.");
                return Optional.empty();
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
                    // Exclude REJECTED alerts for users
                    alerts = alertLogRepository.findAllByAlarmSystemIdInAndNotRejected(systemIds);
                } else {
                    // Exclude REJECTED alerts for admin/other views
                    alerts = alertLogRepository.findAllActiveAlerts();
                }
            } else {
                // Exclude REJECTED alerts for anonymous view
                alerts = alertLogRepository.findAllActiveAlerts();
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
    // REGISTER HEARTBEAT - WITH ACTIVE CHECK (NEW)
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
            // ============================================================
            // NEW: Only update if system is ACTIVE
            // ============================================================
            if ("ACTIVE".equalsIgnoreCase(sys.getStatus())) {
                sys.setLastStatusChangedAt(java.time.LocalDateTime.now());
                alarmSystemRepository.save(sys);
                System.out.println("✅ Heartbeat recorded for: " + sys.getSystemCode());
            } else {
                System.out.println("🚫 Heartbeat rejected: " + sys.getSystemCode() + 
                                   " is " + sys.getStatus());
            }
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
        return alertLogRepository.countPendingActive();
    }

    public long getResolvedCount() {
        return alertLogRepository.countResolved();
    }

    public List<AlertLog> getPendingAlerts() {
        try {
            // Exclude REJECTED alerts from pending lists
            List<AlertLog> alerts = alertLogRepository.findPendingAlerts();
            
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
            // Exclude REJECTED alerts when filtering by status
            List<AlertLog> alerts = alertLogRepository.findByStatusActive(status);
            
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
    // DISARM SYSTEM (API/Dashboard) - WITH ACTIVE CHECK (NEW)
    // ============================================================
    @Transactional
    public DisarmResult disarmSystem(String systemCode, String triggeredBy) {
        Optional<AlarmSystem> machineOpt = alarmSystemRepository.findBySystemCode(systemCode);
        if (machineOpt.isEmpty()) {
            throw new IllegalArgumentException("System not found: " + systemCode);
        }
        AlarmSystem system = machineOpt.get();
        
        // ============================================================
        // NEW: Check if system is ACTIVE
        // ============================================================
        if (!"ACTIVE".equalsIgnoreCase(system.getStatus())) {
            throw new IllegalStateException("Cannot disarm system. System is " + system.getStatus());
        }
        
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
    // STOP SIREN ONLY (API/Dashboard) - WITH ACTIVE CHECK (NEW)
    // ============================================================
    @Transactional
    public SirenStopResult stopSirenOnly(String systemCode, String triggeredBy) {
        return stopSirenOnly(systemCode, triggeredBy, null);
    }

    @Transactional
    public SirenStopResult stopSirenOnly(String systemCode, String triggeredBy, String description) {
        Optional<AlarmSystem> machineOpt = alarmSystemRepository.findBySystemCode(systemCode);
        if (machineOpt.isEmpty()) {
            throw new IllegalArgumentException("System not found: " + systemCode);
        }
        AlarmSystem system = machineOpt.get();
        
        // ============================================================
        // NEW: Check if system is ACTIVE
        // ============================================================
        if (!"ACTIVE".equalsIgnoreCase(system.getStatus())) {
            throw new IllegalStateException("Cannot stop siren. System is " + system.getStatus());
        }
        
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
        stopLog.setStatus("SIREN_STOP");
        stopLog.setAlertType("SIREN_STOP");
        stopLog.setRawMessage("Siren stopped via API/Dashboard" + (smsSent ? "" : " (SMS failed)"));
        stopLog.setReceivedAt(LocalDateTime.now());
        stopLog.setResolvedAt(LocalDateTime.now());
        stopLog.setResolvedBy(triggeredBy != null ? triggeredBy : "SYSTEM");
        stopLog.setZoneNumber(0);
        stopLog.setZoneNumbers("00");
        stopLog.setZoneNames("No Zone");
        stopLog.setResolutionDescription(description != null && !description.trim().isEmpty() 
            ? description.trim() 
            : "Siren stopped by " + (triggeredBy != null ? triggeredBy : "user"));
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

    // ============================================================
    // GET ALERTS FOR COMPANY (USER ROLE)
    // ============================================================
    public List<AlertLog> getAlertsForCompany(String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty() || !"USER".equalsIgnoreCase(userOpt.get().getRole())) {
                return new ArrayList<>();
            }
            
            User user = userOpt.get();
            if (user.getCompany() == null) {
                return new ArrayList<>();
            }
            
            Long companyId = user.getCompany().getId();
            
            // Get all systems in this company
            List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
            List<Long> systemIds = systems.stream()
                .map(AlarmSystem::getId)
                .collect(java.util.stream.Collectors.toList());
            
            if (systemIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Exclude REJECTED alerts for company users
            List<AlertLog> alerts = alertLogRepository.findAllByAlarmSystemIdInAndNotRejected(systemIds);
            
            // Set zone names
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
}