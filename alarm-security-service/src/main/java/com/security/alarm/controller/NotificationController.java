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
        
        // Check permission - users can only see their own notifications
        if (!username.equals(user.getUsername()) && !permissionService.isAdmin(username)) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<Notification> notifications = notificationService.getUserNotifications(
            user.getId(), unreadOnly, page, size
        );
        
        long unreadCount = notificationService.getUnreadCount(user.getId());
        long criticalCount = notificationService.getCriticalUnreadCount(user.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", unreadCount);
        response.put("criticalCount", criticalCount);
        response.put("total", notifications.size());
        response.put("hasMore", notifications.size() >= size);
        
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