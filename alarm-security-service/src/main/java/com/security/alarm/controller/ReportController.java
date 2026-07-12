package com.security.alarm.controller;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.User;
import com.security.alarm.entity.UserSystem;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.UserSystemRepository;
import com.security.alarm.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, allowCredentials = "false")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlertLogRepository alertLogRepository;

    public ReportController(ReportService reportService,
                            UserRepository userRepository,
                            UserSystemRepository userSystemRepository,
                            AlarmSystemRepository alarmSystemRepository,
                            AlertLogRepository alertLogRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
    }

    // ============================================================
    // 1. SUMMARY REPORT
    // ============================================================
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode) {
        
        LocalDateTime fromDate = parseDate(from);
        LocalDateTime toDate = parseDate(to);
        String role = "ADMIN";
        
        List<AlertLog> alerts = getAlerts(fromDate, toDate, username, systemCode);
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                role = userOpt.get().getRole();
            }
        }
        
        Map<String, Object> summary = reportService.generateSummary(alerts, username != null ? username : "System", role);
        return ResponseEntity.ok(summary);
    }

    // ============================================================
    // 2. DETAILED REPORT - Full Alert List
    // ============================================================
    @GetMapping("/detailed")
    public ResponseEntity<?> getDetailed(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status) {
        
        LocalDateTime fromDate = parseDate(from);
        LocalDateTime toDate = parseDate(to);
        
        List<AlertLog> alerts = getAlerts(fromDate, toDate, username, systemCode);
        
        // Filter by status if provided
        if (status != null && !status.trim().isEmpty()) {
            alerts = alerts.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return ResponseEntity.ok(alerts);
    }

    // ============================================================
    // 3. SYSTEM HEALTH
    // ============================================================
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth(@RequestParam(required = false) String username) {
        List<AlarmSystem> systems;
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                List<UserSystem> userSystems = userSystemRepository.findAllByUserId(userOpt.get().getId());
                List<Long> systemIds = new ArrayList<>();
                for (UserSystem us : userSystems) {
                    systemIds.add(us.getSystemId());
                }
                if (!systemIds.isEmpty()) {
                    systems = alarmSystemRepository.findAllById(systemIds);
                } else {
                    systems = new ArrayList<>();
                }
            } else {
                systems = alarmSystemRepository.findAll();
            }
        } else {
            systems = alarmSystemRepository.findAll();
        }
        
        Map<String, Object> health = reportService.generateSystemHealth(systems);
        return ResponseEntity.ok(health);
    }

    // ============================================================
    // 4. USER PERFORMANCE
    // ============================================================
    @GetMapping("/performance")
    public ResponseEntity<?> getUserPerformance(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username) {
        
        LocalDateTime fromDate = parseDate(from);
        LocalDateTime toDate = parseDate(to);
        
        List<AlertLog> alerts = getAlerts(fromDate, toDate, username, null);
        
        // Group by resolved_by
        Map<String, Long> performance = new java.util.LinkedHashMap<>();
        alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getResolvedBy() != null)
            .forEach(a -> {
                String key = a.getResolvedBy();
                performance.put(key, performance.getOrDefault(key, 0L) + 1);
            });
        
        // Calculate average resolution time per user
        Map<String, Double> avgTime = new java.util.LinkedHashMap<>();
        alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getResolvedBy() != null && a.getPendingDurationSeconds() != null)
            .forEach(a -> {
                String key = a.getResolvedBy();
                double current = avgTime.getOrDefault(key, 0.0);
                long count = performance.getOrDefault(key, 1L);
                avgTime.put(key, (current + a.getPendingDurationSeconds()) / count);
            });
        
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("resolvedBy", performance);
        response.put("averageTime", avgTime);
        response.put("totalResolved", alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count());
        response.put("totalPending", alerts.stream().filter(a -> "PENDING".equals(a.getStatus())).count());
        
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 5. EXPORT PDF
    // ============================================================
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPDF(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String reportType) {
        
        LocalDateTime fromDate = parseDate(from);
        LocalDateTime toDate = parseDate(to);
        String reportTypeStr = reportType != null ? reportType : "summary";
        
        List<AlertLog> alerts = getAlerts(fromDate, toDate, username, systemCode);
        
        String systemName = systemCode != null && !systemCode.trim().isEmpty() ? systemCode : "All Systems";
        String userName = username != null ? username : "System";
        String role = "ADMIN";
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                role = userOpt.get().getRole();
            }
        }
        
        Map<String, Object> summary = reportService.generateSummary(alerts, userName, role);
        byte[] pdfBytes = reportService.generateProfessionalPDF(summary, fromDate, toDate, systemName, userName, role);
        
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.status(500).build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "Alarm_Report_" + reportTypeStr + "_" + 
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ============================================================
    // 6. EXPORT EXCEL
    // ============================================================
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode) {
        
        LocalDateTime fromDate = parseDate(from);
        LocalDateTime toDate = parseDate(to);
        
        List<AlertLog> alerts = getAlerts(fromDate, toDate, username, systemCode);
        
        String userName = username != null ? username : "System";
        String role = "ADMIN";
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                role = userOpt.get().getRole();
            }
        }
        
        Map<String, Object> summary = reportService.generateSummary(alerts, userName, role);
        byte[] excelBytes = reportService.generateProfessionalExcel(summary, fromDate, toDate, userName, role);
        
        if (excelBytes == null || excelBytes.length == 0) {
            return ResponseEntity.status(500).build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = "Alarm_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    // ============================================================
    // 7. GET SYSTEMS LIST
    // ============================================================
    @GetMapping("/systems")
    public ResponseEntity<?> getSystems(@RequestParam(required = false) String username) {
        List<AlarmSystem> systems;
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                List<UserSystem> userSystems = userSystemRepository.findAllByUserId(userOpt.get().getId());
                List<Long> systemIds = new ArrayList<>();
                for (UserSystem us : userSystems) {
                    systemIds.add(us.getSystemId());
                }
                if (!systemIds.isEmpty()) {
                    systems = alarmSystemRepository.findAllById(systemIds);
                } else {
                    systems = new ArrayList<>();
                }
            } else {
                systems = alarmSystemRepository.findAll();
            }
        } else {
            systems = alarmSystemRepository.findAll();
        }
        
        return ResponseEntity.ok(systems);
    }

    // ============================================================
    // HELPER: Get Alerts
    // ============================================================
    private List<AlertLog> getAlerts(LocalDateTime fromDate, LocalDateTime toDate, 
                                     String username, String systemCode) {
        List<AlertLog> alerts;
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                List<UserSystem> userSystems = userSystemRepository.findAllByUserId(userOpt.get().getId());
                List<Long> systemIds = new ArrayList<>();
                for (UserSystem us : userSystems) {
                    systemIds.add(us.getSystemId());
                }
                if (!systemIds.isEmpty()) {
                    alerts = alertLogRepository.findByAlarmSystemIdInAndReceivedAtBetween(systemIds, fromDate, toDate);
                } else {
                    alerts = new ArrayList<>();
                }
            } else {
                alerts = alertLogRepository.findByReceivedAtBetween(fromDate, toDate);
            }
        } else {
            alerts = alertLogRepository.findByReceivedAtBetween(fromDate, toDate);
        }
        
        if (systemCode != null && !systemCode.trim().isEmpty()) {
            alerts = alerts.stream()
                .filter(a -> a.getAlarmSystem() != null && 
                            systemCode.equalsIgnoreCase(a.getAlarmSystem().getSystemCode()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return alerts;
    }

    // ============================================================
    // HELPER: Parse Date with Local Timezone
    // ============================================================
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now().minusDays(30);
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now().minusDays(30);
        }
    }
}