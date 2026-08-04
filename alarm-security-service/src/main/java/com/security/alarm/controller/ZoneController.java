package com.security.alarm.controller;

import com.security.alarm.entity.AlarmSystem;
import com.security.alarm.entity.AlarmZone;
import com.security.alarm.repository.AlarmSystemRepository;
import com.security.alarm.repository.AlarmZoneRepository;
import com.security.alarm.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/zones")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, allowCredentials = "false")
public class ZoneController {

    private final AlarmZoneRepository alarmZoneRepository;
    private final AlarmSystemRepository alarmSystemRepository;
    private final PermissionService permissionService;

    public ZoneController(AlarmZoneRepository alarmZoneRepository,
                          AlarmSystemRepository alarmSystemRepository,
                          PermissionService permissionService) {
        this.alarmZoneRepository = alarmZoneRepository;
        this.alarmSystemRepository = alarmSystemRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("/system/{systemId}")
    public ResponseEntity<?> getZonesBySystem(@PathVariable Long systemId,
                                              @RequestParam(required = false) String username) {
        // Permission check
        if (username != null && !username.isEmpty()) {
            if (!permissionService.canAccessSystem(username, systemId)) {
                return ResponseEntity.status(403).body("Access denied: You can only access systems in your company");
            }
        }
        
        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
        if (systemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<AlarmZone> zones = alarmZoneRepository.findByAlarmSystemIdOrderByZoneNumberAsc(systemId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("systemId", systemId);
        response.put("systemCode", systemOpt.get().getSystemCode());
        response.put("zones", zones);
        
        long wirelessCount = zones.stream().filter(z -> "WIRELESS".equals(z.getZoneCategory())).count();
        long wiredCount = zones.stream().filter(z -> "WIRED".equals(z.getZoneCategory())).count();
        long activeCount = zones.stream().filter(z -> z.getIsActive()).count();
        long inactiveCount = zones.stream().filter(z -> !z.getIsActive()).count();
        
        response.put("wirelessCount", wirelessCount);
        response.put("wiredCount", wiredCount);
        response.put("activeCount", activeCount);
        response.put("inactiveCount", inactiveCount);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{zoneId}")
    public ResponseEntity<?> updateZone(@PathVariable Long zoneId,
                                        @RequestBody Map<String, Object> payload,
                                        @RequestParam(required = false) String username) {
        try {
            Optional<AlarmZone> zoneOpt = alarmZoneRepository.findById(zoneId);
            if (zoneOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            AlarmZone zone = zoneOpt.get();
            
            // Permission check - check system ownership
            if (username != null && !username.isEmpty() && zone.getAlarmSystem() != null) {
                if (!permissionService.canManageSystem(username, zone.getAlarmSystem().getId())) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }
            
            if (payload.containsKey("zoneName")) {
                Object nameObj = payload.get("zoneName");
                if (nameObj != null) {
                    String newName = nameObj.toString();
                    if (!newName.trim().isEmpty()) {
                        zone.setZoneName(newName.trim());
                    }
                }
            }
            
            if (payload.containsKey("zoneType")) {
                Object typeObj = payload.get("zoneType");
                if (typeObj != null) {
                    try {
                        Integer newType = Integer.parseInt(typeObj.toString());
                        if (newType >= 0 && newType <= 8) {
                            zone.setZoneType(newType);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid type
                    }
                }
            }
            
            if (payload.containsKey("isActive")) {
                Object activeObj = payload.get("isActive");
                if (activeObj != null) {
                    Boolean isActive = Boolean.parseBoolean(activeObj.toString());
                    zone.setIsActive(isActive);
                }
            }
            
            if (payload.containsKey("description")) {
                Object descObj = payload.get("description");
                if (descObj != null) {
                    zone.setDescription(descObj.toString());
                }
            }
            
            zone.setUpdatedAt(LocalDateTime.now());
            
            AlarmZone updated = alarmZoneRepository.save(zone);
            
            if (updated.getAlarmSystem() != null) {
                Long systemId = updated.getAlarmSystem().getId();
                alarmSystemRepository.findById(systemId).ifPresent(updated::setAlarmSystem);
            }
            
            return ResponseEntity.ok(updated);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/system/{systemId}/reset")
    public ResponseEntity<?> resetZonesToDefault(@PathVariable Long systemId,
                                                 @RequestParam(required = false) String username) {
        // Permission check
        if (username != null && !username.isEmpty()) {
            if (!permissionService.canManageSystem(username, systemId)) {
                return ResponseEntity.status(403).body("Access denied");
            }
        }
        
        Optional<AlarmSystem> systemOpt = alarmSystemRepository.findById(systemId);
        if (systemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        alarmZoneRepository.deleteBySystemId(systemId);
        
        String[] wirelessZoneNames = {
            "Main Entrance", "Cash Counter", "Lobby", "Server Room",
            "Back Office", "Vault Room", "Emergency Exit", "Parking Area",
            "Store Room", "Rest Room", "Corridor 1", "Corridor 2",
            "Main Hall", "Conference Room", "Security Room", "Generator Room"
        };

        String[] wiredZoneNames = {
            "Wired Zone 1", "Wired Zone 2", "Wired Zone 3", "Wired Zone 4",
            "Wired Zone 5", "Wired Zone 6", "Wired Zone 7", "Wired Zone 8"
        };

        for (int i = 0; i < wirelessZoneNames.length; i++) {
            AlarmZone zone = new AlarmZone();
            zone.setAlarmSystem(systemOpt.get());
            zone.setZoneNumber(i + 1);
            zone.setZoneName(wirelessZoneNames[i]);
            zone.setZoneType(1);
            zone.setIsActive(true);
            zone.setZoneCategory("WIRELESS");
            zone.setDescription("Wireless zone " + (i + 1));
            alarmZoneRepository.save(zone);
        }

        for (int i = 0; i < wiredZoneNames.length; i++) {
            AlarmZone zone = new AlarmZone();
            zone.setAlarmSystem(systemOpt.get());
            zone.setZoneNumber(i + 17);
            zone.setZoneName(wiredZoneNames[i]);
            zone.setZoneType(1);
            zone.setIsActive(true);
            zone.setZoneCategory("WIRED");
            zone.setDescription("Wired zone " + (i + 1));
            alarmZoneRepository.save(zone);
        }

        List<AlarmZone> zones = alarmZoneRepository.findByAlarmSystemIdOrderByZoneNumberAsc(systemId);
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getZoneTypes() {
        List<Map<String, Object>> types = List.of(
            Map.of("value", 0, "label", "OFF", "description", "Zone disabled"),
            Map.of("value", 1, "label", "PERIMETER", "description", "Perimeter zone"),
            Map.of("value", 2, "label", "DELAY", "description", "Delay zone"),
            Map.of("value", 3, "label", "AWAY", "description", "Away/Part alarm"),
            Map.of("value", 4, "label", "24HR", "description", "24 hours zone"),
            Map.of("value", 5, "label", "MUTE", "description", "Mute zone"),
            Map.of("value", 6, "label", "EXIT", "description", "Exit button zone"),
            Map.of("value", 7, "label", "BELL", "description", "Door bell zone"),
            Map.of("value", 8, "label", "SOS", "description", "SOS zone")
        );
        return ResponseEntity.ok(types);
    }
}