package com.security.alarm.controller;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AlertController {

    private final AlertService alertService;
    private final com.security.alarm.repository.AlarmSystemRepository alarmSystemRepository;
    private final com.security.alarm.repository.AlertLogRepository alertLogRepository;

    public AlertController(AlertService alertService,
                           com.security.alarm.repository.AlarmSystemRepository alarmSystemRepository,
                           com.security.alarm.repository.AlertLogRepository alertLogRepository) {
        this.alertService = alertService;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
    }

    // ============================================================
    // SMS SIMULATE - Process all commands
    // ============================================================
    @PostMapping("/sms-simulate")
    public ResponseEntity<?> simulateSMS(@RequestBody Map<String, String> smsData) {
        String simNumber = smsData.get("simNumber");
        String message = smsData.get("message");
        String atmCode = smsData.get("atmCode");
        
        try {
            AlertLog savedLog = alertService.processIncomingSMS(simNumber, message, atmCode);
            
            if (message != null && message.toUpperCase().contains("SIREN_STOP")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("action", "SIREN_STOP");
                response.put("sirenStopped", true);
                response.put("alertsResolved", false);
                response.put("message", "Siren stopped successfully. Alerts still pending.");
                response.put("alert", savedLog);
                return ResponseEntity.ok(response);
            }
            
            if (message != null && (message.toUpperCase().contains("DISARM") || 
                message.toUpperCase().contains("8888#2A"))) {
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("action", "DISARM");
                response.put("sirenStopped", true);
                response.put("alertsResolved", true);
                response.put("message", "System disarmed. All alerts resolved.");
                response.put("alert", savedLog);
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(savedLog);
            
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(403).body(iae.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing SMS: " + e.getMessage());
        }
    }

    // ============================================================
    // SET COMMAND - ARM / DISARM (Tech Department Request)
    // Direct SMS to panel - NO ESP32
    // ============================================================
    @PostMapping("/set-command")
    public ResponseEntity<?> setCommand(
            @RequestParam String atmCode,
            @RequestParam String command) {
        
        if (atmCode == null || atmCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("atmCode is required");
        }
        
        if (command == null || command.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("command is required (ARM or DISARM)");
        }
        
        String cmd = command.trim().toUpperCase();
        if (!cmd.equals("ARM") && !cmd.equals("DISARM")) {
            return ResponseEntity.badRequest().body("command must be ARM or DISARM");
        }
        
        try {
            Optional<com.security.alarm.entity.AlarmSystem> systemOpt = 
                alarmSystemRepository.findBySystemCode(atmCode);
            
            if (systemOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("System not found: " + atmCode);
            }
            
            com.security.alarm.entity.AlarmSystem system = systemOpt.get();
            
            // Send SMS to panel
            String panelNumber = system.getPanelSimNumber();
            String password = system.getPanelPassword();
            
            String smsCommand;
            String actionMessage;
            String logStatus;
            String logType;
            
            if (cmd.equals("ARM")) {
                smsCommand = password + "#1A";
                actionMessage = "Arm command sent to panel";
                logStatus = "ARMED";
                logType = "ARM";
                
                // Send SMS to panel
                boolean sent = sendSmsToPanel(panelNumber, smsCommand);
                if (!sent) {
                    return ResponseEntity.status(500).body("Failed to send ARM SMS to panel");
                }
                
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
            } else { // DISARM
                smsCommand = password + "#2A";
                actionMessage = "Disarm command sent to panel";
                logStatus = "RESOLVED";
                logType = "DISARM";
                
                // Send SMS to panel
                boolean sent = sendSmsToPanel(panelNumber, smsCommand);
                if (!sent) {
                    return ResponseEntity.status(500).body("Failed to send DISARM SMS to panel");
                }
                
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
                // Resolve all pending alerts
                List<AlertLog> pendingAlerts = alertLogRepository
                    .findByAlarmSystemIdAndStatusOrderByReceivedAtDesc(system.getId(), "PENDING");
                
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                for (AlertLog alert : pendingAlerts) {
                    java.time.Duration duration = java.time.Duration.between(alert.getReceivedAt(), now);
                    alert.setStatus("RESOLVED");
                    alert.setResolvedAt(now);
                    alert.setResolvedBy("DASHBOARD");
                    alert.setPendingDurationSeconds(duration.getSeconds());
                    alert.setResolutionDescription("System disarmed by dashboard command");
                    alertLogRepository.save(alert);
                }
            }
            
            // Create log
            AlertLog log = new AlertLog();
            log.setAlarmSystem(system);
            log.setStatus(logStatus);
            log.setAlertType(logType);
            log.setRawMessage("Command sent via SMS: " + smsCommand);
            log.setReceivedAt(java.time.LocalDateTime.now());
            log.setZoneNumber(0);
            log.setZoneNumbers("00");
            log.setZoneNames("No Zone");
            alertLogRepository.save(log);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("action", cmd);
            response.put("message", actionMessage);
            response.put("smsCommand", smsCommand);
            response.put("panelNumber", panelNumber);
            response.put("systemCode", atmCode);
            response.put("sirenStatus", "OFF");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing command: " + e.getMessage());
        }
    }

    // ============================================================
    // ESP32 COMMAND POLLING - REMOVED (No longer needed)
    // ============================================================
    // @GetMapping("/command/{atmCode}") - REMOVED
    // ESP32 polling endpoint is no longer available

    // ============================================================
    // DISARM SYSTEM - Resolve all alerts + Siren OFF
    // ============================================================
    @PostMapping("/disarm")
    public ResponseEntity<?> disarmSystem(@RequestBody Map<String, String> request) {
        String systemCode = request.get("systemCode");
        String triggeredBy = request.get("triggeredBy");
        
        if (systemCode == null || systemCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("systemCode is required");
        }
        
        try {
            AlertService.DisarmResult result = alertService.disarmSystem(systemCode, triggeredBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("action", "DISARM");
            response.put("sirenStopped", true);
            response.put("alertsResolved", true);
            response.put("systemCode", systemCode);
            response.put("resolvedAlerts", result.getResolvedCount());
            response.put("smsSent", result.isSmsSent());
            response.put("message", "System disarmed successfully. " + result.getResolvedCount() + 
                " alerts resolved." + (result.isSmsSent() ? " SMS sent to panel." : " SMS to panel failed."));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error disarming system: " + e.getMessage());
        }
    }

    // ============================================================
    // STOP SIREN ONLY
    // ============================================================
    @PostMapping("/stop-siren")
    public ResponseEntity<?> stopSirenOnly(@RequestBody Map<String, String> request) {
        String systemCode = request.get("systemCode");
        String triggeredBy = request.get("triggeredBy");
        
        if (systemCode == null || systemCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("systemCode is required");
        }
        
        try {
            AlertService.SirenStopResult result = alertService.stopSirenOnly(systemCode, triggeredBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("action", "SIREN_STOP");
            response.put("sirenStopped", true);
            response.put("alertsResolved", false);
            response.put("pendingAlerts", result.getPendingCount());
            response.put("systemCode", systemCode);
            response.put("smsSent", result.isSmsSent());
            response.put("message", "Siren stopped. " + result.getPendingCount() + 
                " alerts still pending." + (result.isSmsSent() ? " SMS sent to panel." : " SMS to panel failed."));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error stopping siren: " + e.getMessage());
        }
    }

    // ============================================================
    // HEARTBEAT
    // ============================================================
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(@RequestBody Map<String, String> data) {
        String atmCode = data.get("atmCode");
        String simNumber = data.get("simNumber");
        try {
            alertService.registerHeartbeat(atmCode, simNumber);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Heartbeat recorded");
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(403).body("Invalid ATM Code");
        }
    }

    // ============================================================
    // GET ALL ALERTS
    // ============================================================
    @GetMapping
    public ResponseEntity<List<AlertLog>> getAllAlerts(@RequestParam(required = false) String username) {
        return ResponseEntity.ok(alertService.getAllAlerts(username));
    }

    // ============================================================
    // RESOLVE ALERT
    // ============================================================
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(
            @PathVariable Long id,
            @RequestParam String resolvedBy,
            @RequestParam(required = false) String description,
            HttpServletRequest request) {
        
        try {
            String clientIp = request.getRemoteAddr();
            if (clientIp == null || clientIp.isEmpty() || "0:0:0:0:0:0:0:1".equals(clientIp)) {
                clientIp = "127.0.0.1";
            }
            
            AlertLog resolvedAlert = alertService.resolveAlert(id, resolvedBy, clientIp, description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert resolved successfully");
            response.put("alert", resolvedAlert);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============================================================
    // GET ALERT DETAILS
    // ============================================================
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getAlertDetails(@PathVariable Long id) {
        AlertLog alert = alertService.getAlertWithDetails(id);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }

    // ============================================================
    // GET PENDING COUNT
    // ============================================================
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("pending", alertService.getPendingCount());
        response.put("resolved", alertService.getResolvedCount());
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET PENDING ALERTS
    // ============================================================
    @GetMapping("/pending")
    public ResponseEntity<List<AlertLog>> getPendingAlerts() {
        return ResponseEntity.ok(alertService.getPendingAlerts());
    }

    // ============================================================
    // GET ALERTS BY STATUS
    // ============================================================
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AlertLog>> getAlertsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    // ============================================================
    // HELPER: Send SMS to Panel
    // ============================================================
    private boolean sendSmsToPanel(String panelNumber, String smsCommand) {
        // This is a placeholder - will be replaced with actual SMS service
        // In production, use SmsService
        System.out.println("[SMS] Sending to " + panelNumber + ": " + smsCommand);
        
        try {
            // Simulate SMS sending
            // return smsService.sendSms(panelNumber, smsCommand);
            return true; // Placeholder for now
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            return false;
        }
    }
}