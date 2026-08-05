package com.security.alarm.repository;

import com.security.alarm.entity.SystemArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemArchiveRepository extends JpaRepository<SystemArchive, Long> {
    
    List<SystemArchive> findAllByOrderByArchivedAtDesc();
    
    List<SystemArchive> findByCompanyIdOrderByArchivedAtDesc(Long companyId);
    
    Optional<SystemArchive> findBySystemId(Long systemId);
    
    @Query("SELECT a FROM SystemArchive a WHERE a.retentionUntil < :cutoff AND a.status = 'ARCHIVED'")
    List<SystemArchive> findExpiredArchives(LocalDateTime cutoff);
    
    @Query("SELECT COUNT(a) FROM SystemArchive a WHERE a.companyId = :companyId AND a.status = 'ARCHIVED'")
    long countArchivedByCompanyId(Long companyId);
}