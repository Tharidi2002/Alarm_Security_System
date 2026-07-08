package com.security.alarm.service;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.AlarmSystemRepository;
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
    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;

    public AlertService(AlertLogRepository alertLogRepository, 
                        AlarmSystemRepository alarmSystemRepository,
                        UserRepository userRepository,
                        UserSystemRepository userSystemRepository) {
        this.alertLogRepository = alertLogRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
    }

    // Process incoming SMS
    public AlertLog processIncomingSMS(String fromSimNumber, String smsContent) {
        AlertLog alertLog = new AlertLog();
        alertLog.setReceivedAt(LocalDateTime.now());
        
        // Check if it's ARMED message
        String cleanMessage = smsContent;
        if (fromSimNumber != null && !fromSimNumber.isEmpty()) {
            cleanMessage = cleanMessage.replace(fromSimNumber, "").trim();
        }

        // Check for ARMED status
        if (cleanMessage.toUpperCase().contains("ARMED")) {
            alertLog.setStatus("ARMED");
        } else {
            alertLog.setStatus("PENDING");
        }

        // Find system by SIM number
        Optional<AlarmSystem> machineOpt = alarmSystemRepository.findBySimNumber(fromSimNumber);
        
        if (machineOpt.isPresent()) {
            alertLog.setAlarmSystem(machineOpt.get());
        } else {
            alertLog.setAlarmSystem(null);
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
        } else {
            alertLog.setZoneNumber(0);
            alertLog.setZoneNumbers("00");
        }

        alertLog.setAlertType(cleanMessage);
        alertLog.setRawMessage(smsContent);
        
        return alertLogRepository.save(alertLog);
    }

    // Extract zone numbers from SMS
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

    // Get all alerts (filtered by user)
    public List<AlertLog> getAllAlerts(String username) {
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                List<UserSystem> userSystems = userSystemRepository.findAllByUserId(userOpt.get().getId());
                List<Long> systemIds = userSystems.stream().map(UserSystem::getSystemId).collect(Collectors.toList());
                if (systemIds.isEmpty()) {
                    return new ArrayList<>();
                }
                return alertLogRepository.findAllByAlarmSystemIdInOrderByReceivedAtDesc(systemIds);
            }
        }
        return alertLogRepository.findAllByOrderByReceivedAtDesc();
    }

    // ========== NEW RESOLVE METHODS ==========

    // Resolve an alert
    @Transactional
    public AlertLog resolveAlert(Long alertId, String resolvedBy, String clientIp, String description) {
        Optional<AlertLog> alertOpt = alertLogRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            throw new RuntimeException("Alert not found with ID: " + alertId);
        }

        AlertLog alert = alertOpt.get();
        
        // Only PENDING alerts can be resolved
        if (!"PENDING".equals(alert.getStatus())) {
            throw new RuntimeException("Only PENDING alerts can be resolved. Current status: " + alert.getStatus());
        }

        // Calculate pending duration
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(alert.getReceivedAt(), now);
        
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(now);
        alert.setResolvedBy(resolvedBy);
        alert.setPendingDurationSeconds(duration.getSeconds());
        alert.setResolvedFromIp(clientIp);
        
        if (description != null && !description.trim().isEmpty()) {
            alert.setResolutionDescription(description.trim());
        }

        return alertLogRepository.save(alert);
    }

    // Get alert with details
    public AlertLog getAlertWithDetails(Long alertId) {
        return alertLogRepository.findByIdWithSystem(alertId);
    }

    // Get pending alerts count
    public long getPendingCount() {
        return alertLogRepository.countByStatus("PENDING");
    }

    // Get resolved alerts count
    public long getResolvedCount() {
        return alertLogRepository.countResolved();
    }

    // Get all pending alerts
    public List<AlertLog> getPendingAlerts() {
        return alertLogRepository.findAllByOrderByReceivedAtDesc()
            .stream()
            .filter(a -> "PENDING".equals(a.getStatus()))
            .collect(Collectors.toList());
    }

    // Get alerts by status
    public List<AlertLog> getAlertsByStatus(String status) {
        return alertLogRepository.findAllByOrderByReceivedAtDesc()
            .stream()
            .filter(a -> status.equalsIgnoreCase(a.getStatus()))
            .collect(Collectors.toList());
    }
}