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
@CrossOrigin(origins = {
    "https://alarm-security-system-java.vercel.app",
    "https://alarm-security-system-jawa.vercel.app",
    "https://*.vercel.app",
    "http://localhost:5173",
    "http://localhost:3000"
}, allowCredentials = "true")
public class AlertController {

    private final AlertService alertService;
    private final Map<String, String> activeCommands = new java.util.concurrent.ConcurrentHashMap<>();


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
    // ============================================================
    @PostMapping("/set-command")
    public ResponseEntity<?> setCommand(
            @RequestParam String atmCode,
            @RequestParam String command) {
        
        // Validation
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
            // Find system by atmCode
            Optional<com.security.alarm.entity.AlarmSystem> systemOpt = 
                alarmSystemRepository.findBySystemCode(atmCode);
            
            if (systemOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("System not found: " + atmCode);
            }
            
            com.security.alarm.entity.AlarmSystem system = systemOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("atmCode", atmCode);
            response.put("command", cmd);
            
            if (cmd.equals("ARM")) {
                // ARM Command
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
                // Create ARM log
                AlertLog armLog = new AlertLog();
                armLog.setAlarmSystem(system);
                armLog.setStatus("ARMED");
                armLog.setAlertType("ARM");
                armLog.setRawMessage("System armed by dashboard command");
                armLog.setReceivedAt(java.time.LocalDateTime.now());
                armLog.setZoneNumber(0);
                armLog.setZoneNumbers("00");
                armLog.setZoneNames("No Zone");
                alertLogRepository.save(armLog);
                
                response.put("message", "System armed successfully");
                response.put("sirenStatus", "OFF");
                
            } else if (cmd.equals("DISARM")) {
                // DISARM Command - Resolve all pending alerts + Siren OFF
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
                
                // Create DISARM log
                AlertLog disarmLog = new AlertLog();
                disarmLog.setAlarmSystem(system);
                disarmLog.setStatus("RESOLVED");
                disarmLog.setAlertType("DISARM");
                disarmLog.setRawMessage("System disarmed by dashboard command");
                disarmLog.setReceivedAt(now);
                disarmLog.setResolvedAt(now);
                disarmLog.setResolvedBy("DASHBOARD");
                disarmLog.setResolutionDescription("System disarmed by dashboard");
                disarmLog.setZoneNumber(0);
                disarmLog.setZoneNumbers("00");
                disarmLog.setZoneNames("No Zone");
                alertLogRepository.save(disarmLog);
                
                response.put("message", "System disarmed successfully. " + pendingAlerts.size() + " alerts resolved.");
                response.put("resolvedAlerts", pendingAlerts.size());
                response.put("sirenStatus", "OFF");
            }
            
            // Store command for ESP32 polling
            activeCommands.put(atmCode, cmd);
            System.out.println("[BACKEND]: Command set for " + atmCode + " -> " + cmd);

            return ResponseEntity.ok(response);
            
         } catch (Exception e) {
             return ResponseEntity.status(500).body("Error processing command: " + e.getMessage());
         }
     }

    // ============================================================
    // ESP32 GET COMMAND POLLING ENDPOINT
    // ============================================================
    @GetMapping("/command/{atmCode}")
    public ResponseEntity<String> getCommand(@PathVariable String atmCode) {
        String command = activeCommands.getOrDefault(atmCode, "NONE");
        if (!command.equals("NONE")) {
            activeCommands.put(atmCode, "NONE");
            System.out.println("[BACKEND]: Command " + command + " sent to ESP32 and reset to NONE.");
        }
        return ResponseEntity.ok(command);
    }

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
            response.put("message", "System disarmed successfully. " + result.getResolvedCount() + " alerts resolved.");
            
            // Register command for ESP32 polling
            activeCommands.put(systemCode, "DISARM");

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
            response.put("message", "Siren stopped. " + result.getPendingCount() + " alerts still pending.");
            
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
    // ADDITIONAL DEPENDENCIES (Required for set-command)
    // ============================================================
    private final com.security.alarm.repository.AlarmSystemRepository alarmSystemRepository;
    private final com.security.alarm.repository.AlertLogRepository alertLogRepository;

    public AlertController(AlertService alertService,
                           com.security.alarm.repository.AlarmSystemRepository alarmSystemRepository,
                           com.security.alarm.repository.AlertLogRepository alertLogRepository) {
        this.alertService = alertService;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
    }
}