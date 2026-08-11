package com.security.alarm.controller;

import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.service.NotificationService;
import com.security.alarm.service.ScheduledTaskService;
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/deleted-systems")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DeletedSystemsController {

    private final AlarmSystemRepository alarmSystemRepository;
    private final ScheduledTaskService scheduledTaskService;
    private final NotificationService notificationService;
    private final PermissionService permissionService;

    public DeletedSystemsController(AlarmSystemRepository alarmSystemRepository,
                                    ScheduledTaskService scheduledTaskService,
                                    NotificationService notificationService,
                                    PermissionService permissionService) {
        this.alarmSystemRepository = alarmSystemRepository;
        this.scheduledTaskService = scheduledTaskService;
        this.notificationService = notificationService;
        this.permissionService = permissionService;
    }

    // ============================================================
    // GET EXPIRING SYSTEMS COUNT (For dashboard notification)
    // ============================================================
    @GetMapping("/expiring/count")
    public ResponseEntity<?> getExpiringCount(@RequestParam(required = false) String username) {
        try {
            if (username == null || username.isEmpty() || !permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            long count = scheduledTaskService.getExpiringCount();
            Map<String, Object> response = new HashMap<>();
            response.put("expiringCount", count);
            response.put("message", count > 0 ? count + " systems will be auto-deleted today at 11:59 PM" : "No systems expiring today");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET EXPIRING SYSTEMS (For UI display)
    // ============================================================
    @GetMapping("/expiring")
    public ResponseEntity<?> getExpiringSystems(@RequestParam(required = false) String username) {
        try {
            if (username == null || username.isEmpty() || !permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<AlarmSystem> expiring = scheduledTaskService.getExpiringSystems();
            return ResponseEntity.ok(expiring);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // RESTORE SYSTEM (Before auto-delete)
    // ============================================================
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreSystem(@PathVariable Long id,
                                           @RequestParam(required = false) String username) {
        try {
            if (username == null || username.isEmpty() || !permissionService.isAdmin(username)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            AlarmSystem system = alarmSystemRepository.findById(id).orElse(null);
            if (system == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!Boolean.TRUE.equals(system.getDeleted())) {
                return ResponseEntity.badRequest().body("System is not deleted");
            }
            
            // Restore the system
            system.setDeleted(false);
            system.setDeletedAt(null);
            system.setDeletedBy(null);
            system.setStatus("ACTIVE");
            system.setLastStatusChangedAt(LocalDateTime.now());
            
            AlarmSystem restored = alarmSystemRepository.save(system);
            
            notificationService.sendSystemRestoredNotification(restored);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "System restored successfully",
                "system", restored
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}