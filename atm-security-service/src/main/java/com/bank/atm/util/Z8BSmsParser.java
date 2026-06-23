package com.bank.atm.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class Z8BSmsParser {
    
    private static final Pattern ZONE_PATTERN = Pattern.compile("Zone[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALERT_TYPE_PATTERN = Pattern.compile("(Fire|Smoke|Burglary|Intrusion|Tamper|Door Open|Window Break|Motion|Glass Break|Gas|Flood|Temperature|Power Failure|Low Battery|SOS|Panic|Medical|Emergency)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZONE_NAME_PATTERN = Pattern.compile("(Zone|Zona)\\s*[:\\s]*\\d+\\s*[-:]?\\s*([A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE);
    
    private static final Map<String, String> ALERT_TYPE_MAP = new HashMap<>();
    
    static {
        ALERT_TYPE_MAP.put("0", "DISABLED");
        ALERT_TYPE_MAP.put("1", "PERIMETER_ALERT");
        ALERT_TYPE_MAP.put("2", "DELAY_ALARM");
        ALERT_TYPE_MAP.put("3", "AWAY_ALARM");
        ALERT_TYPE_MAP.put("4", "24HOUR_ALARM");
        ALERT_TYPE_MAP.put("5", "MUTE_ALARM");
        ALERT_TYPE_MAP.put("6", "EXIT_BUTTON");
        ALERT_TYPE_MAP.put("7", "DOOR_BELL");
        ALERT_TYPE_MAP.put("8", "SOS_ALARM");
    }
    
    public ParsedAlert parse(String smsContent) {
        log.info("Parsing SMS: {}", smsContent);
        
        ParsedAlert parsed = new ParsedAlert();
        parsed.setRawMessage(smsContent);
        parsed.setZoneNumber(0);
        parsed.setAlertType("UNKNOWN");
        parsed.setConfidence(0.0);
        
        try {
            Matcher zoneMatcher = ZONE_PATTERN.matcher(smsContent);
            if (zoneMatcher.find()) {
                parsed.setZoneNumber(Integer.parseInt(zoneMatcher.group(1)));
                parsed.setConfidence(parsed.getConfidence() + 0.3);
            }
            
            Matcher alertMatcher = ALERT_TYPE_PATTERN.matcher(smsContent);
            if (alertMatcher.find()) {
                String alertType = alertMatcher.group(1).toUpperCase();
                parsed.setAlertType(alertType);
                parsed.setConfidence(parsed.getConfidence() + 0.5);
            }
            
            Matcher nameMatcher = ZONE_NAME_PATTERN.matcher(smsContent);
            if (nameMatcher.find()) {
                String zoneName = nameMatcher.group(2).trim();
                parsed.setZoneName(zoneName);
                parsed.setConfidence(parsed.getConfidence() + 0.2);
            }
            
            if (smsContent.toLowerCase().contains("power failure") || 
                smsContent.toLowerCase().contains("power cut")) {
                parsed.setAlertType("POWER_FAILURE");
                parsed.setConfidence(parsed.getConfidence() + 0.3);
            }
            
            if (smsContent.toLowerCase().contains("tamper") || 
                smsContent.toLowerCase().contains("tampering")) {
                parsed.setAlertType("TAMPER_ALERT");
                parsed.setConfidence(parsed.getConfidence() + 0.3);
            }
            
            if (smsContent.toLowerCase().contains("sos") || 
                smsContent.toLowerCase().contains("panic")) {
                parsed.setAlertType("SOS_ALARM");
                parsed.setConfidence(parsed.getConfidence() + 0.3);
            }
            
            for (Map.Entry<String, String> entry : ALERT_TYPE_MAP.entrySet()) {
                if (smsContent.contains("Function:" + entry.getKey()) || 
                    smsContent.contains("[F" + entry.getKey() + "]")) {
                    parsed.setAlertType(entry.getValue());
                    parsed.setConfidence(parsed.getConfidence() + 0.2);
                    break;
                }
            }
            
            if (smsContent.toLowerCase().contains("ac power") || 
                smsContent.toLowerCase().contains("power fail")) {
                parsed.setAlertType("POWER_FAILURE");
                parsed.setConfidence(parsed.getConfidence() + 0.2);
            }
            
        } catch (Exception e) {
            log.error("Error parsing SMS: {}", e.getMessage());
        }
        
        log.info("Parsed result: zone={}, type={}, confidence={}", 
            parsed.getZoneNumber(), parsed.getAlertType(), parsed.getConfidence());
        
        return parsed;
    }
    
    @Data
    public static class ParsedAlert {
        private int zoneNumber;
        private String zoneName;
        private String alertType;
        private String rawMessage;
        private double confidence;
        
        public ParsedAlert() {
            this.zoneNumber = 0;
            this.alertType = "UNKNOWN";
            this.rawMessage = "";
            this.confidence = 0.0;
        }
        
        public ParsedAlert(int zoneNumber, String alertType) {
            this.zoneNumber = zoneNumber;
            this.alertType = alertType;
            this.rawMessage = "";
            this.confidence = 0.5;
        }
        
        public boolean isHighConfidence() {
            return confidence >= 0.7;
        }
        
        public String getDisplayAlertType() {
            return alertType.replace("_", " ").toUpperCase();
        }
    }
}