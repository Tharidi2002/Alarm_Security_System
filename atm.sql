-- Database එක create කරන්න (නැත්නම්)
CREATE DATABASE IF NOT EXISTS atm
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Database එක use කරන්න
USE atm;


-- ========================================
-- 1. BANKS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS banks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 2. ATM_MACHINES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS atm_machines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    atm_code VARCHAR(50) UNIQUE NOT NULL,
    atm_name VARCHAR(100),
    location VARCHAR(255) NOT NULL,
    sim_number VARCHAR(20) UNIQUE NOT NULL,
    gsm_signal INT DEFAULT 0,
    battery_level INT DEFAULT 100,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_heartbeat TIMESTAMP NULL,
    firmware_version VARCHAR(20) DEFAULT '1.0.0',
    installation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    bank_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE SET NULL,
    INDEX idx_sim_number (sim_number),
    INDEX idx_bank_id (bank_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 3. ATM_ZONES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS atm_zones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    atm_id BIGINT NOT NULL,
    zone_number INT NOT NULL,
    zone_name VARCHAR(100) NOT NULL,
    zone_type VARCHAR(50) DEFAULT 'SECURITY',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (atm_id) REFERENCES atm_machines(id) ON DELETE CASCADE,
    UNIQUE KEY unique_atm_zone (atm_id, zone_number),
    INDEX idx_atm_id (atm_id),
    INDEX idx_zone_number (zone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 4. ALERT_LOGS TABLE (Main Alerts Table)
-- ========================================
CREATE TABLE IF NOT EXISTS alert_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    atm_id BIGINT,
    zone_number INT DEFAULT 0,
    zone_name VARCHAR(100),
    alert_type VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    raw_message TEXT,
    confidence_score DOUBLE DEFAULT 0.0,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',
    acknowledged_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    acknowledged_by VARCHAR(50),
    resolved_by VARCHAR(50),
    resolution_notes TEXT,
    severity INT DEFAULT 1,
    is_escalated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (atm_id) REFERENCES atm_machines(id) ON DELETE SET NULL,
    INDEX idx_atm_id (atm_id),
    INDEX idx_status (status),
    INDEX idx_received_at (received_at),
    INDEX idx_severity (severity),
    INDEX idx_alert_type (alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 5. USERS TABLE (For Authentication)
-- ========================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'BANK_USER',
    bank_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE SET NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_bank_id (bank_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 6. ACTIVITY_LOGS TABLE (Audit Trail)
-- ========================================
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_performed_at (performed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 7. SMS_GATEWAY_LOGS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS sms_gateway_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    to_number VARCHAR(20) NOT NULL,
    from_number VARCHAR(20) NOT NULL,
    message TEXT,
    direction VARCHAR(10) DEFAULT 'INBOUND',
    status VARCHAR(20) DEFAULT 'RECEIVED',
    sms_provider VARCHAR(50),
    provider_message_id VARCHAR(100),
    sent_at TIMESTAMP NULL,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    INDEX idx_to_number (to_number),
    INDEX idx_from_number (from_number),
    INDEX idx_status (status),
    INDEX idx_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 8. REPORTS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_type VARCHAR(50) NOT NULL,
    report_name VARCHAR(100) NOT NULL,
    generated_by VARCHAR(50),
    file_path VARCHAR(500),
    file_format VARCHAR(20) DEFAULT 'PDF',
    parameters JSON,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    start_date DATE,
    end_date DATE,
    INDEX idx_report_type (report_type),
    INDEX idx_generated_at (generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




-- ========================================
-- ATM SECURITY SYSTEM - COMPLETE SCHEMA
-- Database: atm
-- ========================================

-- Drop database if exists (අලුතෙන් හදන්න ඕනෙනම්)
-- DROP DATABASE IF EXISTS atm;

-- Create database


USE atm;

-- ===== ALL TABLES =====
-- (මෙතනට ඉහත සියලු CREATE TABLE statements එක් කරන්න)
-- banks, atm_machines, atm_zones, alert_logs, users, 
-- activity_logs, sms_gateway_logs, reports

-- ========================================
-- SAMPLE DATA (Testing සඳහා)
-- ========================================

-- 1. Sample Banks
INSERT INTO banks (name, contact_email, contact_phone, address) VALUES 
('National Bank', 'info@nationalbank.com', '+94771111111', 'Colombo 01'),
('Commercial Bank', 'info@combank.com', '+94772222222', 'Colombo 02'),
('Sampath Bank', 'info@sampath.lk', '+94773333333', 'Colombo 03');

-- 2. Sample ATM Machines
INSERT INTO atm_machines (atm_code, atm_name, location, sim_number, bank_id, status) VALUES 
('ATM001', 'Main Branch ATM', 'Colombo Fort', '0712345678', 1, 'ACTIVE'),
('ATM002', 'Kandy Branch ATM', 'Kandy City Centre', '0712345679', 1, 'ACTIVE'),
('ATM003', 'Galle Branch ATM', 'Galle Fort', '0712345680', 2, 'ACTIVE'),
('ATM004', 'Jaffna Branch ATM', 'Jaffna Town', '0712345681', 3, 'ACTIVE');

-- 3. Sample ATM Zones (Each ATM has 8 zones)
INSERT INTO atm_zones (atm_id, zone_number, zone_name, zone_type) VALUES 
-- ATM001 Zones
(1, 1, 'Cash Dispenser', 'SECURITY'),
(1, 2, 'Card Reader', 'SECURITY'),
(1, 3, 'Keypad Area', 'SECURITY'),
(1, 4, 'Cash Vault Door', 'HIGH_SECURITY'),
(1, 5, 'Fire Sensor', 'FIRE'),
(1, 6, 'Motion Detector', 'MOTION'),
(1, 7, 'Main Door', 'ACCESS'),
(1, 8, 'SOS Button', 'EMERGENCY'),
-- ATM002 Zones
(2, 1, 'Cash Dispenser', 'SECURITY'),
(2, 2, 'Card Reader', 'SECURITY'),
(2, 3, 'Keypad Area', 'SECURITY'),
(2, 4, 'Cash Vault Door', 'HIGH_SECURITY'),
(2, 5, 'Fire Sensor', 'FIRE'),
(2, 6, 'Motion Detector', 'MOTION'),
(2, 7, 'Main Door', 'ACCESS'),
(2, 8, 'SOS Button', 'EMERGENCY');

-- 4. Sample Users
-- Password: admin123 (BCrypt encrypted)
INSERT INTO users (username, password, email, full_name, role, bank_id) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', 'admin@system.com', 'System Admin', 'ADMIN', NULL),
('bank_user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', 'user1@nationalbank.com', 'National Bank User', 'BANK_USER', 1),
('bank_user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', 'user2@combank.com', 'Commercial Bank User', 'BANK_USER', 2);

-- 5. Sample Alert Logs (Testing)
INSERT INTO alert_logs (atm_id, zone_number, zone_name, alert_type, raw_message, status, severity, received_at) VALUES 
(1, 5, 'Fire Sensor', 'FIRE', 'Alarm! Zone:05 Fire at ATM Main Branch', 'PENDING', 3, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(1, 1, 'Cash Dispenser', 'TAMPER_ALERT', 'Alarm! Zone:01 Tamper at Cash Dispenser', 'PENDING', 3, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(2, 7, 'Main Door', 'DOOR_OPEN', 'Alarm! Zone:07 Door Open at Kandy Branch', 'ACKNOWLEDGED', 2, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(3, 4, 'Cash Vault Door', 'INTRUSION', 'Alarm! Zone:04 Intrusion at Galle Branch', 'PENDING', 4, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(1, 8, 'SOS Button', 'SOS_ALARM', 'SOS Alarm! Zone:08 at Main Branch', 'PENDING', 4, DATE_SUB(NOW(), INTERVAL 2 MINUTE));

-- 6. Sample Activity Logs
INSERT INTO activity_logs (user_id, username, action, details, ip_address) VALUES 
(1, 'admin', 'LOGIN', 'Admin logged in', '192.168.1.1'),
(2, 'bank_user1', 'VIEW_ALERTS', 'Viewed alerts for National Bank', '192.168.1.2');

-- ========================================
-- VERIFY TABLES
-- ========================================
SHOW TABLES;
SELECT COUNT(*) FROM banks;
SELECT COUNT(*) FROM atm_machines;
SELECT COUNT(*) FROM atm_zones;
SELECT COUNT(*) FROM alert_logs;
SELECT COUNT(*) FROM users;