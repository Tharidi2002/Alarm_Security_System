package com.security.alarm.repository;

import com.security.alarm.entity.RetentionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RetentionConfigRepository extends JpaRepository<RetentionConfig, Long> {

    Optional<RetentionConfig> findByConfigKey(String configKey);

    @Query("SELECT c.configValue FROM RetentionConfig c WHERE c.configKey = :key")
    String findConfigValueByConfigKey(@Param("key") String key);
}