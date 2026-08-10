package com.security.alarm.controller;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.service.AlertService;
import com.security.alarm.service.PermissionService;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.AlertLogRepository;
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
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlertLogRepository alertLogRepository;
    private final PermissionService permissionService;

    public AlertController(AlertService alertService,
                           AlarmSystemRepository alarmSystemRepository,
                           AlertLogRepository alertLogRepository,
                           PermissionService permissionService) {
        this.alertService = alertService;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
        this.permissionService = permissionService;
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
            
            // ============================================================
            // NEW: Check if alert was rejected due to INACTIVE system
            // ============================================================
            if ("REJECTED".equals(savedLog.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("rejected", true);
                response.put("reason", "System is INACTIVE");
                response.put("alert", savedLog);
                return ResponseEntity.ok(response);  // ← 200 OK
                // return ResponseEntity.status(403).body(response);
            }
            
            if (message != null && message.toUpperCase().contains("SIREN_STOP")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("action", "SIREN_STOP");
                response.put("sirenStopped", true);
                response.put("alertsResolved", false);
                response.put("status", "SIREN_STOP");  // ← NEW
                response.put("message", "Siren stopped successfully.");
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
    // SET COMMAND - ARM / DISARM - WITH ACTIVE CHECK (NEW)
    // ============================================================
    @PostMapping("/set-command")
    public ResponseEntity<?> setCommand(
            @RequestParam String atmCode,
            @RequestParam String command,
            @RequestParam(required = false) String username) {
        
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
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findBySystemCode(atmCode);
            if (systemOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("System not found: " + atmCode);
            }
            
            AlarmSystem system = systemOpt.get();
            
            // ============================================================
            // NEW: Check if system is ACTIVE
            // ============================================================
            if (!"ACTIVE".equalsIgnoreCase(system.getStatus())) {
                return ResponseEntity.status(403).body("System is " + system.getStatus() + 
                    ". Cannot send commands to inactive system.");
            }
            
            // Permission check
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, system.getId())) {
                    return ResponseEntity.status(403).body("Access denied: You can only manage systems in your company");
                }
            }
            
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
                
                boolean sent = sendSmsToPanel(panelNumber, smsCommand);
                if (!sent) {
                    return ResponseEntity.status(500).body("Failed to send DISARM SMS to panel");
                }
                
                system.setSirenStatus("OFF");
                alarmSystemRepository.save(system);
                
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
    // DISARM SYSTEM - WITH ACTIVE CHECK (NEW)
    // ============================================================
    @PostMapping("/disarm")
    public ResponseEntity<?> disarmSystem(@RequestBody Map<String, String> request,
                                          @RequestParam(required = false) String username) {
        String systemCode = request.get("systemCode");
        String triggeredBy = request.get("triggeredBy");
        
        if (systemCode == null || systemCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("systemCode is required");
        }
        
        try {
            // Permission check
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findBySystemCode(systemCode);
            if (systemOpt.isPresent() && username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, systemOpt.get().getId())) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
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
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error disarming system: " + e.getMessage());
        }
    }

    // ============================================================
    // STOP SIREN ONLY - WITH ACTIVE CHECK (NEW)
    // ============================================================
    @PostMapping("/stop-siren")
    public ResponseEntity<?> stopSirenOnly(@RequestBody Map<String, String> request,
                                           @RequestParam(required = false) String username) {
        String systemCode = request.get("systemCode");
        String triggeredBy = request.get("triggeredBy");
        String description = request.get("description");
        
        if (triggeredBy == null || triggeredBy.trim().isEmpty()) {
            triggeredBy = username != null ? username : "DASHBOARD";
        }
        
        if (systemCode == null || systemCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("systemCode is required");
        }
        
        try {
            // Permission check
            Optional<AlarmSystem> systemOpt = alarmSystemRepository.findBySystemCode(systemCode);
            if (systemOpt.isPresent() && username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, systemOpt.get().getId())) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            AlertService.SirenStopResult result = alertService.stopSirenOnly(systemCode, triggeredBy, description);
            
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
            response.put("description", description != null ? description : "");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
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
    // GET ALL ALERTS - COMPANY-BASED FILTERING
    // ============================================================
    @GetMapping
    public ResponseEntity<?> getAllAlerts(@RequestParam(required = false) String username) {
        if (username != null && !username.isEmpty()) {
            if (permissionService.isUser(username)) {
                List<AlertLog> alerts = alertService.getAlertsForCompany(username);
                return ResponseEntity.ok(alerts);
            }
            return ResponseEntity.ok(alertService.getAllAlerts(username));
        }
        return ResponseEntity.ok(alertService.getAllAlerts(null));
    }

    // ============================================================
    // RESOLVE ALERT - WITH PERMISSION CHECK
    // ============================================================
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(
            @PathVariable Long id,
            @RequestParam String resolvedBy,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String username,
            HttpServletRequest request) {
        
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canResolveAlert(username, id)) {
                    return ResponseEntity.status(403).body("Access denied: You can only resolve alerts from your company");
                }
            }
            
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
    // GET ALERT DETAILS - WITH PERMISSION CHECK
    // ============================================================
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getAlertDetails(@PathVariable Long id,
                                             @RequestParam(required = false) String username) {
        if (username != null && !username.isEmpty()) {
            if (!permissionService.canAccessAlert(username, id)) {
                return ResponseEntity.status(403).body("Access denied");
            }
        }
        
        AlertLog alert = alertService.getAlertWithDetails(id);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }

    // ============================================================
    // GET PENDING COUNT - COMPANY-BASED
    // ============================================================
    @GetMapping("/pending/count")
    public ResponseEntity<?> getPendingCount(@RequestParam(required = false) String username) {
        long pending;
        long resolved;
        
        if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
            List<AlertLog> alerts = alertService.getAlertsForCompany(username);
            pending = alerts.stream().filter(a -> "PENDING".equals(a.getStatus())).count();
            resolved = alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count();
        } else {
            pending = alertService.getPendingCount();
            resolved = alertService.getResolvedCount();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("pending", pending);
        response.put("resolved", resolved);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET PENDING ALERTS - COMPANY-BASED
    // ============================================================
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAlerts(@RequestParam(required = false) String username) {
        if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
            List<AlertLog> alerts = alertService.getAlertsForCompany(username);
            List<AlertLog> pending = alerts.stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(pending);
        }
        return ResponseEntity.ok(alertService.getPendingAlerts());
    }

    // ============================================================
    // GET ALERTS BY STATUS - COMPANY-BASED
    // ============================================================
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getAlertsByStatus(@PathVariable String status,
                                               @RequestParam(required = false) String username) {
        if (username != null && !username.isEmpty() && permissionService.isUser(username)) {
            List<AlertLog> alerts = alertService.getAlertsForCompany(username);
            List<AlertLog> filtered = alerts.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(filtered);
        }
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    // ============================================================
    // HELPER: Send SMS to Panel
    // ============================================================
    private boolean sendSmsToPanel(String panelNumber, String smsCommand) {
        System.out.println("[SMS] Sending to " + panelNumber + ": " + smsCommand);
        try {
            // TODO: Implement actual SMS sending
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            return false;
        }
    }
}