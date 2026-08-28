package com.security.alarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.PdfEncodings;

import com.security.alarm.entity.AlertLog;
import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.AlarmZone;
import com.security.alarm.entity.SavedReport;
import com.security.alarm.entity.ReportDownloadHistory;
import com.security.alarm.entity.ReportViewHistory;
import com.security.alarm.repository.AlarmZoneRepository;
import com.security.alarm.repository.AlertLogRepository;
import com.security.alarm.repository.SavedReportRepository;
import com.security.alarm.repository.ReportDownloadHistoryRepository;
import com.security.alarm.repository.ReportViewHistoryRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AlarmZoneRepository alarmZoneRepository;
    private final AlertLogRepository alertLogRepository;
    private final SavedReportRepository savedReportRepository;
    private final ReportDownloadHistoryRepository downloadHistoryRepository;
    private final ReportViewHistoryRepository viewHistoryRepository;
    private final ObjectMapper objectMapper;

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(30, 58, 138);
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(239, 68, 68);
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(34, 197, 94);
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(234, 179, 8);
    private static final DeviceRgb HEADER_BG = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb REPORTED_COLOR = new DeviceRgb(52, 211, 153);

    public ReportService(AlarmZoneRepository alarmZoneRepository,
                         AlertLogRepository alertLogRepository,
                         SavedReportRepository savedReportRepository,
                         ReportDownloadHistoryRepository downloadHistoryRepository,
                         ReportViewHistoryRepository viewHistoryRepository,
                         ObjectMapper objectMapper) {
        this.alarmZoneRepository = alarmZoneRepository;
        this.alertLogRepository = alertLogRepository;
        this.savedReportRepository = savedReportRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // GENERATE SUMMARY DATA
    // ============================================================
    public Map<String, Object> generateSummary(List<AlertLog> alerts, String username, String role) {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        long total = alerts.size();
        long pending = alerts.stream().filter(a -> "PENDING".equals(a.getStatus())).count();
        long resolved = alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count();
        long call = alerts.stream().filter(a -> "CALL".equals(a.getStatus())).count();
        long armed = alerts.stream().filter(a -> "ARMED".equals(a.getStatus())).count();
        long sirenStop = alerts.stream().filter(a -> "SIREN_STOP".equals(a.getStatus())).count();
        long rejected = alerts.stream().filter(a -> "REJECTED".equals(a.getStatus())).count();
        
        summary.put("reportType", "SUMMARY");
        summary.put("totalRecords", total);
        summary.put("totalAlerts", total);
        summary.put("pending", pending);
        summary.put("resolved", resolved);
        summary.put("call", call);
        summary.put("armed", armed);
        summary.put("sirenStop", sirenStop);
        summary.put("rejected", rejected);
        summary.put("generatedBy", username);
        summary.put("userRole", role);
        
        Map<String, Long> bySystem = alerts.stream()
            .filter(a -> a.getAlarmSystem() != null)
            .collect(Collectors.groupingBy(
                a -> a.getAlarmSystem().getSystemCode(),
                Collectors.counting()
            ));
        summary.put("bySystem", bySystem);
        
        Map<String, Long> byZone = new LinkedHashMap<>();
        alerts.stream()
            .filter(a -> a.getZoneNumbers() != null && !a.getZoneNumbers().isEmpty())
            .forEach(a -> {
                String[] zones = a.getZoneNumbers().split(",");
                for (String zone : zones) {
                    String zoneName = getZoneName(a.getAlarmSystem() != null ? a.getAlarmSystem().getId() : null, zone);
                    byZone.put(zoneName, byZone.getOrDefault(zoneName, 0L) + 1);
                }
            });
        summary.put("byZone", byZone);
        
        Map<String, Long> dailyTrend = alerts.stream()
            .collect(Collectors.groupingBy(
                a -> a.getReceivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                Collectors.counting()
            ));
        summary.put("dailyTrend", dailyTrend);
        
        Map<String, Long> resolvedBy = alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getResolvedBy() != null)
            .collect(Collectors.groupingBy(
                AlertLog::getResolvedBy,
                Collectors.counting()
            ));
        summary.put("resolvedBy", resolvedBy);
        
        OptionalDouble avgTime = alerts.stream()
            .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getPendingDurationSeconds() != null)
            .mapToLong(AlertLog::getPendingDurationSeconds)
            .average();
        summary.put("avgResolutionSeconds", Math.round(avgTime.orElse(0.0) * 10.0) / 10.0);
        
        Map<String, Long> statusDist = alerts.stream()
            .collect(Collectors.groupingBy(AlertLog::getStatus, Collectors.counting()));
        summary.put("statusDistribution", statusDist);
        
        return summary;
    }

    private String getZoneName(Long systemId, String zoneNumber) {
        if (systemId == null) return "Zone " + zoneNumber;
        try {
            int zoneNum = Integer.parseInt(zoneNumber.trim());
            Optional<AlarmZone> zone = alarmZoneRepository.findByAlarmSystemIdAndZoneNumber(systemId, zoneNum);
            return zone.map(AlarmZone::getZoneName).orElse("Zone " + zoneNumber);
        } catch (NumberFormatException e) {
            return "Zone " + zoneNumber;
        }
    }

    public String getZoneNames(Long systemId, String zoneNumbers) {
        if (zoneNumbers == null || zoneNumbers.trim().isEmpty() || "00".equals(zoneNumbers.trim()) || "0".equals(zoneNumbers.trim())) {
            return "No Zone";
        }
        
        String[] zoneArray = zoneNumbers.split(",");
        List<String> zoneNames = new ArrayList<>();
        
        for (String zoneStr : zoneArray) {
            String trimmed = zoneStr.trim();
            if (trimmed.isEmpty()) continue;
            zoneNames.add(getZoneName(systemId, trimmed));
        }
        
        return zoneNames.isEmpty() ? "No Zone" : String.join(", ", zoneNames);
    }

    // ============================================================
    // GENERATE & SAVE REPORT - ALL TYPES
    // ============================================================
    
    @Transactional
    public SavedReport generateAndSaveReport(Map<String, Object> reportData,
                                             String reportName,
                                             String reportType,
                                             String username,
                                             Long userId,
                                             Long companyId,
                                             LocalDateTime from,
                                             LocalDateTime to,
                                             String systemCode,
                                             String statusFilter,
                                             String zoneFilter,
                                             String userFilter,
                                             String clientIp,
                                             byte[] fileData,
                                             String fileFormat,
                                             String fileName) {
        
        SavedReport savedReport = new SavedReport();
        savedReport.setReportName(reportName != null ? reportName : 
            reportType + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        savedReport.setReportType(reportType);
        savedReport.setGeneratedBy(username);
        savedReport.setGeneratedFromIp(clientIp);
        savedReport.setDateFrom(from);
        savedReport.setDateTo(to);
        savedReport.setSystemCode(systemCode);
        savedReport.setStatusFilter(statusFilter);
        savedReport.setZoneFilter(zoneFilter);
        savedReport.setUserFilter(userFilter);
        savedReport.setCompanyId(companyId);
        savedReport.setUserId(userId);
        savedReport.setCreatedBy(username);
        savedReport.setFileFormat(fileFormat);
        savedReport.setFileName(fileName);
        
        if (fileData != null) {
            savedReport.setFileSize((long) fileData.length);
        }
        
        Object records = reportData.get("totalRecords");
        if (records != null) {
            savedReport.setRecordCount(((Number) records).intValue());
        } else {
            Object alertLogs = reportData.get("alertLogs");
            if (alertLogs instanceof List) {
                savedReport.setRecordCount(((List<?>) alertLogs).size());
            }
        }
        
        try {
            savedReport.setReportData(objectMapper.writeValueAsString(reportData));
        } catch (Exception e) {
            e.printStackTrace();
            savedReport.setReportData("{}");
        }
        
        return savedReportRepository.save(savedReport);
    }

    // ============================================================
    // SAVE REPORT WITHOUT FILE (JSON only)
    // ============================================================
    
    @Transactional
    public SavedReport saveReportOnly(Map<String, Object> reportData,
                                      String reportName,
                                      String reportType,
                                      String username,
                                      Long userId,
                                      Long companyId,
                                      LocalDateTime from,
                                      LocalDateTime to,
                                      String systemCode,
                                      String statusFilter,
                                      String clientIp) {
        
        SavedReport savedReport = new SavedReport();
        savedReport.setReportName(reportName != null ? reportName : 
            reportType + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        savedReport.setReportType(reportType);
        savedReport.setGeneratedBy(username);
        savedReport.setGeneratedFromIp(clientIp);
        savedReport.setDateFrom(from);
        savedReport.setDateTo(to);
        savedReport.setSystemCode(systemCode);
        savedReport.setStatusFilter(statusFilter);
        savedReport.setCompanyId(companyId);
        savedReport.setUserId(userId);
        savedReport.setCreatedBy(username);
        
        Object records = reportData.get("totalRecords");
        if (records != null) {
            savedReport.setRecordCount(((Number) records).intValue());
        } else {
            Object alertLogs = reportData.get("alertLogs");
            if (alertLogs instanceof List) {
                savedReport.setRecordCount(((List<?>) alertLogs).size());
            }
        }
        
        try {
            savedReport.setReportData(objectMapper.writeValueAsString(reportData));
        } catch (Exception e) {
            e.printStackTrace();
            savedReport.setReportData("{}");
        }
        
        return savedReportRepository.save(savedReport);
    }

    // ============================================================
    // GET SAVED REPORTS
    // ============================================================
    
    public List<SavedReport> getSavedReports(String username, Long companyId, Long userId) {
        if (companyId != null) {
            return savedReportRepository.findActiveByCompanyId(companyId);
        } else if (userId != null) {
            return savedReportRepository.findActiveByUserId(userId);
        } else if (username != null) {
            return savedReportRepository.findActiveByGeneratedBy(username);
        }
        return savedReportRepository.findAllActive();
    }
    
    public List<SavedReport> getSavedReportsByType(String reportType, String username) {
        if (username != null) {
            List<SavedReport> all = savedReportRepository.findActiveByGeneratedBy(username);
            return all.stream()
                .filter(r -> r.getReportType().equals(reportType))
                .collect(Collectors.toList());
        }
        return savedReportRepository.findActiveByReportType(reportType);
    }
    
    public Optional<SavedReport> getSavedReport(Long id) {
        return savedReportRepository.findById(id);
    }
    
    public Map<String, Object> getReportData(Long reportId) {
        Optional<SavedReport> reportOpt = savedReportRepository.findById(reportId);
        if (reportOpt.isEmpty()) {
            throw new RuntimeException("Report not found");
        }
        
        SavedReport report = reportOpt.get();
        try {
            return objectMapper.readValue(report.getReportData(), Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ============================================================
    // VIEW REPORT (with tracking)
    // ============================================================
    
    @Transactional
    public SavedReport viewReport(Long reportId, String username, String clientIp) {
        Optional<SavedReport> reportOpt = savedReportRepository.findById(reportId);
        if (reportOpt.isEmpty()) {
            throw new RuntimeException("Report not found");
        }
        
        savedReportRepository.incrementViewCount(reportId);
        
        ReportViewHistory history = new ReportViewHistory();
        history.setReportId(reportId);
        history.setViewedBy(username);
        history.setViewedFromIp(clientIp);
        viewHistoryRepository.save(history);
        
        return reportOpt.get();
    }

    // ============================================================
    // DOWNLOAD REPORT (with tracking)
    // ============================================================
    
    @Transactional
    public SavedReport downloadReport(Long reportId, String username, String clientIp, String fileFormat) {
        Optional<SavedReport> reportOpt = savedReportRepository.findById(reportId);
        if (reportOpt.isEmpty()) {
            throw new RuntimeException("Report not found");
        }
        
        SavedReport report = reportOpt.get();
        savedReportRepository.incrementDownloadCount(reportId);
        
        ReportDownloadHistory history = new ReportDownloadHistory();
        history.setReportId(reportId);
        history.setDownloadedBy(username);
        history.setDownloadedFromIp(clientIp);
        history.setFileFormat(fileFormat);
        history.setFileSize(report.getFileSize());
        history.setDownloadStatus("SUCCESS");
        downloadHistoryRepository.save(history);
        
        return report;
    }

    // ============================================================
    // DELETE REPORT
    // ============================================================
    
    @Transactional
    public void deleteSavedReport(Long id, String deletedBy) {
        savedReportRepository.softDeleteById(id, LocalDateTime.now(), deletedBy);
    }
    
    @Transactional
    public void deleteMultipleReports(List<Long> ids, String deletedBy) {
        savedReportRepository.softDeleteByIds(ids, LocalDateTime.now(), deletedBy);
    }

    // ============================================================
    // GET DOWNLOAD HISTORY
    // ============================================================
    
    public List<ReportDownloadHistory> getDownloadHistory(Long reportId) {
        return downloadHistoryRepository.findByReportIdOrderByDownloadedAtDesc(reportId);
    }
    
    public List<ReportViewHistory> getViewHistory(Long reportId) {
        return viewHistoryRepository.findByReportIdOrderByViewedAtDesc(reportId);
    }

    // ============================================================
    // GENERATE PDF - All Report Types
    // ============================================================
    
    public byte[] generateReportPDF(Map<String, Object> reportData, 
                                    LocalDateTime from, 
                                    LocalDateTime to,
                                    String reportType,
                                    String username,
                                    String role) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 🔥 Null checks
            if (reportData == null) { reportData = new HashMap<>(); }
            if (reportType == null || reportType.isEmpty()) { reportType = "SUMMARY"; }
            if (from == null) { from = LocalDateTime.now().minusDays(30); }
            if (to == null) { to = LocalDateTime.now(); }
            if (username == null || username.isEmpty()) { username = "System"; }
            if (role == null || role.isEmpty()) { role = "ADMIN"; }
            
            // 🔥 Ensure required keys exist
            if (!reportData.containsKey("totalRecords")) { reportData.put("totalRecords", 0); }
            if (!reportData.containsKey("pending")) { reportData.put("pending", 0); }
            if (!reportData.containsKey("resolved")) { reportData.put("resolved", 0); }
            if (!reportData.containsKey("call")) { reportData.put("call", 0); }
            if (!reportData.containsKey("armed")) { reportData.put("armed", 0); }
            if (!reportData.containsKey("sirenStop")) { reportData.put("sirenStop", 0); }
            if (!reportData.containsKey("rejected")) { reportData.put("rejected", 0); }
            if (!reportData.containsKey("bySystem")) { reportData.put("bySystem", new HashMap<>()); }
            if (!reportData.containsKey("byZone")) { reportData.put("byZone", new HashMap<>()); }
            if (!reportData.containsKey("resolvedBy")) { reportData.put("resolvedBy", new HashMap<>()); }

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdfDoc);
            
            PdfFont font = PdfFontFactory.createFont("Helvetica", PdfEncodings.CP1252);
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold", PdfEncodings.CP1252);
            PdfFont smallFont = PdfFontFactory.createFont("Helvetica", PdfEncodings.CP1252);
            
            Paragraph title = new Paragraph("ALARM SECURITY SYSTEM")
                .setFont(boldFont).setFontSize(22).setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0);
            document.add(title);
            
            Paragraph subtitle = new Paragraph(reportType + " Report")
                .setFont(font).setFontSize(14).setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15);
            document.add(subtitle);
            
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
            
            String fromStr = from != null ? from.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
            String toStr = to != null ? to.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
            long totalRecordsVal = getLongValue(reportData, "totalRecords", "totalAlerts", 0L);
            
            String[][] infoData = {
                {"Report Type", reportType},
                {"Date Range", fromStr + " - " + toStr},
                {"Generated By", username + " (" + role + ")"},
                {"Generated On", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))},
                {"Total Records", String.valueOf(totalRecordsVal)}
            };
            
            for (String[] row : infoData) {
                Cell labelCell = new Cell().add(new Paragraph(row[0]).setFont(boldFont).setFontSize(10))
                    .setBorder(Border.NO_BORDER).setPadding(2);
                Cell valueCell = new Cell().add(new Paragraph(row[1]).setFont(font).setFontSize(10))
                    .setBorder(Border.NO_BORDER).setPadding(2);
                infoTable.addCell(labelCell);
                infoTable.addCell(valueCell);
            }
            document.add(infoTable);
            
            // ============================================================
            // 🔥 STATUS SUMMARY - FIXED
            // ============================================================
            Object statusCounts = reportData.get("statusCounts");
            if (statusCounts == null) {
                statusCounts = reportData.get("statusDistribution");
            }
            
            if ("SUMMARY".equalsIgnoreCase(reportType) || "ALERT_LOGS".equalsIgnoreCase(reportType)) {
                // 🔥 FIX: If still null or not a Map, build from individual fields
                if (statusCounts == null || !(statusCounts instanceof Map)) {
                    Map<String, Long> builtCounts = new LinkedHashMap<>();
                    builtCounts.put("PENDING", getNumberVal(reportData.get("pending")));
                    builtCounts.put("RESOLVED", getNumberVal(reportData.get("resolved")));
                    builtCounts.put("CALL", getNumberVal(reportData.get("call")));
                    builtCounts.put("ARMED", getNumberVal(reportData.get("armed")));
                    builtCounts.put("SIREN_STOP", getNumberVal(reportData.get("sirenStop")));
                    builtCounts.put("REJECTED", getNumberVal(reportData.get("rejected")));
                    statusCounts = builtCounts;
                }
            
            if (statusCounts instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> counts = (Map<String, ?>) statusCounts;
                
                Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
                
                String[] statuses = {"PENDING", "RESOLVED", "REJECTED", "SIREN_STOP", "CALL", "ARMED"};
                Color[] colors = {ACCENT_COLOR, SUCCESS_COLOR, ColorConstants.GRAY, WARNING_COLOR, 
                                    new DeviceRgb(59, 130, 246), new DeviceRgb(234, 179, 8)};
                
                for (int i = 0; i < statuses.length; i++) {
                    long count = getNumberVal(counts.get(statuses[i]));
                    Cell cell = new Cell().setBackgroundColor(colors[i]).setPadding(8)
                        .setTextAlignment(TextAlignment.CENTER);
                    cell.add(new Paragraph(String.valueOf(count)).setFont(boldFont).setFontSize(16)
                        .setFontColor(ColorConstants.WHITE));
                    cell.add(new Paragraph(statuses[i]).setFont(smallFont).setFontSize(8)
                        .setFontColor(ColorConstants.WHITE));
                    statsTable.addCell(cell);
                }
                document.add(statsTable);
            }
            } // END SUMMARY OR ALERT_LOGS CHECK
            
            // ============================================================
            // HEALTH REPORT METRICS
            // ============================================================
            if ("HEALTH".equalsIgnoreCase(reportType)) {
                Table healthTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
                
                String[] hLabels = {"Total Systems", "Active Systems", "Inactive Systems", "Total Zones", "Active Zones"};
                String[] hKeys = {"totalSystems", "activeSystems", "inactiveSystems", "totalZones", "activeZones"};
                Color[] hColors = {ColorConstants.DARK_GRAY, SUCCESS_COLOR, ColorConstants.RED, PRIMARY_COLOR, SUCCESS_COLOR};
                
                for (int i = 0; i < hLabels.length; i++) {
                    long count = getNumberVal(reportData.get(hKeys[i]));
                    Cell cell = new Cell().setBackgroundColor(hColors[i]).setPadding(8)
                        .setTextAlignment(TextAlignment.CENTER);
                    cell.add(new Paragraph(String.valueOf(count)).setFont(boldFont).setFontSize(16)
                        .setFontColor(ColorConstants.WHITE));
                    cell.add(new Paragraph(hLabels[i]).setFont(smallFont).setFontSize(8)
                        .setFontColor(ColorConstants.WHITE));
                    healthTable.addCell(cell);
                }
                document.add(healthTable);
                
                Object sysObj = reportData.get("systems");
                if (sysObj instanceof List) {
                    List<?> sysList = (List<?>) sysObj;
                    if (!sysList.isEmpty()) {
                        Paragraph sysTitle = new Paragraph("System Details")
                            .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY_COLOR).setMarginBottom(10);
                        document.add(sysTitle);
                        
                        Table sysTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 1, 1, 1}))
                            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
                        
                        String[] sysHeaders = {"System Code", "Location", "Status", "Last Changed", "Total Zones", "Active Zones"};
                        for (String h : sysHeaders) {
                            Cell hc = new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10))
                                .setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(5);
                            sysTable.addCell(hc);
                        }
                        
                        for (Object item : sysList) {
                            if (item instanceof Map) {
                                Map<String, Object> map = (Map<String, Object>) item;
                                sysTable.addCell(new Cell().add(new Paragraph(getStrVal(map, "systemCode", "-")).setFont(font).setFontSize(9)).setPadding(4));
                                sysTable.addCell(new Cell().add(new Paragraph(getStrVal(map, "location", "-")).setFont(font).setFontSize(9)).setPadding(4));
                                sysTable.addCell(new Cell().add(new Paragraph(getStrVal(map, "status", "-")).setFont(font).setFontSize(9)).setPadding(4));
                                
                                String lastChg = getStrVal(map, "lastStatusChanged", "-");
                                if (lastChg.length() > 19) lastChg = lastChg.substring(0, 19).replace("T", " ");
                                sysTable.addCell(new Cell().add(new Paragraph(lastChg).setFont(font).setFontSize(9)).setPadding(4));
                                
                                sysTable.addCell(new Cell().add(new Paragraph(String.valueOf(getNumberVal(map.get("totalZones")))).setFont(font).setFontSize(9)).setPadding(4));
                                sysTable.addCell(new Cell().add(new Paragraph(String.valueOf(getNumberVal(map.get("activeZones")))).setFont(font).setFontSize(9)).setPadding(4));
                            }
                        }
                        document.add(sysTable);
                    }
                }
            }
            
            // ============================================================
            // PERFORMANCE REPORT METRICS
            // ============================================================
            if ("PERFORMANCE".equalsIgnoreCase(reportType)) {
                Table perfTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
                
                String[] pLabels = {"Total Resolved", "Total Pending", "Fastest Resolver", "Slowest Resolver"};
                
                long totRes = getNumberVal(reportData.get("totalResolved"));
                long totPen = getNumberVal(reportData.get("totalPending"));
                String fastRes = getStrVal(reportData, "fastestResolver", "-");
                String slowRes = getStrVal(reportData, "slowestResolver", "-");
                String[] pValues = {String.valueOf(totRes), String.valueOf(totPen), fastRes, slowRes};
                Color[] pColors = {SUCCESS_COLOR, WARNING_COLOR, PRIMARY_COLOR, ColorConstants.RED};
                
                for (int i = 0; i < pLabels.length; i++) {
                    Cell cell = new Cell().setBackgroundColor(pColors[i]).setPadding(8)
                        .setTextAlignment(TextAlignment.CENTER);
                    cell.add(new Paragraph(pValues[i]).setFont(boldFont).setFontSize(16)
                        .setFontColor(ColorConstants.WHITE));
                    cell.add(new Paragraph(pLabels[i]).setFont(smallFont).setFontSize(8)
                        .setFontColor(ColorConstants.WHITE));
                    perfTable.addCell(cell);
                }
                document.add(perfTable);
                
                document.add(new Paragraph("Average Resolution Time: " + getStrVal(reportData, "averageTime", "0") + " seconds")
                    .setFont(boldFont).setFontSize(12).setMarginBottom(15));
                
                Object perfObj = reportData.get("userPerformance");
                if (perfObj instanceof List) {
                    List<?> perfList = (List<?>) perfObj;
                    if (!perfList.isEmpty()) {
                        Paragraph perfTitle = new Paragraph("User Performance Metrics")
                            .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY_COLOR).setMarginBottom(10);
                        document.add(perfTitle);
                        
                        Table pTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1}))
                            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
                        
                        String[] pHeaders = {"User", "Resolved Alerts", "Avg Time (s)", "Min Time (s)", "Max Time (s)"};
                        for (String h : pHeaders) {
                            Cell hc = new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10))
                                .setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(5);
                            pTable.addCell(hc);
                        }
                        
                        for (Object item : perfList) {
                            if (item instanceof Map) {
                                Map<String, Object> map = (Map<String, Object>) item;
                                pTable.addCell(new Cell().add(new Paragraph(getStrVal(map, "username", "-")).setFont(font).setFontSize(9)).setPadding(4));
                                pTable.addCell(new Cell().add(new Paragraph(String.valueOf(getNumberVal(map.get("resolvedCount")))).setFont(font).setFontSize(9)).setPadding(4));
                                pTable.addCell(new Cell().add(new Paragraph(String.valueOf(getNumberVal(map.get("avgTime")))).setFont(font).setFontSize(9)).setPadding(4));
                                pTable.addCell(new Cell().add(new Paragraph(String.valueOf(getNumberVal(map.get("minTime")))).setFont(font).setFontSize(9)).setPadding(4));
                                pTable.addCell(new Cell().add(new Paragraph(String.valueOf(getNumberVal(map.get("maxTime")))).setFont(font).setFontSize(9)).setPadding(4));
                            }
                        }
                        document.add(pTable);
                    }
                }
            }
            
            // ============================================================
            // BY SYSTEM
            // ============================================================
            Object bySystem = reportData.get("bySystem");
            if (bySystem instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> systemMap = (Map<String, ?>) bySystem;
                if (!systemMap.isEmpty()) {
                    Paragraph sysTitle = new Paragraph("Alerts by System")
                        .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY_COLOR).setMarginBottom(10);
                    document.add(sysTitle);
                    
                    Table sysTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1}))
                        .setWidth(UnitValue.createPercentValue(60));
                    
                    String[] sysHeaders = {"System", "Alerts", "%"};
                    for (String h : sysHeaders) {
                        Cell hc = new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10))
                            .setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(5);
                        sysTable.addCell(hc);
                    }
                    
                    long total = getLongValue(reportData, "totalRecords", "totalAlerts", 1L);
                    for (Map.Entry<String, ?> entry : systemMap.entrySet()) {
                        long val = getNumberVal(entry.getValue());
                        double pct = total > 0 ? (val * 100.0 / total) : 0;
                        sysTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getKey())).setFont(font).setFontSize(9)).setPadding(4));
                        sysTable.addCell(new Cell().add(new Paragraph(String.valueOf(val)).setFont(font).setFontSize(9)).setPadding(4));
                        sysTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFont(font).setFontSize(9)).setPadding(4));
                    }
                    document.add(sysTable);
                }
            }
            
            // ============================================================
            // BY ZONE
            // ============================================================
            Object byZone = reportData.get("byZone");
            if (byZone instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> zoneMap = (Map<String, ?>) byZone;
                if (!zoneMap.isEmpty()) {
                    Paragraph zoneTitle = new Paragraph("Alerts by Zone")
                        .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY_COLOR)
                        .setMarginTop(15).setMarginBottom(10);
                    document.add(zoneTitle);
                    
                    Table zoneTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1}))
                        .setWidth(UnitValue.createPercentValue(60));
                    
                    String[] zoneHeaders = {"Zone", "Alerts", "%"};
                    for (String h : zoneHeaders) {
                        Cell hc = new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10))
                            .setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(5);
                        zoneTable.addCell(hc);
                    }
                    
                    long total = getLongValue(reportData, "totalRecords", "totalAlerts", 1L);
                    List<Map.Entry<String, ?>> sorted = new ArrayList<>(zoneMap.entrySet());
                    sorted.sort((a, b) -> Long.compare(getNumberVal(b.getValue()), getNumberVal(a.getValue())));
                    for (Map.Entry<String, ?> entry : sorted.stream().limit(15).collect(Collectors.toList())) {
                        long val = getNumberVal(entry.getValue());
                        double pct = total > 0 ? (val * 100.0 / total) : 0;
                        zoneTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getKey())).setFont(font).setFontSize(9)).setPadding(4));
                        zoneTable.addCell(new Cell().add(new Paragraph(String.valueOf(val)).setFont(font).setFontSize(9)).setPadding(4));
                        zoneTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFont(font).setFontSize(9)).setPadding(4));
                    }
                    document.add(zoneTable);
                }
            }
            
            // ============================================================
            // RESOLVED BY
            // ============================================================
            Object resolvedBy = reportData.get("resolvedBy");
            if (resolvedBy instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> resolvedMap = (Map<String, ?>) resolvedBy;
                if (!resolvedMap.isEmpty()) {
                    Paragraph resTitle = new Paragraph("Resolved By")
                        .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY_COLOR)
                        .setMarginTop(15).setMarginBottom(10);
                    document.add(resTitle);
                    
                    Table resTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1}))
                        .setWidth(UnitValue.createPercentValue(50));
                    
                    String[] resHeaders = {"User", "Resolved", "%"};
                    for (String h : resHeaders) {
                        Cell hc = new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10))
                            .setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(5);
                        resTable.addCell(hc);
                    }
                    
                    long totalResolved = getLongValue(reportData, "resolved", null, 0L);
                    for (Map.Entry<String, ?> entry : resolvedMap.entrySet()) {
                        long val = getNumberVal(entry.getValue());
                        double pct = totalResolved > 0 ? (val * 100.0 / totalResolved) : 0;
                        resTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getKey())).setFont(font).setFontSize(9)).setPadding(4));
                        resTable.addCell(new Cell().add(new Paragraph(String.valueOf(val)).setFont(font).setFontSize(9)).setPadding(4));
                        resTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFont(font).setFontSize(9)).setPadding(4));
                    }
                    document.add(resTable);
                }
            }
            
            // ============================================================
            // ALERT LOGS TABLE (if present)
            // ============================================================
            Object logsObj = reportData.get("alertLogs");
            if (logsObj == null) {
                logsObj = reportData.get("alerts");
            }
            List<Map<String, Object>> alertList = extractAlertList(logsObj);
            
            if (!alertList.isEmpty()) {
                Paragraph logsTitle = new Paragraph("Alert Records (" + alertList.size() + " Total)")
                    .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY_COLOR)
                    .setMarginTop(15).setMarginBottom(10);
                document.add(logsTitle);
                
                Table table = new Table(UnitValue.createPercentArray(new float[]{0.8f, 1.6f, 1.4f, 1f, 1.8f, 1.2f, 1.2f, 1.8f, 1.2f, 0.8f}))
                    .setWidth(UnitValue.createPercentValue(100));
                
                String[] headers = {"ID", "System", "Location", "Zones", "Zone Names", "Type", "Status", "Received", "Resolved By", "Reported"};
                for (String h : headers) {
                    Cell hc = new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(8))
                        .setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(4);
                    table.addCell(hc);
                }
                
                for (Map<String, Object> log : alertList) {
                    String idStr = getStrVal(log, "id", "-");
                    
                    String sysCode = getMapVal(log.get("system"), "systemCode", "-");
                    if ("-".equals(sysCode)) {
                        sysCode = getMapVal(log.get("alarmSystem"), "systemCode", "-");
                    }
                    String locStr = getMapVal(log.get("system"), "location", "-");
                    if ("-".equals(locStr)) {
                        locStr = getMapVal(log.get("alarmSystem"), "location", "-");
                    }
                    
                    String zones = getStrVal(log, "zoneNumbers", getStrVal(log, "zoneNumber", "-"));
                    String zNames = getStrVal(log, "zoneNames", "-");
                    String typeStr = getStrVal(log, "alertType", "-");
                    String statusStr = getStrVal(log, "status", "-");
                    String rAt = getStrVal(log, "receivedAt", "-");
                    if (rAt.length() > 19) rAt = rAt.substring(0, 19).replace("T", " ");
                    String rBy = getStrVal(log, "resolvedBy", "-");
                    
                    Object isRepObj = log.get("isReported");
                    String reportedStr = (isRepObj instanceof Boolean && (Boolean) isRepObj) ? "Yes" : "No";
                    
                    table.addCell(new Cell().add(new Paragraph(idStr).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(sysCode).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(locStr).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(zones).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(zNames).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(typeStr).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(statusStr).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(rAt).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(rBy).setFont(font).setFontSize(7)).setPadding(3));
                    table.addCell(new Cell().add(new Paragraph(reportedStr).setFont(font).setFontSize(7)).setPadding(3));
                }
                
                document.add(table);
            }
            
            Paragraph footer = new Paragraph("Confidential - For authorized use only")
                .setFont(font).setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(30);
            document.add(footer);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    // ============================================================
    // GENERATE EXCEL - All Report Types
    // ============================================================
    
    public byte[] generateReportExcel(Map<String, Object> reportData,
                                      LocalDateTime from,
                                      LocalDateTime to,
                                      String reportType,
                                      String username,
                                      String role) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook();
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle greenStyle = createGreenStyle(workbook);
            CellStyle redStyle = createRedStyle(workbook);
            CellStyle yellowStyle = createYellowStyle(workbook);
            CellStyle blueStyle = createBlueStyle(workbook);
            
            Sheet sheet = workbook.createSheet(reportType);
            int rowNum = 0;
            
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ALARM SECURITY SYSTEM - " + reportType + " REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowNum++;
            
            String fromStrExcel = from != null ? from.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
            String toStrExcel = to != null ? to.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
            
            String[][] infoData = {
                {"Report Type", reportType},
                {"Date Range", fromStrExcel + " - " + toStrExcel},
                {"Generated By", username + " (" + role + ")"},
                {"Generated On", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))}
            };
            
            for (String[] rowData : infoData) {
                Row r = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell labelCell = r.createCell(0);
                labelCell.setCellValue(rowData[0]);
                labelCell.setCellStyle(headerStyle);
                org.apache.poi.ss.usermodel.Cell valueCell = r.createCell(1);
                valueCell.setCellValue(rowData[1]);
            }
            rowNum++;
            
            Row statsHeader = sheet.createRow(rowNum++);
            String[] statsHeaders = {"Metric", "Value"};
            for (int i = 0; i < statsHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = statsHeader.createCell(i);
                cell.setCellValue(statsHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            
            Object[][] statsData = {
                {"Total Alerts", reportData.getOrDefault("totalAlerts", reportData.getOrDefault("totalRecords", 0)), blueStyle},
                {"Pending", reportData.getOrDefault("pending", 0), redStyle},
                {"Resolved", reportData.getOrDefault("resolved", 0), greenStyle},
                {"CALL/ARMED", getLongValue(reportData, "call", null, 0L) + getLongValue(reportData, "armed", null, 0L), yellowStyle}
            };
            
            for (Object[] rowData : statsData) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue((String) rowData[0]);
                org.apache.poi.ss.usermodel.Cell valCell = r.createCell(1);
                valCell.setCellValue(String.valueOf(rowData[1]));
                valCell.setCellStyle((CellStyle) rowData[2]);
            }
            rowNum += 2;
            
            Object bySystemObj = reportData.get("bySystem");
            if (bySystemObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> bySystem = (Map<String, ?>) bySystemObj;
                if (!bySystem.isEmpty()) {
                    Row sysTitle = sheet.createRow(rowNum++);
                    sysTitle.createCell(0).setCellValue("ALERTS BY SYSTEM");
                    sysTitle.getCell(0).setCellStyle(headerStyle);
                    
                    Row sysHeader = sheet.createRow(rowNum++);
                    sysHeader.createCell(0).setCellValue("System");
                    sysHeader.createCell(1).setCellValue("Alerts");
                    sysHeader.createCell(2).setCellValue("%");
                    sysHeader.getCell(0).setCellStyle(headerStyle);
                    sysHeader.getCell(1).setCellStyle(headerStyle);
                    sysHeader.getCell(2).setCellStyle(headerStyle);
                    
                    long total = getLongValue(reportData, "totalAlerts", "totalRecords", 1L);
                    for (Map.Entry<String, ?> entry : bySystem.entrySet()) {
                        long val = getNumberVal(entry.getValue());
                        Row r = sheet.createRow(rowNum++);
                        r.createCell(0).setCellValue(String.valueOf(entry.getKey()));
                        r.createCell(1).setCellValue(val);
                        double pct = total > 0 ? (val * 100.0 / total) : 0;
                        r.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }
            }
            
            Object byZoneObj = reportData.get("byZone");
            if (byZoneObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> byZone = (Map<String, ?>) byZoneObj;
                if (!byZone.isEmpty()) {
                    rowNum += 2;
                    Row zoneTitle = sheet.createRow(rowNum++);
                    zoneTitle.createCell(0).setCellValue("ALERTS BY ZONE");
                    zoneTitle.getCell(0).setCellStyle(headerStyle);
                    
                    Row zoneHeader = sheet.createRow(rowNum++);
                    zoneHeader.createCell(0).setCellValue("Zone");
                    zoneHeader.createCell(1).setCellValue("Alerts");
                    zoneHeader.createCell(2).setCellValue("%");
                    zoneHeader.getCell(0).setCellStyle(headerStyle);
                    zoneHeader.getCell(1).setCellStyle(headerStyle);
                    zoneHeader.getCell(2).setCellStyle(headerStyle);
                    
                    long total = getLongValue(reportData, "totalAlerts", "totalRecords", 1L);
                    List<Map.Entry<String, ?>> sorted = new ArrayList<>(byZone.entrySet());
                    sorted.sort((a, b) -> Long.compare(getNumberVal(b.getValue()), getNumberVal(a.getValue())));
                    for (Map.Entry<String, ?> entry : sorted) {
                        long val = getNumberVal(entry.getValue());
                        Row r = sheet.createRow(rowNum++);
                        r.createCell(0).setCellValue(String.valueOf(entry.getKey()));
                        r.createCell(1).setCellValue(val);
                        double pct = total > 0 ? (val * 100.0 / total) : 0;
                        r.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }
            }
            
            Object resolvedByObj = reportData.get("resolvedBy");
            if (resolvedByObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> resolvedBy = (Map<String, ?>) resolvedByObj;
                if (!resolvedBy.isEmpty()) {
                    rowNum += 2;
                    Row resTitle = sheet.createRow(rowNum++);
                    resTitle.createCell(0).setCellValue("RESOLVED BY");
                    resTitle.getCell(0).setCellStyle(headerStyle);
                    
                    Row resHeader = sheet.createRow(rowNum++);
                    resHeader.createCell(0).setCellValue("User");
                    resHeader.createCell(1).setCellValue("Resolved");
                    resHeader.createCell(2).setCellValue("%");
                    resHeader.getCell(0).setCellStyle(headerStyle);
                    resHeader.getCell(1).setCellStyle(headerStyle);
                    resHeader.getCell(2).setCellStyle(headerStyle);
                    
                    long totalResolved = getLongValue(reportData, "resolved", null, 0L);
                    for (Map.Entry<String, ?> entry : resolvedBy.entrySet()) {
                        long val = getNumberVal(entry.getValue());
                        Row r = sheet.createRow(rowNum++);
                        r.createCell(0).setCellValue(String.valueOf(entry.getKey()));
                        r.createCell(1).setCellValue(val);
                        double pct = totalResolved > 0 ? (val * 100.0 / totalResolved) : 0;
                        r.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }
            }
            
            Object logsObjExcel = reportData.get("alertLogs");
            if (logsObjExcel == null) {
                logsObjExcel = reportData.get("alerts");
            }
            List<Map<String, Object>> alertListExcel = extractAlertList(logsObjExcel);
            if (!alertListExcel.isEmpty()) {
                rowNum += 2;
                Row logTitle = sheet.createRow(rowNum++);
                logTitle.createCell(0).setCellValue("ALERT LOGS DETAIL");
                logTitle.getCell(0).setCellStyle(headerStyle);
                
                Row logHeader = sheet.createRow(rowNum++);
                String[] headers = {"ID", "System", "Location", "Zones", "Zone Names", "Type", "Status", "Received", "Resolved By", "Reported"};
                for (int i = 0; i < headers.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = logHeader.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                for (Map<String, Object> log : alertListExcel) {
                    Row r = sheet.createRow(rowNum++);
                    r.createCell(0).setCellValue(getStrVal(log, "id", "-"));
                    
                    String sysCode = getMapVal(log.get("system"), "systemCode", "-");
                    if ("-".equals(sysCode)) {
                        sysCode = getMapVal(log.get("alarmSystem"), "systemCode", "-");
                    }
                    String locStr = getMapVal(log.get("system"), "location", "-");
                    if ("-".equals(locStr)) {
                        locStr = getMapVal(log.get("alarmSystem"), "location", "-");
                    }
                    r.createCell(1).setCellValue(sysCode);
                    r.createCell(2).setCellValue(locStr);
                    r.createCell(3).setCellValue(getStrVal(log, "zoneNumbers", getStrVal(log, "zoneNumber", "-")));
                    r.createCell(4).setCellValue(getStrVal(log, "zoneNames", "-"));
                    r.createCell(5).setCellValue(getStrVal(log, "alertType", "-"));
                    r.createCell(6).setCellValue(getStrVal(log, "status", "-"));
                    
                    String rAt = getStrVal(log, "receivedAt", "-");
                    if (rAt.length() > 19) rAt = rAt.substring(0, 19).replace("T", " ");
                    r.createCell(7).setCellValue(rAt);
                    
                    String rBy = getStrVal(log, "resolvedBy", "-");
                    r.createCell(8).setCellValue(rBy);
                    
                    Object isRepObj = log.get("isReported");
                    String reportedStr = (isRepObj instanceof Boolean && (Boolean) isRepObj) ? "Yes" : "No";
                    r.createCell(9).setCellValue(reportedStr);
                }
            }
            
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(baos);
            workbook.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    // ============================================================
    // STYLE CREATION METHODS
    // ============================================================
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createGreenStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.GREEN.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createRedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createYellowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.ORANGE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBlueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // ============================================================
    // GENERATE SYSTEM HEALTH
    // ============================================================
    public Map<String, Object> generateSystemHealth(List<AlarmSystem> systems) {
        Map<String, Object> health = new LinkedHashMap<>();
        
        List<Map<String, Object>> systemDetails = new ArrayList<>();
        long totalActive = 0;
        long totalInactive = 0;
        long totalZones = 0;
        long totalActiveZones = 0;
        
        for (AlarmSystem system : systems) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("systemCode", system.getSystemCode());
            detail.put("location", system.getLocation());
            detail.put("status", system.getStatus());
            detail.put("lastStatusChanged", system.getLastStatusChangedAt());
            
            if ("ACTIVE".equals(system.getStatus())) {
                totalActive++;
            } else {
                totalInactive++;
            }
            
            List<AlarmZone> zones = alarmZoneRepository.findByAlarmSystemIdOrderByZoneNumberAsc(system.getId());
            detail.put("totalZones", zones.size());
            
            long activeZones = zones.stream().filter(zone -> zone.getIsActive() != null && zone.getIsActive()).count();
            detail.put("activeZones", activeZones);
            detail.put("inactiveZones", zones.size() - activeZones);
            
            totalZones += zones.size();
            totalActiveZones += activeZones;
            
            systemDetails.add(detail);
        }
        
        health.put("totalSystems", systems.size());
        health.put("activeSystems", totalActive);
        health.put("inactiveSystems", totalInactive);
        health.put("totalZones", totalZones);
        health.put("activeZones", totalActiveZones);
        health.put("systems", systemDetails);
        
        return health;
    }

    // ============================================================
    // ALERT LOGS REPORT WITH MARKING
    // ============================================================
    
    @Transactional
    public Map<String, Object> generateAlertLogsReportWithMarking(
            List<AlertLog> alerts, 
            LocalDateTime from, 
            LocalDateTime to,
            String username,
            String role,
            String reportName,
            Long companyId,
            Long userId,
            String clientIp) {
        
        Map<String, Object> report = generateAlertLogsReport(alerts, from, to, username, role);
        
        List<Long> alertIds = alerts.stream()
            .map(AlertLog::getId)
            .collect(Collectors.toList());
        
        if (!alertIds.isEmpty()) {
            SavedReport savedReport = new SavedReport();
            savedReport.setReportName(reportName != null ? reportName : 
                "Alert_Logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            savedReport.setReportType("ALERT_LOGS");
            savedReport.setGeneratedBy(username);  // ← "admin" විය යුතුයි
            savedReport.setGeneratedFromIp(clientIp);
            savedReport.setDateFrom(from);
            savedReport.setDateTo(to);
            savedReport.setRecordCount(alerts.size());
            savedReport.setCompanyId(companyId);   // ← company_id set කරන්න
            savedReport.setUserId(userId);          // ← user_id set කරන්න
            savedReport.setCreatedBy(username);
            
            try {
                savedReport.setReportData(objectMapper.writeValueAsString(report));
            } catch (Exception e) {
                e.printStackTrace();
                savedReport.setReportData("{}");
            }
            
            SavedReport saved = savedReportRepository.save(savedReport);
            
            alertLogRepository.markAlertsAsReported(alertIds, saved.getId());
            
            report.put("reportId", saved.getId());
            report.put("markedAlertIds", alertIds);
            report.put("markedCount", alertIds.size());
            report.put("savedReport", saved);
        }
        
        return report;
    }

    // ============================================================
    // ALERT LOGS REPORT (Without marking)
    // ============================================================
    
    public Map<String, Object> generateAlertLogsReport(List<AlertLog> alerts, 
                                                       LocalDateTime from, 
                                                       LocalDateTime to,
                                                       String username,
                                                       String role) {
        Map<String, Object> report = new LinkedHashMap<>();
        
        report.put("reportType", "ALERT_LOGS");
        report.put("generatedBy", username != null ? username : "System");
        report.put("userRole", role != null ? role : "ADMIN");
        report.put("totalRecords", alerts.size());
        report.put("generatedAt", LocalDateTime.now().toString());
        
        Map<String, Long> statusCounts = alerts.stream()
            .collect(Collectors.groupingBy(
                a -> a.getStatus() != null ? a.getStatus() : "UNKNOWN",
                Collectors.counting()
            ));
        report.put("statusCounts", statusCounts);
        
        Map<String, Long> systemCounts = alerts.stream()
            .filter(a -> a.getAlarmSystem() != null)
            .collect(Collectors.groupingBy(
                a -> a.getAlarmSystem().getSystemCode(),
                Collectors.counting()
            ));
        report.put("systemCounts", systemCounts);
        
        List<Map<String, Object>> alertLogsList = new ArrayList<>();
        
        for (AlertLog alert : alerts) {
            Map<String, Object> log = new LinkedHashMap<>();
            
            log.put("id", alert.getId());
            log.put("alertType", alert.getAlertType());
            log.put("status", alert.getStatus());
            log.put("receivedAt", alert.getReceivedAt());
            log.put("zoneNumber", alert.getZoneNumber());
            log.put("zoneNumbers", alert.getZoneNumbers());
            
            String zNames = alert.getZoneNames();
            if ((zNames == null || zNames.isEmpty() || "-".equals(zNames) || "null".equalsIgnoreCase(zNames)) 
                    && alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                zNames = getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers());
                alert.setZoneNames(zNames);
            }
            log.put("zoneNames", (zNames != null && !zNames.isEmpty()) ? zNames : "No Zone");
            log.put("rawMessage", alert.getRawMessage());
            log.put("resolvedAt", alert.getResolvedAt());
            log.put("resolvedBy", alert.getResolvedBy());
            log.put("pendingDurationSeconds", alert.getPendingDurationSeconds());
            log.put("resolutionDescription", alert.getResolutionDescription());
            log.put("resolvedFromIp", alert.getResolvedFromIp());
            log.put("isReported", alert.getIsReported() != null && alert.getIsReported());
            log.put("reportId", alert.getReportId());
            log.put("isArchived", alert.getIsArchived() != null && alert.getIsArchived());
            
            if (alert.getAlarmSystem() != null) {
                Map<String, Object> systemMap = new LinkedHashMap<>();
                systemMap.put("id", alert.getAlarmSystem().getId());
                systemMap.put("systemCode", alert.getAlarmSystem().getSystemCode());
                systemMap.put("location", alert.getAlarmSystem().getLocation());
                systemMap.put("description", alert.getAlarmSystem().getDescription());
                systemMap.put("simNumber", alert.getAlarmSystem().getSimNumber());
                systemMap.put("status", alert.getAlarmSystem().getStatus());
                systemMap.put("sirenStatus", alert.getAlarmSystem().getSirenStatus());
                
                if (alert.getAlarmSystem().getCompany() != null) {
                    Map<String, Object> companyMap = new LinkedHashMap<>();
                    companyMap.put("id", alert.getAlarmSystem().getCompany().getId());
                    companyMap.put("companyName", alert.getAlarmSystem().getCompany().getCompanyName());
                    companyMap.put("companyCode", alert.getAlarmSystem().getCompany().getCompanyCode());
                    systemMap.put("company", companyMap);
                }
                log.put("system", systemMap);
            }
            
            alertLogsList.add(log);
        }
        
        report.put("alertLogs", alertLogsList);
        
        return report;
    }

    // ============================================================
    // GET ALL SAVED REPORTS WITH FILTERS
    // ============================================================
    
    public List<SavedReport> getSavedReportsWithFilters(String username, String reportType, 
                                                        Long companyId, Long userId) {
        List<SavedReport> reports;
        
        if (companyId != null) {
            reports = savedReportRepository.findActiveByCompanyId(companyId);
        } else if (userId != null) {
            reports = savedReportRepository.findActiveByUserId(userId);
        } else if (username != null) {
            reports = savedReportRepository.findActiveByGeneratedBy(username);
        } else {
            reports = savedReportRepository.findAllActive();
        }
        
        if (reportType != null && !reportType.isEmpty()) {
            reports = reports.stream()
                .filter(r -> r.getReportType().equals(reportType))
                .collect(Collectors.toList());
        }
        
        return reports;
    }

    // ============================================================
    // GET REPORT STATS
    // ============================================================
    
    public Map<String, Object> getReportStats(String username, Long companyId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        if (companyId != null) {
            stats.put("totalReports", savedReportRepository.countActiveByCompanyId(companyId));
        } else if (username != null) {
            stats.put("totalReports", savedReportRepository.countActiveByGeneratedBy(username));
        } else {
            stats.put("totalReports", (long) savedReportRepository.findAllActive().size());
        }
        
        List<SavedReport> reports;
        if (companyId != null) {
            reports = savedReportRepository.findActiveByCompanyId(companyId);
        } else if (username != null) {
            reports = savedReportRepository.findActiveByGeneratedBy(username);
        } else {
            reports = savedReportRepository.findAllActive();
        }
        
        Map<String, Long> typeCounts = reports.stream()
            .collect(Collectors.groupingBy(SavedReport::getReportType, Collectors.counting()));
        stats.put("reportTypeCounts", typeCounts);
        
        long totalDownloads = reports.stream()
            .mapToLong(r -> r.getDownloadCount() != null ? r.getDownloadCount() : 0)
            .sum();
        stats.put("totalDownloads", totalDownloads);
        
        return stats;
    }

    // ============================================================
    // DELEGATE METHODS FOR CONTROLLER BACKWARD COMPATIBILITY
    // ============================================================

    public byte[] generateProfessionalPDF(Map<String, Object> summary, 
                                          LocalDateTime from, 
                                          LocalDateTime to, 
                                          String systemName, 
                                          String username, 
                                          String role) {
        return generateReportPDF(summary, from, to, "Summary", username, role);
    }

    public byte[] generateProfessionalExcel(Map<String, Object> summary, 
                                            LocalDateTime from, 
                                            LocalDateTime to, 
                                            String username, 
                                            String role) {
        return generateReportExcel(summary, from, to, "Summary", username, role);
    }

    public byte[] generateAlertLogsPDF(List<AlertLog> alerts, 
                                       LocalDateTime from, 
                                       LocalDateTime to, 
                                       String username, 
                                       String role) {
        for (AlertLog alert : alerts) {
            if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                alert.setZoneNames(getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers()));
            }
        }
        Map<String, Object> reportData = generateAlertLogsReport(alerts, from, to, username, role);
        return generateReportPDF(reportData, from, to, "ALERT_LOGS", username, role);
    }

    public byte[] generateAlertLogsExcel(List<AlertLog> alerts, 
                                         LocalDateTime from, 
                                         LocalDateTime to, 
                                         String username, 
                                         String role) {
        for (AlertLog alert : alerts) {
            if (alert.getAlarmSystem() != null && alert.getZoneNumbers() != null) {
                alert.setZoneNames(getZoneNames(alert.getAlarmSystem().getId(), alert.getZoneNumbers()));
            }
        }
        Map<String, Object> reportData = generateAlertLogsReport(alerts, from, to, username, role);
        return generateReportExcel(reportData, from, to, "ALERT_LOGS", username, role);
    }

    private String getStrVal(Map<String, Object> map, String key, String defaultValue) {
        if (map != null) {
            Object val = map.get(key);
            if (val != null) {
                String str = val.toString();
                if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
                    return str;
                }
            }
        }
        return defaultValue;
    }

    private String getMapVal(Object mapObj, String key, String defaultValue) {
        if (mapObj instanceof Map) {
            Object val = ((Map<?, ?>) mapObj).get(key);
            if (val != null) {
                String str = val.toString();
                if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
                    return str;
                }
            }
        }
        return defaultValue;
    }

    private List<Map<String, Object>> extractAlertList(Object logsObj) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (logsObj instanceof List) {
            List<?> list = (List<?>) logsObj;
            for (Object item : list) {
                if (item instanceof AlertLog) {
                    AlertLog a = (AlertLog) item;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", a.getId());
                    map.put("alertType", a.getAlertType());
                    map.put("status", a.getStatus());
                    map.put("receivedAt", a.getReceivedAt() != null ? a.getReceivedAt().toString() : "");
                    map.put("zoneNumbers", a.getZoneNumbers());
                    
                    String zNames = a.getZoneNames();
                    if ((zNames == null || zNames.isEmpty() || "-".equals(zNames) || "null".equalsIgnoreCase(zNames))
                            && a.getAlarmSystem() != null && a.getZoneNumbers() != null) {
                        zNames = getZoneNames(a.getAlarmSystem().getId(), a.getZoneNumbers());
                        a.setZoneNames(zNames);
                    }
                    map.put("zoneNames", (zNames != null && !zNames.isEmpty()) ? zNames : "No Zone");
                    
                    map.put("resolvedBy", a.getResolvedBy() != null ? a.getResolvedBy() : "-");
                    map.put("isReported", a.getIsReported() != null && a.getIsReported());
                    if (a.getAlarmSystem() != null) {
                        Map<String, Object> sys = new LinkedHashMap<>();
                        sys.put("id", a.getAlarmSystem().getId());
                        sys.put("systemCode", a.getAlarmSystem().getSystemCode());
                        sys.put("location", a.getAlarmSystem().getLocation());
                        map.put("system", sys);
                    }
                    result.add(map);
                } else if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) item);
                    
                    String zNames = getStrVal(map, "zoneNames", "");
                    if (zNames.isEmpty() || "-".equals(zNames) || "null".equalsIgnoreCase(zNames)) {
                        Long systemId = null;
                        Object sysObj = map.get("system");
                        if (sysObj instanceof Map) {
                            Object idObj = ((Map<?, ?>) sysObj).get("id");
                            if (idObj instanceof Number) systemId = ((Number) idObj).longValue();
                        } else if (map.get("alarmSystem") instanceof Map) {
                            Object idObj = ((Map<?, ?>) map.get("alarmSystem")).get("id");
                            if (idObj instanceof Number) systemId = ((Number) idObj).longValue();
                        }
                        
                        String zNumbers = getStrVal(map, "zoneNumbers", getStrVal(map, "zoneNumber", ""));
                        if (!zNumbers.isEmpty() && !"00".equals(zNumbers)) {
                            zNames = getZoneNames(systemId, zNumbers);
                            map.put("zoneNames", zNames);
                        } else {
                            map.put("zoneNames", "No Zone");
                        }
                    }
                    result.add(map);
                }
            }
        }
        return result;
    }

    private long getLongValue(Map<String, Object> map, String key1, String key2, long defaultValue) {
        if (map == null) return defaultValue;
        Object val = map.get(key1);
        if (val == null && key2 != null) {
            val = map.get(key2);
        }
        return getNumberVal(val != null ? val : defaultValue);
    }

    private long getNumberVal(Object val) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val != null) {
            try {
                return Long.parseLong(val.toString());
            } catch (Exception ignored) {}
        }
        return 0L;
    }
}