package com.security.alarm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.User;
import com.security.alarm.entity.SavedReport;
import com.security.alarm.entity.ReportDownloadHistory;
import com.security.alarm.entity.ReportViewHistory;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.AlarmZoneRepository;
import com.security.alarm.repository.UserRepository;
import com.security.alarm.repository.SavedReportRepository;
import com.security.alarm.repository.ReportDownloadHistoryRepository;
import com.security.alarm.repository.ReportViewHistoryRepository;
import com.security.alarm.service.ReportService;
import com.security.alarm.service.PermissionService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final AlertLogRepository alertLogRepository;
    private final PermissionService permissionService;
    private final SavedReportRepository savedReportRepository;
    private final AlarmZoneRepository alarmZoneRepository;

    public ReportController(ReportService reportService,
                            UserRepository userRepository,
                            AlarmSystemRepository alarmSystemRepository,
                            AlertLogRepository alertLogRepository,
                            PermissionService permissionService,
                            SavedReportRepository savedReportRepository,
                            AlarmZoneRepository alarmZoneRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.alertLogRepository = alertLogRepository;
        this.permissionService = permissionService;
        this.savedReportRepository = savedReportRepository;
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
        List<com.security.alarm.entity.AlarmSystem> systems;
        
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
    // PREVIEW REPORT DATA
    // ============================================================
    @GetMapping("/preview")
    public ResponseEntity<?> getReportPreview(
            @RequestParam String reportType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status) {
        
        try {
            LocalDateTime fromDate = parseDate(from);
            LocalDateTime toDate = parseDate(to);
            List<AlertLog> alerts = getAlertLogsWithFilters(fromDate, toDate, username, systemCode, status);
            
            String userName = username != null ? username : "System";
            String role = "ADMIN";
            if (username != null && !username.trim().isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole();
                }
            }

            Map<String, Object> preview;
            switch (reportType.toLowerCase()) {
                case "summary":
                    preview = reportService.generateSummary(alerts, userName, role);
                    break;
                case "detailed":
                    preview = new HashMap<>();
                    preview.put("totalRecords", alerts.size());
                    preview.put("reportType", "DETAILED");
                    break;
                case "health":
                    var systems = alarmSystemRepository.findAll();
                    preview = reportService.generateSystemHealth(systems);
                    break;
                case "performance":
                    preview = generatePerformanceReport(alerts);
                    break;
                case "alert-logs":
                    preview = reportService.generateAlertLogsReport(alerts, fromDate, toDate, userName, role);
                    break;
                default:
                    preview = reportService.generateSummary(alerts, userName, role);
            }
            
            preview.put("reportType", reportType);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error loading preview: " + e.getMessage());
        }
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
        List<com.security.alarm.entity.AlarmSystem> systems;
        
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
    // 11. GENERATE & SAVE REPORT (All Types)
    // ============================================================
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateAndSaveReport(
            @RequestParam String reportType,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reportName,
            @RequestParam(defaultValue = "false") boolean saveToDb,
            HttpServletRequest request) {
        
        try {
            LocalDateTime fromDate = parseDate(from);
            LocalDateTime toDate = parseDate(to);
            
            String clientIp = getClientIp(request);
            String userName = username != null ? username : "System";
            String role = "ADMIN";
            Long userId = null;
            Long companyId = null;
            
            if (username != null && !username.isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    role = user.getRole();
                    userId = user.getId();
                    if (user.getCompany() != null) {
                        companyId = user.getCompany().getId();
                    }
                }
            }
            
            List<AlertLog> alerts = getAlertLogsWithFilters(fromDate, toDate, username, systemCode, status);
            
            Map<String, Object> reportData;
            String reportTypeLabel = reportType;
            
            switch (reportType.toLowerCase()) {
                case "summary":
                    reportData = reportService.generateSummary(alerts, userName, role);
                    reportTypeLabel = "SUMMARY";
                    break;
                case "detailed":
                    reportData = new LinkedHashMap<>();
                    reportData.put("alerts", alerts);
                    reportData.put("totalRecords", alerts.size());
                    reportData.put("reportType", "DETAILED");
                    reportTypeLabel = "DETAILED";
                    break;
                case "health":
                    var systems = alarmSystemRepository.findAll();
                    reportData = reportService.generateSystemHealth(systems);
                    reportTypeLabel = "HEALTH";
                    break;
                case "performance":
                    reportData = generatePerformanceReport(alerts);
                    reportTypeLabel = "PERFORMANCE";
                    break;
                case "alert-logs":
                    reportData = reportService.generateAlertLogsReport(alerts, fromDate, toDate, userName, role);
                    reportTypeLabel = "ALERT_LOGS";
                    break;
                default:
                    reportData = reportService.generateSummary(alerts, userName, role);
                    reportTypeLabel = "SUMMARY";
            }
            
            if (saveToDb) {
                String reportNameFinal = reportName != null ? reportName : 
                    reportTypeLabel + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                
                SavedReport saved = reportService.saveReportOnly(
                    reportData,
                    reportNameFinal,
                    reportTypeLabel,
                    userName,
                    userId,
                    companyId,
                    fromDate,
                    toDate,
                    systemCode,
                    status,
                    clientIp
                );
                
                reportData.put("savedReportId", saved.getId());
                reportData.put("savedReportName", saved.getReportName());
                reportData.put("savedAt", saved.getGeneratedAt());
            }
            
            return ResponseEntity.ok(reportData);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 12. GET SAVED REPORTS
    // ============================================================
    
    @GetMapping("/saved")
    public ResponseEntity<?> getSavedReports(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String reportType) {
        
        try {
            Long userId = null;
            Long companyId = null;
            
            if (username != null && !username.isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userId = user.getId();
                    if (user.getCompany() != null) {
                        companyId = user.getCompany().getId();
                    }
                }
            }
            
            List<SavedReport> reports = reportService.getSavedReportsWithFilters(
                username, reportType, companyId, userId
            );
            
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 13. GET SAVED REPORT BY ID
    // ============================================================
    
    @GetMapping("/saved/{id}")
    public ResponseEntity<?> getSavedReport(@PathVariable Long id) {
        try {
            Optional<SavedReport> reportOpt = reportService.getSavedReport(id);
            if (reportOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(reportOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/saved/{id}/data")
    public ResponseEntity<?> getSavedReportData(@PathVariable Long id) {
        try {
            Map<String, Object> data = reportService.getReportData(id);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 14. VIEW SAVED REPORT (with tracking)
    // ============================================================
    
    @PostMapping("/saved/{id}/view")
    public ResponseEntity<?> viewSavedReport(
            @PathVariable Long id,
            @RequestParam String username,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIp(request);
            SavedReport report = reportService.viewReport(id, username, clientIp);
            Map<String, Object> data = reportService.getReportData(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("report", report);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 15. DOWNLOAD SAVED REPORT
    // ============================================================
    
    @GetMapping("/saved/{id}/download")
    public ResponseEntity<byte[]> downloadSavedReport(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam String format, // pdf or excel
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIp(request);
            
            // Track download
            reportService.downloadReport(id, username, clientIp, format);
            
            // Get report data
            Map<String, Object> reportData = reportService.getReportData(id);
            Optional<SavedReport> reportOpt = reportService.getSavedReport(id);
            
            if (reportOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            SavedReport savedReport = reportOpt.get();
            
            // ============================================================
            // 🔥 NULL checks - Add these
            // ============================================================
            
            // If reportData is null or empty, use saved report's reportData
            if (reportData == null || reportData.isEmpty()) {
                // Try to parse from saved report
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    if (savedReport.getReportData() != null && !savedReport.getReportData().isEmpty()) {
                        reportData = mapper.readValue(savedReport.getReportData(), Map.class);
                    } else {
                        // Create empty map with basic info
                        reportData = new HashMap<>();
                        reportData.put("reportType", savedReport.getReportType());
                        reportData.put("generatedBy", savedReport.getGeneratedBy());
                        reportData.put("totalRecords", savedReport.getRecordCount() != null ? savedReport.getRecordCount() : 0);
                    }
                } catch (Exception e) {
                    reportData = new HashMap<>();
                    reportData.put("reportType", savedReport.getReportType());
                    reportData.put("generatedBy", savedReport.getGeneratedBy());
                    reportData.put("totalRecords", savedReport.getRecordCount() != null ? savedReport.getRecordCount() : 0);
                }
            }
            
            // Ensure reportType is set
            String reportType = savedReport.getReportType();
            if (reportType == null || reportType.isEmpty()) {
                reportType = "SUMMARY";
            }
            reportData.put("reportType", reportType);
            reportData.put("totalRecords", savedReport.getRecordCount() != null ? savedReport.getRecordCount() : 0);
            
            // Generate file
            byte[] fileData;
            String fileName;
            MediaType mediaType;
            
            LocalDateTime fromDate = savedReport.getDateFrom() != null ? savedReport.getDateFrom() : LocalDateTime.now().minusDays(30);
            LocalDateTime toDate = savedReport.getDateTo() != null ? savedReport.getDateTo() : LocalDateTime.now();
            String role = "ADMIN";
            
            if (username != null && !username.isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole();
                }
            }
            
            // ============================================================
            // 🔥 Generate PDF/Excel with safe data
            // ============================================================
            if ("pdf".equalsIgnoreCase(format)) {
                fileData = reportService.generateReportPDF(reportData, fromDate, toDate, reportType, username, role);
                fileName = savedReport.getReportName() + ".pdf";
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                fileData = reportService.generateReportExcel(reportData, fromDate, toDate, reportType, username, role);
                fileName = savedReport.getReportName() + ".xlsx";
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            
            if (fileData == null || fileData.length == 0) {
                return ResponseEntity.status(500).build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            
            return ResponseEntity.ok().headers(headers).body(fileData);
            
        } catch (Exception e) {
            e.printStackTrace();
            // 🔥 Log the error properly
            System.err.println("Error downloading report: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // ============================================================
    // 16. DELETE SAVED REPORT
    // ============================================================
    
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<?> deleteSavedReport(
            @PathVariable Long id,
            @RequestParam String username) {
        
        try {
            reportService.deleteSavedReport(id, username);
            return ResponseEntity.ok("Report deleted successfully");
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 17. DELETE MULTIPLE SAVED REPORTS
    // ============================================================
    
    @DeleteMapping("/saved/delete-multiple")
    public ResponseEntity<?> deleteMultipleSavedReports(
            @RequestBody List<Long> ids,
            @RequestParam String username) {
        
        try {
            reportService.deleteMultipleReports(ids, username);
            return ResponseEntity.ok("Reports deleted successfully");
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 18. GET DOWNLOAD HISTORY
    // ============================================================
    
    @GetMapping("/saved/{id}/download-history")
    public ResponseEntity<?> getDownloadHistory(@PathVariable Long id) {
        try {
            List<ReportDownloadHistory> history = reportService.getDownloadHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 19. GET VIEW HISTORY
    // ============================================================
    
    @GetMapping("/saved/{id}/view-history")
    public ResponseEntity<?> getViewHistory(@PathVariable Long id) {
        try {
            List<ReportViewHistory> history = reportService.getViewHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 20. GET REPORT STATS
    // ============================================================
    
    @GetMapping("/stats")
    public ResponseEntity<?> getReportStats(@RequestParam(required = false) String username) {
        try {
            Long companyId = null;
            if (username != null && !username.isEmpty()) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent() && userOpt.get().getCompany() != null) {
                    companyId = userOpt.get().getCompany().getId();
                }
            }
            
            Map<String, Object> stats = reportService.getReportStats(username, companyId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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
                    List<com.security.alarm.entity.AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
                    List<Long> systemIds = systems.stream()
                        .map(com.security.alarm.entity.AlarmSystem::getId)
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
                    List<com.security.alarm.entity.AlarmSystem> systems = alarmSystemRepository.findByCompanyId(companyId);
                    List<Long> systemIds = systems.stream()
                        .map(com.security.alarm.entity.AlarmSystem::getId)
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

    private Map<String, Object> generatePerformanceReport(List<AlertLog> alerts) {
        Map<String, Long> performance = new LinkedHashMap<>();
        alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getResolvedBy() != null)
            .forEach(a -> {
                String key = a.getResolvedBy();
                performance.put(key, performance.getOrDefault(key, 0L) + 1);
            });
        
        Map<String, Double> avgTime = new LinkedHashMap<>();
        alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getResolvedBy() != null && a.getPendingDurationSeconds() != null)
            .forEach(a -> {
                String key = a.getResolvedBy();
                double current = avgTime.getOrDefault(key, 0.0);
                long count = performance.getOrDefault(key, 1L);
                avgTime.put(key, (current + a.getPendingDurationSeconds()) / count);
            });
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resolvedBy", performance);
        response.put("averageTime", avgTime);
        response.put("totalResolved", alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count());
        response.put("totalPending", alerts.stream().filter(a -> "PENDING".equals(a.getStatus())).count());
        response.put("reportType", "PERFORMANCE");
        response.put("totalRecords", alerts.size());
        
        return response;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now().minusDays(30);
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now().minusDays(30);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}