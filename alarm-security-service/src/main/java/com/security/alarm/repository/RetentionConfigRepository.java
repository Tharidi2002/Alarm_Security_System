package com.security.alarm.repository;

import com.security.alarm.entity.RetentionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RetentionConfigRepository extends JpaRepository<RetentionConfig, Long> {

    Optional<RetentionConfig> findByConfigKey(String configKey);

    String findConfigValueByConfigKey(String configKey);
}