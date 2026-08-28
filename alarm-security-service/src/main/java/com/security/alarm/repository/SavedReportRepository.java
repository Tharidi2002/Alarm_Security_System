package com.security.alarm.repository;

import com.security.alarm.entity.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {

    // ============================================================
    // GET ACTIVE REPORTS
    // ============================================================
    
    @Query("SELECT r FROM SavedReport r WHERE r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findAllActive();
    
    @Query("SELECT r FROM SavedReport r WHERE r.generatedBy = :username AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findActiveByGeneratedBy(@Param("username") String username);
    
    @Query("SELECT r FROM SavedReport r WHERE r.companyId = :companyId AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findActiveByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT r FROM SavedReport r WHERE r.userId = :userId AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findActiveByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM SavedReport r WHERE r.reportType = :reportType AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findActiveByReportType(@Param("reportType") String reportType);
    
    @Query("SELECT r FROM SavedReport r WHERE r.companyId = :companyId AND r.isPublic = true AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findPublicByCompanyId(@Param("companyId") Long companyId);

    // ============================================================
    // GET DELETED REPORTS
    // ============================================================
    
    @Query("SELECT r FROM SavedReport r WHERE r.isDeleted = true ORDER BY r.deletedAt DESC")
    List<SavedReport> findAllDeleted();
    
    @Query("SELECT r FROM SavedReport r WHERE r.generatedBy = :username AND r.isDeleted = true ORDER BY r.deletedAt DESC")
    List<SavedReport> findDeletedByGeneratedBy(@Param("username") String username);

    // ============================================================
    // COUNT METHODS
    // ============================================================
    
    @Query("SELECT COUNT(r) FROM SavedReport r WHERE r.generatedBy = :username AND r.isDeleted = false")
    long countActiveByGeneratedBy(@Param("username") String username);
    
    @Query("SELECT COUNT(r) FROM SavedReport r WHERE r.companyId = :companyId AND r.isDeleted = false")
    long countActiveByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT COUNT(r) FROM SavedReport r WHERE r.reportType = :reportType AND r.isDeleted = false")
    long countActiveByReportType(@Param("reportType") String reportType);

    // ============================================================
    // SOFT DELETE
    // ============================================================
    
    @Modifying
    @Transactional
    @Query("UPDATE SavedReport r SET r.isDeleted = true, r.deletedAt = :deletedAt, r.deletedBy = :deletedBy WHERE r.id = :id")
    void softDeleteById(@Param("id") Long id, 
                        @Param("deletedAt") LocalDateTime deletedAt, 
                        @Param("deletedBy") String deletedBy);
    
    @Modifying
    @Transactional
    @Query("UPDATE SavedReport r SET r.isDeleted = true, r.deletedAt = :deletedAt, r.deletedBy = :deletedBy WHERE r.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, 
                         @Param("deletedAt") LocalDateTime deletedAt, 
                         @Param("deletedBy") String deletedBy);

    // ============================================================
    // UPDATE COUNTS
    // ============================================================
    
    @Modifying
    @Transactional
    @Query("UPDATE SavedReport r SET r.downloadCount = r.downloadCount + 1 WHERE r.id = :id")
    void incrementDownloadCount(@Param("id") Long id);
    
    @Modifying
    @Transactional
    @Query("UPDATE SavedReport r SET r.viewCount = r.viewCount + 1 WHERE r.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // ============================================================
    // FIND BY DATE RANGE
    // ============================================================
    
    @Query("SELECT r FROM SavedReport r WHERE r.generatedAt BETWEEN :from AND :to AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findActiveByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    @Query("SELECT r FROM SavedReport r WHERE r.generatedAt < :cutoff AND r.isDeleted = false")
    List<SavedReport> findOldReports(@Param("cutoff") LocalDateTime cutoff);

    // ============================================================
    // FIND BY COMPANY AND TYPE
    // ============================================================
    
    @Query("SELECT r FROM SavedReport r WHERE r.companyId = :companyId AND r.reportType = :reportType AND r.isDeleted = false ORDER BY r.generatedAt DESC")
    List<SavedReport> findActiveByCompanyIdAndReportType(@Param("companyId") Long companyId, @Param("reportType") String reportType);
}