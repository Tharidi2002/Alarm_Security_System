package com.security.alarm.controller;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.service.RetentionService;
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/retention")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RetentionController {

    private final RetentionService retentionService;
    private final PermissionService permissionService;

    public RetentionController(RetentionService retentionService,
                               PermissionService permissionService) {
        this.retentionService = retentionService;
        this.permissionService = permissionService;
    }

    // ============================================================
    // GET PENDING DELETION ALERTS
    // ============================================================
    
    @GetMapping("/pending-delete")
    public ResponseEntity<?> getPendingDeletionAlerts(@RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username) && !permissionService.isUser(username)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            List<AlertLog> alerts = retentionService.getPendingDeletionAlerts(username);
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET RETENTION STATS
    // ============================================================
    
    @GetMapping("/stats")
    public ResponseEntity<?> getRetentionStats(@RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            Map<String, Object> stats = retentionService.getRetentionStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // POSTPONE DELETION
    // ============================================================
    
    @PostMapping("/postpone/{alertId}")
    public ResponseEntity<?> postponeDeletion(@PathVariable Long alertId,
                                              @RequestParam String username) {
        try {
            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest().body("username is required");
            }
            
            // Permission check
            if (!permissionService.isAdmin(username) && !permissionService.isUser(username)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            retentionService.postponeDeletion(alertId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deletion postponed for alert ID: " + alertId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET PENDING DELETION COUNT
    // ============================================================
    
    @GetMapping("/pending/count")
    public ResponseEntity<?> getPendingDeletionCount(@RequestParam(required = false) String username) {
        try {
            if (username != null && !username.isEmpty()) {
                if (!permissionService.isAdmin(username)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            long count = retentionService.getPendingDeletionCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("pendingDeletionCount", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}