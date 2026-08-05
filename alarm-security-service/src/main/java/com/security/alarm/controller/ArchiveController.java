package com.security.alarm.controller;

import com.security.alarm.entity.SystemArchive;
import com.security.alarm.service.ArchiveService;
import com.security.alarm.service.PermissionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/archive")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final PermissionService permissionService;

    public ArchiveController(ArchiveService archiveService,
                             PermissionService permissionService) {
        this.archiveService = archiveService;
        this.permissionService = permissionService;
    }

    // ============================================================
    // CHECK DELETION ELIGIBILITY
    // ============================================================
    @GetMapping("/systems/{systemId}/check")
    public ResponseEntity<?> checkDeletionEligibility(@PathVariable Long systemId,
                                                      @RequestParam(required = false) String username) {
        try {
            // Permission check
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, systemId)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }

            ArchiveService.DeletionCheckResult result = archiveService.checkDeletionEligibility(systemId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("canDelete", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============================================================
    // ARCHIVE AND DELETE SYSTEM
    // ============================================================
    @PostMapping("/systems/{systemId}/archive-delete")
    public ResponseEntity<?> archiveAndDeleteSystem(@PathVariable Long systemId,
                                                    @RequestParam(required = false) String username,
                                                    @RequestParam(required = false) String deleteBy) {
        try {
            // Permission check
            if (username != null && !username.isEmpty()) {
                if (!permissionService.canManageSystem(username, systemId)) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }

            String deletedBy = deleteBy != null ? deleteBy : (username != null ? username : "SYSTEM");
            SystemArchive archive = archiveService.archiveAndDeleteSystem(systemId, deletedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "System archived and deleted successfully");
            response.put("archiveId", archive.getId());
            response.put("archive", archive);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ============================================================
    // GET ALL ARCHIVES
    // ============================================================
    @GetMapping
    public ResponseEntity<?> getAllArchives(@RequestParam(required = false) String username) {
        try {
            List<SystemArchive> archives;

            if (username != null && !username.isEmpty()) {
                if (permissionService.isUser(username)) {
                    Long companyId = permissionService.getUserCompanyId(username);
                    if (companyId != null) {
                        archives = archiveService.getArchivesByCompany(companyId);
                    } else {
                        archives = List.of();
                    }
                } else {
                    archives = archiveService.getAllArchives();
                }
            } else {
                archives = archiveService.getAllArchives();
            }

            return ResponseEntity.ok(archives);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET ARCHIVE BY ID
    // ============================================================
    @GetMapping("/{archiveId}")
    public ResponseEntity<?> getArchive(@PathVariable Long archiveId,
                                        @RequestParam(required = false) String username) {
        try {
            var archiveOpt = archiveService.getArchive(archiveId);
            if (archiveOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SystemArchive archive = archiveOpt.get();

            // Permission check - only if company ID is set
            if (username != null && !username.isEmpty() && archive.getCompanyId() != null) {
                if (!permissionService.canAccessCompany(username, archive.getCompanyId())) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }

            Map<String, Object> response = archiveService.getArchiveReportData(archiveId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GENERATE ARCHIVE REPORT
    // ============================================================
    @GetMapping("/{archiveId}/report")
    public ResponseEntity<?> getArchiveReport(@PathVariable Long archiveId,
                                              @RequestParam(required = false) String username) {
        try {
            var archiveOpt = archiveService.getArchive(archiveId);
            if (archiveOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SystemArchive archive = archiveOpt.get();

            // Permission check
            if (username != null && !username.isEmpty() && archive.getCompanyId() != null) {
                if (!permissionService.canAccessCompany(username, archive.getCompanyId())) {
                    return ResponseEntity.status(403).body("Access denied");
                }
            }

            Map<String, Object> report = archiveService.getArchiveReportData(archiveId);

            // Parse archive data JSON
            // This will be used by frontend to display report

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // GET ARCHIVE STATS
    // ============================================================
    @GetMapping("/stats")
    public ResponseEntity<?> getArchiveStats(@RequestParam(required = false) String username) {
        try {
            List<SystemArchive> archives;

            if (username != null && !username.isEmpty()) {
                if (permissionService.isUser(username)) {
                    Long companyId = permissionService.getUserCompanyId(username);
                    if (companyId != null) {
                        archives = archiveService.getArchivesByCompany(companyId);
                    } else {
                        archives = List.of();
                    }
                } else {
                    archives = archiveService.getAllArchives();
                }
            } else {
                archives = archiveService.getAllArchives();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalArchives", archives.size());

            long totalAlerts = 0;
            long totalZones = 0;
            for (SystemArchive archive : archives) {
                totalAlerts += archive.getAlertCount() != null ? archive.getAlertCount() : 0;
                totalZones += archive.getZoneCount() != null ? archive.getZoneCount() : 0;
            }
            stats.put("totalAlertsArchived", totalAlerts);
            stats.put("totalZonesArchived", totalZones);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}