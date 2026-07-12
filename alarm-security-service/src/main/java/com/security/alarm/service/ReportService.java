package com.security.alarm.service;

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
import com.security.alarm.repository.AlarmZoneRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AlarmZoneRepository alarmZoneRepository;

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(30, 58, 138);
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(239, 68, 68);
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(34, 197, 94);
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(234, 179, 8);
    private static final DeviceRgb HEADER_BG = new DeviceRgb(241, 245, 249);

    public ReportService(AlarmZoneRepository alarmZoneRepository) {
        this.alarmZoneRepository = alarmZoneRepository;
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
        
        summary.put("totalAlerts", total);
        summary.put("pending", pending);
        summary.put("resolved", resolved);
        summary.put("call", call);
        summary.put("armed", armed);
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
        summary.put("avgResolutionSeconds", avgTime.orElse(0));
        
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

    // ============================================================
    // GENERATE PROFESSIONAL PDF
    // ============================================================
    public byte[] generateProfessionalPDF(Map<String, Object> summary, 
                                          LocalDateTime from, LocalDateTime to, 
                                          String systemName, String username, String role) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4);
            Document document = new Document(pdfDoc);
            
            PdfFont font = PdfFontFactory.createFont("Helvetica", PdfEncodings.CP1252);
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold", PdfEncodings.CP1252);
            
            // Header
            Paragraph company = new Paragraph("ALARM SECURITY SYSTEM")
                .setFont(boldFont).setFontSize(22).setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0);
            document.add(company);
            
            Paragraph subtitle = new Paragraph("Professional Security Monitoring Report")
                .setFont(font).setFontSize(12).setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15);
            document.add(subtitle);
            
            Table divider = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
            Cell dividerCell = new Cell().setBackgroundColor(PRIMARY_COLOR).setHeight(2).setBorder(Border.NO_BORDER);
            divider.addCell(dividerCell);
            document.add(divider);
            
            // Report Info
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(15).setMarginBottom(15);
            
            String[][] infoData = {
                {"Report Type", "Summary Report"},
                {"Date Range", from.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + " - " + 
                              to.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))},
                {"System", systemName},
                {"Generated By", username + " (" + role + ")"},
                {"Generated On", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))}
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
            
            // Stats Cards
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);
            
            Object[][] statsData = {
                {"Total Alerts", summary.get("totalAlerts"), PRIMARY_COLOR},
                {"Pending", summary.get("pending"), ACCENT_COLOR},
                {"Resolved", summary.get("resolved"), SUCCESS_COLOR},
                {"CALL/ARMED", String.valueOf((long)summary.get("call") + (long)summary.get("armed")), WARNING_COLOR}
            };
            
            for (Object[] row : statsData) {
                Cell cell = new Cell().setBackgroundColor((DeviceRgb) row[2]).setPadding(10).setTextAlignment(TextAlignment.CENTER);
                Paragraph value = new Paragraph(String.valueOf(row[1])).setFont(boldFont).setFontSize(24)
                    .setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER);
                Paragraph label = new Paragraph((String) row[0]).setFont(font).setFontSize(10)
                    .setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER);
                cell.add(value);
                cell.add(label);
                statsTable.addCell(cell);
            }
            document.add(statsTable);
            
            // By System
            Object bySystemObj = summary.get("bySystem");
            if (bySystemObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> bySystem = (Map<String, Long>) bySystemObj;
                if (!bySystem.isEmpty()) {
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
                    
                    long total = (long) summary.get("totalAlerts");
                    for (Map.Entry<String, Long> entry : bySystem.entrySet()) {
                        double pct = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
                        sysTable.addCell(new Cell().add(new Paragraph(entry.getKey()).setFont(font).setFontSize(9)).setPadding(4));
                        sysTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue())).setFont(font).setFontSize(9)).setPadding(4));
                        sysTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFont(font).setFontSize(9)).setPadding(4));
                    }
                    document.add(sysTable);
                }
            }
            
            // By Zone
            Object byZoneObj = summary.get("byZone");
            if (byZoneObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> byZone = (Map<String, Long>) byZoneObj;
                if (!byZone.isEmpty()) {
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
                    
                    long total = (long) summary.get("totalAlerts");
                    List<Map.Entry<String, Long>> sortedZoneEntries = new ArrayList<>(byZone.entrySet());
                    sortedZoneEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                    for (Map.Entry<String, Long> entry : sortedZoneEntries.stream().limit(15).collect(Collectors.toList())) {
                        double pct = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
                        zoneTable.addCell(new Cell().add(new Paragraph(entry.getKey()).setFont(font).setFontSize(9)).setPadding(4));
                        zoneTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue())).setFont(font).setFontSize(9)).setPadding(4));
                        zoneTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFont(font).setFontSize(9)).setPadding(4));
                    }
                    document.add(zoneTable);
                }
            }
            
            // Resolved By
            Object resolvedByObj = summary.get("resolvedBy");
            if (resolvedByObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> resolvedBy = (Map<String, Long>) resolvedByObj;
                if (!resolvedBy.isEmpty()) {
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
                    
                    long totalResolved = (long) summary.get("resolved");
                    for (Map.Entry<String, Long> entry : resolvedBy.entrySet()) {
                        double pct = totalResolved > 0 ? (entry.getValue() * 100.0 / totalResolved) : 0;
                        resTable.addCell(new Cell().add(new Paragraph(entry.getKey()).setFont(font).setFontSize(9)).setPadding(4));
                        resTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue())).setFont(font).setFontSize(9)).setPadding(4));
                        resTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pct)).setFont(font).setFontSize(9)).setPadding(4));
                    }
                    document.add(resTable);
                }
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
    // GENERATE PROFESSIONAL EXCEL
    // ============================================================
    public byte[] generateProfessionalExcel(Map<String, Object> summary, 
                                            LocalDateTime from, LocalDateTime to,
                                            String username, String role) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook();
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle greenStyle = createGreenStyle(workbook);
            CellStyle redStyle = createRedStyle(workbook);
            CellStyle yellowStyle = createYellowStyle(workbook);
            CellStyle blueStyle = createBlueStyle(workbook);
            
            Sheet summarySheet = workbook.createSheet("Summary");
            int rowNum = 0;
            
            // Title
            Row titleRow = summarySheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ALARM SECURITY SYSTEM - PROFESSIONAL REPORT");
            titleCell.setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowNum++;
            
            // Report Info
            String[][] infoData = {
                {"Report Type", "Summary Report"},
                {"Date Range", from.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + " - " + 
                               to.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))},
                {"Generated By", username + " (" + role + ")"},
                {"Generated On", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))}
            };
            
            for (String[] rowData : infoData) {
                Row r = summarySheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell labelCell = r.createCell(0);
                labelCell.setCellValue(rowData[0]);
                labelCell.setCellStyle(headerStyle);
                org.apache.poi.ss.usermodel.Cell valueCell = r.createCell(1);
                valueCell.setCellValue(rowData[1]);
            }
            rowNum++;
            
            // Stats
            Row statsHeader = summarySheet.createRow(rowNum++);
            String[] statsHeaders = {"Metric", "Value"};
            for (int i = 0; i < statsHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = statsHeader.createCell(i);
                cell.setCellValue(statsHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            
            Object[][] statsData = {
                {"Total Alerts", summary.get("totalAlerts"), blueStyle},
                {"Pending", summary.get("pending"), redStyle},
                {"Resolved", summary.get("resolved"), greenStyle},
                {"CALL/ARMED", (long)summary.get("call") + (long)summary.get("armed"), yellowStyle}
            };
            
            for (Object[] rowData : statsData) {
                Row r = summarySheet.createRow(rowNum++);
                r.createCell(0).setCellValue((String) rowData[0]);
                org.apache.poi.ss.usermodel.Cell valCell = r.createCell(1);
                valCell.setCellValue(String.valueOf(rowData[1]));
                valCell.setCellStyle((CellStyle) rowData[2]);
            }
            
            // By System
            Object bySystemObj = summary.get("bySystem");
            if (bySystemObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> bySystem = (Map<String, Long>) bySystemObj;
                if (!bySystem.isEmpty()) {
                    rowNum += 2;
                    Row sysTitle = summarySheet.createRow(rowNum++);
                    sysTitle.createCell(0).setCellValue("ALERTS BY SYSTEM");
                    sysTitle.getCell(0).setCellStyle(headerStyle);
                    
                    Row sysHeader = summarySheet.createRow(rowNum++);
                    sysHeader.createCell(0).setCellValue("System");
                    sysHeader.createCell(1).setCellValue("Alerts");
                    sysHeader.createCell(2).setCellValue("%");
                    sysHeader.getCell(0).setCellStyle(headerStyle);
                    sysHeader.getCell(1).setCellStyle(headerStyle);
                    sysHeader.getCell(2).setCellStyle(headerStyle);
                    
                    long total = (long) summary.get("totalAlerts");
                    for (Map.Entry<String, Long> entry : bySystem.entrySet()) {
                        Row r = summarySheet.createRow(rowNum++);
                        r.createCell(0).setCellValue(entry.getKey());
                        r.createCell(1).setCellValue(entry.getValue());
                        double pct = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
                        r.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }
            }
            
            // By Zone
            Object byZoneObj = summary.get("byZone");
            if (byZoneObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> byZone = (Map<String, Long>) byZoneObj;
                if (!byZone.isEmpty()) {
                    rowNum += 2;
                    Row zoneTitle = summarySheet.createRow(rowNum++);
                    zoneTitle.createCell(0).setCellValue("ALERTS BY ZONE");
                    zoneTitle.getCell(0).setCellStyle(headerStyle);
                    
                    Row zoneHeader = summarySheet.createRow(rowNum++);
                    zoneHeader.createCell(0).setCellValue("Zone");
                    zoneHeader.createCell(1).setCellValue("Alerts");
                    zoneHeader.createCell(2).setCellValue("%");
                    zoneHeader.getCell(0).setCellStyle(headerStyle);
                    zoneHeader.getCell(1).setCellStyle(headerStyle);
                    zoneHeader.getCell(2).setCellStyle(headerStyle);
                    
                    long total = (long) summary.get("totalAlerts");
                    List<Map.Entry<String, Long>> sortedZoneEntries = new ArrayList<>(byZone.entrySet());
                    sortedZoneEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                    for (Map.Entry<String, Long> entry : sortedZoneEntries.stream().limit(15).collect(Collectors.toList())) {
                        Row r = summarySheet.createRow(rowNum++);
                        r.createCell(0).setCellValue(entry.getKey());
                        r.createCell(1).setCellValue(entry.getValue());
                        double pct = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
                        r.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }
            }
            
            // Resolved By
            Object resolvedByObj = summary.get("resolvedBy");
            if (resolvedByObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> resolvedBy = (Map<String, Long>) resolvedByObj;
                if (!resolvedBy.isEmpty()) {
                    rowNum += 2;
                    Row resTitle = summarySheet.createRow(rowNum++);
                    resTitle.createCell(0).setCellValue("RESOLVED BY");
                    resTitle.getCell(0).setCellStyle(headerStyle);
                    
                    Row resHeader = summarySheet.createRow(rowNum++);
                    resHeader.createCell(0).setCellValue("User");
                    resHeader.createCell(1).setCellValue("Resolved");
                    resHeader.createCell(2).setCellValue("%");
                    resHeader.getCell(0).setCellStyle(headerStyle);
                    resHeader.getCell(1).setCellStyle(headerStyle);
                    resHeader.getCell(2).setCellStyle(headerStyle);
                    
                    long totalResolved = (long) summary.get("resolved");
                    for (Map.Entry<String, Long> entry : resolvedBy.entrySet()) {
                        Row r = summarySheet.createRow(rowNum++);
                        r.createCell(0).setCellValue(entry.getKey());
                        r.createCell(1).setCellValue(entry.getValue());
                        double pct = totalResolved > 0 ? (entry.getValue() * 100.0 / totalResolved) : 0;
                        r.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }
            }
            
            for (int i = 0; i < 3; i++) {
                summarySheet.autoSizeColumn(i);
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
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
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
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
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

    public List<AlertLog> getDetailedAlerts(LocalDateTime from, LocalDateTime to, 
                                            String username, String systemCode, String status) {
        return new ArrayList<>();
    }

    public List<Map<String, Object>> generateUserPerformance(LocalDateTime from, LocalDateTime to) {
        return new ArrayList<>();
    }
}