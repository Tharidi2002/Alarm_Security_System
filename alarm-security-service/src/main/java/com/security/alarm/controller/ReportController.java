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
import com.security.alarm.service.PermissionService;
import com.security.alarm.repository.AlarmZoneRepository;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final UserSystemRepository userSystemRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlertLogRepository alertLogRepository;
    private final PermissionService permissionService;
    private final AlarmZoneRepository alarmZoneRepository;

    public ReportController(ReportService reportService,
                            UserRepository userRepository,
                            UserSystemRepository userSystemRepository,
                            AlarmSystemRepository alarmSystemRepository,
                            AlertLogRepository alertLogRepository,
                            PermissionService permissionService,
                            AlarmZoneRepository alarmZoneRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.userSystemRepository = userSystemRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
        this.permissionService = permissionService;
        this.alarmZoneRepository = alarmZoneRepository;
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
    // 2. DETAILED REPORT
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
                Long companyId = userOpt.get().getCompany() != null ? 
                    userOpt.get().getCompany().getId() : null;
                if (companyId != null) {
                    systems = alarmSystemRepository.findByCompanyId(companyId);
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
        
        Map<String, Long> performance = new java.util.LinkedHashMap<>();
        alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getResolvedBy() != null)
            .forEach(a -> {
                String key = a.getResolvedBy();
                performance.put(key, performance.getOrDefault(key, 0L) + 1);
            });
        
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
                Long companyId = userOpt.get().getCompany() != null ? 
                    userOpt.get().getCompany().getId() : null;
                if (companyId != null) {
                    systems = alarmSystemRepository.findByCompanyId(companyId);
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
    // 8. ALERT LOGS REPORT
    // ============================================================
    @GetMapping("/alert-logs")
    public ResponseEntity<?> getAlertLogsReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            LocalDateTime fromDate = parseDate(from);
            LocalDateTime toDate = parseDate(to);
            
            List<AlertLog> alerts = getAlertLogsWithFilters(fromDate, toDate, username, systemCode, status);
            
            int start = page * size;
            int end = Math.min(start + size, alerts.size());
            List<AlertLog> paginatedAlerts = alerts.subList(start, end);
            
            for (AlertLog alert : paginatedAlerts) {
                if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                    String zoneNames = getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers());
                    alert.setZoneNames(zoneNames);
                } else {
                    alert.setZoneNames("No Zone");
                }
            }
            
            String role = "ADMIN";
            if (username != null && !username.trim().isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole();
                }
            }
            
            Map<String, Object> report = reportService.generateAlertLogsReport(
                paginatedAlerts, fromDate, toDate, username, role
            );
            
            report.put("page", page);
            report.put("size", size);
            report.put("totalPages", (int) Math.ceil((double) alerts.size() / size));
            report.put("totalRecords", alerts.size());
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 9. ALERT LOGS EXPORT PDF
    // ============================================================
    @GetMapping("/alert-logs/export/pdf")
    public ResponseEntity<byte[]> exportAlertLogsPDF(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status) {
        
        try {
            LocalDateTime fromDate = parseDate(from);
            LocalDateTime toDate = parseDate(to);
            
            List<AlertLog> alerts = getAlertLogsWithFilters(fromDate, toDate, username, systemCode, status);
            
            for (AlertLog alert : alerts) {
                if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                    String zoneNames = getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers());
                    alert.setZoneNames(zoneNames);
                } else {
                    alert.setZoneNames("No Zone");
                }
            }
            
            String role = "ADMIN";
            if (username != null && !username.trim().isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole();
                }
            }
            
            byte[] pdfBytes = reportService.generateAlertLogsPDF(
                alerts, fromDate, toDate, 
                username != null ? username : "System", 
                role
            );
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(500).build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "Alert_Logs_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // ============================================================
    // 10. ALERT LOGS EXPORT EXCEL
    // ============================================================
    @GetMapping("/alert-logs/export/excel")
    public ResponseEntity<byte[]> exportAlertLogsExcel(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status) {
        
        try {
            LocalDateTime fromDate = parseDate(from);
            LocalDateTime toDate = parseDate(to);
            
            List<AlertLog> alerts = getAlertLogsWithFilters(fromDate, toDate, username, systemCode, status);
            
            for (AlertLog alert : alerts) {
                if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                    String zoneNames = getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers());
                    alert.setZoneNames(zoneNames);
                } else {
                    alert.setZoneNames("No Zone");
                }
            }
            
            String role = "ADMIN";
            if (username != null && !username.trim().isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole();
                }
            }
            
            byte[] excelBytes = reportService.generateAlertLogsExcel(
                alerts, fromDate, toDate,
                username != null ? username : "System",
                role
            );
            
            if (excelBytes == null || excelBytes.length == 0) {
                return ResponseEntity.status(500).build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "Alert_Logs_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            
            return ResponseEntity.ok().headers(headers).body(excelBytes);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    private List<AlertLog> getAlerts(LocalDateTime fromDate, LocalDateTime toDate, 
                                     String username, String systemCode) {
        List<AlertLog> alerts;
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                Long companyId = userOpt.get().getCompany() != null ? 
                    userOpt.get().getCompany().getId() : null;
                if (companyId != null) {
                    List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
                    List<Long> systemIds = systems.stream()
                        .map(AlarmSystem::getId)
                        .collect(java.util.stream.Collectors.toList());
                    if (!systemIds.isEmpty()) {
                        alerts = alertLogRepository.findByAlarmSystemIdInAndReceivedAtBetween(systemIds, fromDate, toDate);
                    } else {
                        alerts = new ArrayList<>();
                    }
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

    private List<AlertLog> getAlertLogsWithFilters(LocalDateTime fromDate, 
                                                   LocalDateTime toDate,
                                                   String username, 
                                                   String systemCode,
                                                   String status) {
        List<AlertLog> alerts;
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && "USER".equalsIgnoreCase(userOpt.get().getRole())) {
                Long companyId = userOpt.get().getCompany() != null ? 
                    userOpt.get().getCompany().getId() : null;
                if (companyId != null) {
                    List<AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
                    List<Long> systemIds = systems.stream()
                        .map(AlarmSystem::getId)
                        .collect(java.util.stream.Collectors.toList());
                    if (!systemIds.isEmpty()) {
                        alerts = alertLogRepository.findByAlarmSystemIdInAndReceivedAtBetween(
                            systemIds, fromDate, toDate
                        );
                    } else {
                        alerts = new ArrayList<>();
                    }
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
        
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            alerts = alerts.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return alerts;
    }

    private String getZoneNames(Long systemId, String zoneNumbers) {
        if (zoneNumbers == null || zoneNumbers.isEmpty() || zoneNumbers.equals("00")) {
            return "No Zone";
        }
        
        String[] zoneArray = zoneNumbers.split(",");
        List<String> zoneNames = new ArrayList<>();
        
        for (String zoneStr : zoneArray) {
            try {
                int zoneNum = Integer.parseInt(zoneStr.trim());
                Optional<com.security.alarm.entity.AlarmZone> zoneOpt = 
                    alarmZoneRepository.findByAlarmSystemIdAndZoneNumber(systemId, zoneNum);
                if (zoneOpt.isPresent()) {
                    zoneNames.add(zoneOpt.get().getZoneName());
                } else {
                    zoneNames.add("Zone " + zoneStr.trim());
                }
            } catch (NumberFormatException e) {
                zoneNames.add("Zone " + zoneStr.trim());
            }
        }
        
        return String.join(", ", zoneNames);
    }

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