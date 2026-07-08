-- ======================================================
-- Alarm Security Database - Complete Setup Script
-- ======================================================

-- 1. Database එක create කරන්න (නැත්නම්)
DROP DATABASE IF EXISTS alarm_security_db;
CREATE DATABASE alarm_security_db;



USE alarm_security_db;

-- Add new columns to alert_logs table
ALTER TABLE alert_logs 
ADD COLUMN resolved_by VARCHAR(50) NULL COMMENT 'Username who resolved the alert',
ADD COLUMN resolved_at TIMESTAMP NULL COMMENT 'When the alert was resolved',
ADD COLUMN pending_duration VARCHAR(50) NULL COMMENT 'How long the alert was pending (e.g., 5m 30s)',
ADD COLUMN resolve_description TEXT NULL COMMENT 'Optional description for resolution',
ADD COLUMN resolved_by_user_id BIGINT NULL COMMENT 'User ID who resolved the alert';

-- Add index for faster queries
CREATE INDEX idx_alert_logs_resolved_at ON alert_logs(resolved_at DESC);
CREATE INDEX idx_alert_logs_status_resolved ON alert_logs(status, resolved_at);

-- Add new columns to alert_logs table
