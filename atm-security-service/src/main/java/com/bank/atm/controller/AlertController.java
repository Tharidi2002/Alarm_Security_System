package com.bank.atm.controller;

import com.bank.atm.entity.AlertLog;
import com.bank.atm.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173"})
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/sms-simulate")
    public ResponseEntity<?> simulateSMS(@RequestBody Map<String, String> smsData) {
        try {
            String simNumber = smsData.get("simNumber");
            String message = smsData.get("message");
            
            log.info("Simulating SMS - SIM: {}, Message: {}", simNumber, message);
            
            AlertLog savedLog = alertService.processIncomingSMS(simNumber, message);
            return ResponseEntity.ok(savedLog);
        } catch (Exception e) {
            log.error("Error processing SMS: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long atmId) {
        try {
            Page<AlertLog> alerts = alertService.getAlertsPaginated(page, size, status, atmId);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAlertById(@PathVariable Long id) {
        try {
            AlertLog alert = alertService.getAlertById(id);
            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            log.error("Error fetching alert: {}", e.getMessage(), e);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(
            @PathVariable Long id,
            @RequestParam String username) {
        try {
            AlertLog updated = alertService.acknowledgeAlert(id, username);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error acknowledging alert: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String notes = request != null ? request.get("notes") : "";
            AlertLog updated = alertService.resolveAlert(id, username, notes);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error resolving alert: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/ignore")
    public ResponseEntity<?> ignoreAlert(@PathVariable Long id) {
        try {
            AlertLog updated = alertService.ignoreAlert(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error ignoring alert: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", alertService.getPendingAlertsCount());
            stats.put("unresolved", alertService.getUnresolvedAlertsCount());
            stats.put("total", alertService.getAllAlerts().size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching stats: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}