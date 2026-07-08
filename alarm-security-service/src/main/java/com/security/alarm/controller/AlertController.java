package com.security.alarm.controller;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // Simulate SMS
    @PostMapping("/sms-simulate")
    public ResponseEntity<AlertLog> simulateSMS(@RequestBody Map<String, String> smsData) {
        String simNumber = smsData.get("simNumber");
        String message = smsData.get("message");
        AlertLog savedLog = alertService.processIncomingSMS(simNumber, message);
        return ResponseEntity.ok(savedLog);
    }

    // Get all alerts
    @GetMapping
    public ResponseEntity<List<AlertLog>> getAllAlerts(@RequestParam(required = false) String username) {
        return ResponseEntity.ok(alertService.getAllAlerts(username));
    }

    // ========== NEW RESOLVE ENDPOINTS ==========

    // Resolve an alert
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

    // Get alert with full details
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getAlertDetails(@PathVariable Long id) {
        AlertLog alert = alertService.getAlertWithDetails(id);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }

    // Get pending alerts count
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("pending", alertService.getPendingCount());
        response.put("resolved", alertService.getResolvedCount());
        return ResponseEntity.ok(response);
    }

    // Get all pending alerts
    @GetMapping("/pending")
    public ResponseEntity<List<AlertLog>> getPendingAlerts() {
        return ResponseEntity.ok(alertService.getPendingAlerts());
    }

    // Get alerts by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AlertLog>> getAlertsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }
}