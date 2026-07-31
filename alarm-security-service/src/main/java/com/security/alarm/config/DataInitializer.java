package com.security.alarm.config;

import com.security.alarm.entity.SystemConfig;
import com.security.alarm.repository.SystemConfigRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;

    public DataInitializer(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    public void run(String... args) {
        // Create default secret code if not exists
        if (systemConfigRepository.findByConfigKey("MASTER_SECRET_CODE").isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey("MASTER_SECRET_CODE");
            config.setConfigValue("ALARM-2024-SECURE-KEY");
            systemConfigRepository.save(config);
            System.out.println("✅ MASTER_SECRET_CODE created: ALARM-2024-SECURE-KEY");
            System.out.println("⚠️  Please change this code in production!");
        } else {
            System.out.println("✅ MASTER_SECRET_CODE already exists");
        }
    }
}