-- ======================================================
-- Alarm Security Database - Complete Setup Script
-- ======================================================

-- 1. Database එක create කරන්න (නැත්නම්)
DROP DATABASE IF EXISTS alarm_security_db;
CREATE DATABASE alarm_security_db;
USE alarm_security_db;

-- ======================================================
-- 2. Tables Create කරන්න (Hibernate එකෙන් auto-create වුනත්,
--    මෙන්න manual backup එක)
-- ======================================================

-- 2.1 Alarm Systems Table

CREATE TABLE IF NOT EXISTS `alarm_systems` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `system_code` VARCHAR(50) NOT NULL UNIQUE,
    `location` VARCHAR(255) NOT NULL,
    `sim_number` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `last_status_changed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2 Alarm Zones Table

CREATE TABLE IF NOT EXISTS `alarm_zones` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `system_id` BIGINT,
    `zone_number` INT NOT NULL,
    `zone_name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`system_id`) REFERENCES `alarm_systems`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.3 Alert Logs Table (Updated with new columns)

CREATE TABLE IF NOT EXISTS `alert_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `system_id` BIGINT,
    `zone_number` INT DEFAULT 0,
    `zone_numbers` VARCHAR(100) DEFAULT '00',
    `alert_type` VARCHAR(255) NOT NULL,
    `raw_message` TEXT,
    `received_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`system_id`) REFERENCES `alarm_systems`(`id`) ON DELETE CASCADE,
    INDEX `idx_received_at` (`received_at` DESC),
    INDEX `idx_status` (`status`),
    INDEX `idx_zone_numbers` (`zone_numbers`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================
-- 3. Initial Data Insert කරන්න
-- ======================================================

-- 3.1 Alarm Systems - Main System
INSERT INTO `alarm_systems` (`system_code`, `location`, `sim_number`, `status`) 
VALUES 
    ('ALARM-MAIN-01', 'Colombo 03', '0772032675', 'ACTIVE'),
    ('ALARM-Z8B-01', 'Colombo 07 - Kollupitiya', '0714868100', 'ACTIVE'),
    ('ALARM-Z8B-02', 'Colombo 05 - Havelock Town', '0714868101', 'ACTIVE'),
    ('ALARM-Z8B-03', 'Galle - Main Street', '0714868102', 'ACTIVE'),
    ('ALARM-Z8B-04', 'Kandy - City Center', '0714868103', 'ACTIVE');

-- 3.2 Alarm Zones - Zone mapping for ALARM-MAIN-01
INSERT INTO `alarm_zones` (`system_id`, `zone_number`, `zone_name`) 
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-MAIN-01'),
    zone_num,
    zone_name
FROM (
    SELECT 1 as zone_num, 'Main Entrance' as zone_name UNION ALL
    SELECT 2, 'Cash Counter Area' UNION ALL
    SELECT 3, 'Lobby' UNION ALL
    SELECT 4, 'Server Room' UNION ALL
    SELECT 5, 'Back Office' UNION ALL
    SELECT 6, 'Vault Room' UNION ALL
    SELECT 7, 'Emergency Exit' UNION ALL
    SELECT 8, 'Parking Area'
) zones;

-- 3.3 Alarm Zones - Zone mapping for Z8B Systems
INSERT INTO `alarm_zones` (`system_id`, `zone_number`, `zone_name`) 
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01'),
    zone_num,
    zone_name
FROM (
    SELECT 1 as zone_num, 'Main Door' as zone_name UNION ALL
    SELECT 2, 'Dispenser Area' UNION ALL
    SELECT 3, 'Card Reader Area' UNION ALL
    SELECT 4, 'CCTV Camera 1' UNION ALL
    SELECT 5, 'CCTV Camera 2' UNION ALL
    SELECT 6, 'Alarm Sensor 1' UNION ALL
    SELECT 7, 'Alarm Sensor 2' UNION ALL
    SELECT 8, 'Emergency Exit'
) zones;

INSERT INTO `alarm_zones` (`system_id`, `zone_number`, `zone_name`) 
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-02'),
    zone_num,
    zone_name
FROM (
    SELECT 1 as zone_num, 'Main Door' as zone_name UNION ALL
    SELECT 2, 'Dispenser Area' UNION ALL
    SELECT 3, 'Card Reader Area' UNION ALL
    SELECT 4, 'CCTV Camera 1' UNION ALL
    SELECT 5, 'CCTV Camera 2' UNION ALL
    SELECT 6, 'Alarm Sensor 1' UNION ALL
    SELECT 7, 'Alarm Sensor 2' UNION ALL
    SELECT 8, 'Emergency Exit'
) zones;

INSERT INTO `alarm_zones` (`system_id`, `zone_number`, `zone_name`) 
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-03'),
    zone_num,
    zone_name
FROM (
    SELECT 1 as zone_num, 'Main Door' as zone_name UNION ALL
    SELECT 2, 'Dispenser Area' UNION ALL
    SELECT 3, 'Card Reader Area' UNION ALL
    SELECT 4, 'CCTV Camera 1' UNION ALL
    SELECT 5, 'CCTV Camera 2' UNION ALL
    SELECT 6, 'Alarm Sensor 1' UNION ALL
    SELECT 7, 'Alarm Sensor 2' UNION ALL
    SELECT 8, 'Emergency Exit'
) zones;

INSERT INTO `alarm_zones` (`system_id`, `zone_number`, `zone_name`) 
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-04'),
    zone_num,
    zone_name
FROM (
    SELECT 1 as zone_num, 'Main Door' as zone_name UNION ALL
    SELECT 2, 'Dispenser Area' UNION ALL
    SELECT 3, 'Card Reader Area' UNION ALL
    SELECT 4, 'CCTV Camera 1' UNION ALL
    SELECT 5, 'CCTV Camera 2' UNION ALL
    SELECT 6, 'Alarm Sensor 1' UNION ALL
    SELECT 7, 'Alarm Sensor 2' UNION ALL
    SELECT 8, 'Emergency Exit'
) zones;

-- ======================================================
-- 4. Sample Alert Logs Data (For Testing)
-- ======================================================

-- 4.1 Z8B System Alerts (Zone 01)
INSERT INTO `alert_logs` (`system_id`, `zone_number`, `zone_numbers`, `alert_type`, `raw_message`, `received_at`, `status`)
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01'),
    1, '01',
    'ZONE 01 ALARM!',
    '0714868100 ZONE 01 ALARM!',
    DATE_SUB(NOW(), INTERVAL 5 MINUTE),
    'PENDING'
UNION ALL
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01'),
    8, '08',
    'ZONE 08 ALARM!',
    '0714868100 ZONE 08 ALARM!',
    DATE_SUB(NOW(), INTERVAL 4 MINUTE),
    'PENDING'
UNION ALL
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01'),
    0, '00',
    'Host tamper alarm!',
    '0714868100 Host tamper alarm!',
    DATE_SUB(NOW(), INTERVAL 3 MINUTE),
    'PENDING'
UNION ALL
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01'),
    0, '00',
    'ARMED',
    '0714868100 ARMED',
    DATE_SUB(NOW(), INTERVAL 2 MINUTE),
    'PENDING'
UNION ALL
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01'),
    1, '01,08,07,05',
    'ZONE 01 ALARM! ZONE 08 ALARM! ZONE 07 ALARM! ZONE 05 ALARM!',
    '0714868100 ZONE 01 ALARM! ZONE 08 ALARM! ZONE 07 ALARM! ZONE 05 ALARM!',
    DATE_SUB(NOW(), INTERVAL 1 MINUTE),
    'PENDING';

-- 4.2 Main System Alerts
INSERT INTO `alert_logs` (`system_id`, `zone_number`, `zone_numbers`, `alert_type`, `raw_message`, `received_at`, `status`)
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-MAIN-01'),
    2, '02',
    'Alarm! Zone:02 Fire Sensor Activated',
    'Alarm! Zone:02 Fire Sensor Activated',
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    'PENDING'
UNION ALL
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-MAIN-01'),
    0, '00',
    'Power Failure Detected',
    'Power Failure Detected at Colombo 03',
    DATE_SUB(NOW(), INTERVAL 8 MINUTE),
    'RESOLVED'
UNION ALL
SELECT 
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-MAIN-01'),
    1, '01',
    'Unauthorized Access Attempt - Zone 01',
    'Unauthorized Access Attempt at Main Entrance',
    DATE_SUB(NOW(), INTERVAL 6 MINUTE),
    'PENDING';

-- ======================================================
-- 5. Views for Dashboard
-- ======================================================

-- 5.1 Recent Alerts View
CREATE OR REPLACE VIEW `v_recent_alerts` AS
SELECT 
    al.id,
    al.system_id,
    am.system_code,
    am.location,
    al.zone_number,
    al.zone_numbers,
    al.alert_type,
    al.received_at,
    al.status,
    TIMESTAMPDIFF(MINUTE, al.received_at, NOW()) as minutes_ago
FROM alert_logs al
LEFT JOIN alarm_systems am ON al.system_id = am.id
ORDER BY al.received_at DESC
LIMIT 100;

-- 5.2 Alert Summary View
CREATE OR REPLACE VIEW `v_alert_summary` AS
SELECT 
    am.system_code,
    am.location,
    COUNT(*) as total_alerts,
    SUM(CASE WHEN al.status = 'PENDING' THEN 1 ELSE 0 END) as pending_alerts,
    SUM(CASE WHEN al.status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved_alerts,
    MAX(al.received_at) as last_alert_time
FROM alarm_systems am
LEFT JOIN alert_logs al ON am.id = al.system_id
GROUP BY am.id, am.system_code, am.location;

-- 5.3 Zone Alert Count View
CREATE OR REPLACE VIEW `v_zone_alert_count` AS
SELECT 
    am.system_code,
    al.zone_number,
    al.zone_numbers,
    COUNT(*) as alert_count,
    MAX(al.received_at) as last_alert
FROM alert_logs al
LEFT JOIN alarm_systems am ON al.system_id = am.id
WHERE al.zone_number > 0
GROUP BY am.system_code, al.zone_number, al.zone_numbers
ORDER BY alert_count DESC;

-- ======================================================
-- 6. Stored Procedures
-- ======================================================

-- 6.1 Get Alerts by System
DELIMITER //
CREATE PROCEDURE `sp_get_system_alerts`(IN p_system_code VARCHAR(50))
BEGIN
    SELECT 
        al.*,
        am.system_code,
        am.location,
        am.sim_number
    FROM alert_logs al
    LEFT JOIN alarm_systems am ON al.system_id = am.id
    WHERE am.system_code = p_system_code
    ORDER BY al.received_at DESC;
END //
DELIMITER ;

-- 6.2 Get Alerts by Date Range
DELIMITER //
CREATE PROCEDURE `sp_get_alerts_by_date`(IN p_start_date DATETIME, IN p_end_date DATETIME)
BEGIN
    SELECT 
        al.*,
        am.system_code,
        am.location
    FROM alert_logs al
    LEFT JOIN alarm_systems am ON al.system_id = am.id
    WHERE al.received_at BETWEEN p_start_date AND p_end_date
    ORDER BY al.received_at DESC;
END //
DELIMITER ;

-- 6.3 Update Alert Status
DELIMITER //
CREATE PROCEDURE `sp_resolve_alert`(IN p_alert_id BIGINT)
BEGIN
    UPDATE alert_logs 
    SET status = 'RESOLVED' 
    WHERE id = p_alert_id;
    
    SELECT * FROM alert_logs WHERE id = p_alert_id;
END //
DELIMITER ;

-- ======================================================
-- 7. Useful Queries for Testing
-- ======================================================

-- 7.1 Check all alerts
SELECT * FROM alert_logs ORDER BY received_at DESC;

-- 7.2 Check pending alerts count
SELECT COUNT(*) as pending_count FROM alert_logs WHERE status = 'PENDING';

-- 7.3 Check Alarm Systems
SELECT * FROM alarm_systems;

-- 7.4 Check Alarm Zones
SELECT * FROM alarm_zones;

-- 7.5 Check recent alerts with system details
SELECT 
    al.id,
    am.system_code,
    am.location,
    al.zone_numbers,
    al.alert_type,
    al.received_at,
    al.status
FROM alert_logs al
LEFT JOIN alarm_systems am ON al.system_id = am.id
ORDER BY al.received_at DESC
LIMIT 10;

-- 7.6 Zone wise alert count
SELECT 
    zone_number,
    zone_numbers,
    COUNT(*) as count
FROM alert_logs
WHERE zone_number > 0
GROUP BY zone_number, zone_numbers
ORDER BY count DESC;

-- 7.7 Daily alert count
SELECT 
    DATE(received_at) as alert_date,
    COUNT(*) as total,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending
FROM alert_logs
GROUP BY DATE(received_at)
ORDER BY alert_date DESC;

-- ======================================================
-- 8. Indexes for Performance
-- ======================================================

-- Already added in table creation, but if needed:
-- CREATE INDEX idx_alert_logs_system_id ON alert_logs(system_id);
-- CREATE INDEX idx_alert_logs_received_at ON alert_logs(received_at DESC);
-- CREATE INDEX idx_alert_logs_status ON alert_logs(status);
-- CREATE INDEX idx_alert_logs_zone_numbers ON alert_logs(zone_numbers);

-- ======================================================
-- 9. Triggers (Optional)
-- ======================================================

-- 9.1 Auto-update zone_numbers from zone_number
DELIMITER //
CREATE TRIGGER `before_alert_insert` 
BEFORE INSERT ON `alert_logs` 
FOR EACH ROW
BEGIN
    IF NEW.zone_numbers IS NULL OR NEW.zone_numbers = '' THEN
        IF NEW.zone_number IS NOT NULL AND NEW.zone_number > 0 THEN
            SET NEW.zone_numbers = LPAD(NEW.zone_number, 2, '0');
        ELSE
            SET NEW.zone_numbers = '00';
        END IF;
    END IF;
END //
DELIMITER ;

-- ======================================================
-- 10. Cleanup (Optional - Delete test data)
-- ======================================================

-- DELETE FROM alert_logs WHERE alert_type LIKE '%ZONE%';
-- DELETE FROM alert_logs WHERE received_at < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ======================================================
-- 11. User Management Tables & Data
-- ======================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_systems` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `system_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`system_id`) REFERENCES `alarm_systems`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default Admin
INSERT INTO `users` (`username`, `password`, `role`)
VALUES ('admin', 'admin123', 'ADMIN')
ON DUPLICATE KEY UPDATE username=username;

-- Insert default User 1
INSERT INTO `users` (`username`, `password`, `role`)
VALUES ('user1', 'user123', 'USER')
ON DUPLICATE KEY UPDATE username=username;

-- Insert default User 2
INSERT INTO `users` (`username`, `password`, `role`)
VALUES ('user2', 'user123', 'USER')
ON DUPLICATE KEY UPDATE username=username;

-- Map systems for User 1 (ALARM-MAIN-01 -> id 1)
INSERT INTO `user_systems` (`user_id`, `system_id`)
SELECT 
    (SELECT id FROM users WHERE username = 'user1'),
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-MAIN-01')
WHERE NOT EXISTS (
    SELECT 1 FROM user_systems WHERE user_id = (SELECT id FROM users WHERE username = 'user1') AND system_id = (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-MAIN-01')
);

-- Map systems for User 2 (ALARM-Z8B-01 -> id 2, ALARM-Z8B-02 -> id 3)
INSERT INTO `user_systems` (`user_id`, `system_id`)
SELECT 
    (SELECT id FROM users WHERE username = 'user2'),
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01')
WHERE NOT EXISTS (
    SELECT 1 FROM user_systems WHERE user_id = (SELECT id FROM users WHERE username = 'user2') AND system_id = (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-01')
);

INSERT INTO `user_systems` (`user_id`, `system_id`)
SELECT 
    (SELECT id FROM users WHERE username = 'user2'),
    (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-02')
WHERE NOT EXISTS (
    SELECT 1 FROM user_systems WHERE user_id = (SELECT id FROM users WHERE username = 'user2') AND system_id = (SELECT id FROM alarm_systems WHERE system_code = 'ALARM-Z8B-02')
);

-- ======================================================
-- End of Script
-- ======================================================