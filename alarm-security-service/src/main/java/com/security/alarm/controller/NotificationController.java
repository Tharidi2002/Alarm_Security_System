package com.security.alarm.controller;

import com.security.alarm.entity.Notification;
import com.security.alarm.entity.User;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.service.NotificationService;
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository,
                                  PermissionService permissionService) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    // ============================================================
    // GET NOTIFICATIONS
    // ============================================================
    
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam String username,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        User user = userOpt.get();
        
        if (!username.equals(user.getUsername()) && !permissionService.isAdmin(username)) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<Notification> notifications = notificationService.getUserNotifications(
            user.getId(), unreadOnly, page, size
        );
        
        // ===== FIX: Break Hibernate proxies before serialization =====
        List<Map<String, Object>> notificationList = notifications.stream()
            .map(n -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", n.getId());
                map.put("type", n.getType());
                map.put("title", n.getTitle());
                map.put("message", n.getMessage());
                map.put("severity", n.getSeverity());
                map.put("isRead", n.getIsRead());
                map.put("isArchived", n.getIsArchived());
                map.put("createdAt", n.getCreatedAt());
                map.put("readAt", n.getReadAt());
                map.put("expiresAt", n.getExpiresAt());
                map.put("actionBy", n.getActionBy());
                map.put("alertId", n.getAlertId());
                map.put("metadata", n.getMetadata());
                
                // Handle company - break proxy
                if (n.getCompany() != null) {
                    Map<String, Object> companyMap = new HashMap<>();
                    companyMap.put("id", n.getCompany().getId());
                    companyMap.put("companyName", n.getCompany().getCompanyName());
                    companyMap.put("companyCode", n.getCompany().getCompanyCode());
                    map.put("company", companyMap);
                }
                
                // Handle system - break proxy
                if (n.getSystem() != null) {
                    Map<String, Object> systemMap = new HashMap<>();
                    systemMap.put("id", n.getSystem().getId());
                    systemMap.put("systemCode", n.getSystem().getSystemCode());
                    map.put("system", systemMap);
                }
                
                // Handle user - break proxy
                if (n.getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", n.getUser().getId());
                    userMap.put("username", n.getUser().getUsername());
                    map.put("user", userMap);
                }
                
                return map;
            })
            .collect(Collectors.toList());
        
        long unreadCount = notificationService.getUnreadCount(user.getId());
        long criticalCount = notificationService.getCriticalUnreadCount(user.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationList);
        response.put("unreadCount", unreadCount);
        response.put("criticalCount", criticalCount);
        response.put("total", notificationList.size());
        response.put("hasMore", notificationList.size() >= size);
        
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET UNREAD COUNT (for bell icon)
    // ============================================================
    
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        long count = notificationService.getUnreadCount(userOpt.get().getId());
        long critical = notificationService.getCriticalUnreadCount(userOpt.get().getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", count);
        response.put("criticalCount", critical);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // MARK SINGLE AS READ
    // ============================================================
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @RequestParam String username) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        try {
            Notification notification = notificationService.markAsRead(id, userOpt.get().getId());
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ============================================================
    // MARK MULTIPLE AS READ
    // ============================================================
    
    @PutMapping("/read-multiple")
    public ResponseEntity<?> markMultipleAsRead(
            @RequestBody Map<String, Object> request,
            @RequestParam String username) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("ids is required");
        }
        
        int count = notificationService.markMultipleAsRead(ids, userOpt.get().getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("markedCount", count);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================
    
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        int count = notificationService.markAllAsRead(userOpt.get().getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("markedCount", count);
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // DELETE NOTIFICATION
    // ============================================================
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long id,
            @RequestParam String username) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        Optional<Notification> notificationOpt = notificationService.getNotification(id, userOpt.get().getId());
        if (notificationOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Notification not found");
        }
        
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Notification deleted");
    }

    // ============================================================
    // GET NOTIFICATION BY ID
    // ============================================================
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getNotification(
            @PathVariable Long id,
            @RequestParam String username) {
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        
        Optional<Notification> notificationOpt = notificationService.getNotification(id, userOpt.get().getId());
        if (notificationOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Notification not found");
        }
        
        return ResponseEntity.ok(notificationOpt.get());
    }
}