-- ============================================================
-- LOCAL DATABASE SETUP
-- ============================================================
-- Run this in MySQL to create local database

CREATE DATABASE alarm;
USE alarm;


-- ============================================================
-- JUNE 2026 - Alert Logs (30 days)
-- ============================================================

-- System 1 (HHC Gamming) - Zone Alarms
INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, pending_duration_seconds, resolved_at, resolved_by, resolution_description) VALUES
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-06-01 08:15:00', 'RESOLVED', 1, '1', 1, 45, '2026-06-01 08:15:45', 'user1', 'False alarm - verified'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-06-02 09:30:00', 'RESOLVED', 2, '2', 1, 120, '2026-06-02 09:32:00', 'admin', 'Intrusion detected - cleared'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-06-03 14:20:00', 'RESOLVED', 3, '3', 1, 60, '2026-06-03 14:21:00', 'user1', 'Test alert - resolved'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-06-05 10:00:00', 'RESOLVED', 4, '4', 1, 30, '2026-06-05 10:00:30', 'admin', 'System check'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-06-07 16:45:00', 'RESOLVED', 5, '5', 1, 90, '2026-06-07 16:46:30', 'user1', 'Maintenance alert'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-06-10 11:10:00', 'RESOLVED', 6, '6', 1, 150, '2026-06-10 11:12:30', 'admin', 'Vault access detected'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-06-12 07:30:00', 'RESOLVED', 7, '7', 1, 75, '2026-06-12 07:31:15', 'user1', 'Emergency exit opened'),
('Zone 8 Alarm!', 'Zone 8 Alarm!', '2026-06-15 13:20:00', 'RESOLVED', 8, '8', 1, 40, '2026-06-15 13:20:40', 'admin', 'Parking area motion'),
('Zone 9 Alarm!', 'Zone 9 Alarm!', '2026-06-18 09:50:00', 'RESOLVED', 9, '9', 1, 55, '2026-06-18 09:50:55', 'user1', 'Store room alert'),
('Zone 10 Alarm!', 'Zone 10 Alarm!', '2026-06-20 15:00:00', 'RESOLVED', 10, '10', 1, 85, '2026-06-20 15:01:25', 'admin', 'Rest room sensor'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-06-04 10:30:00', 'RESOLVED', 1, '1', 2, 60, '2026-06-04 10:31:00', 'user2', 'Front door alarm'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-06-06 14:15:00', 'RESOLVED', 2, '2', 2, 35, '2026-06-06 14:15:35', 'admin', 'Cash counter check'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-06-08 11:40:00', 'RESOLVED', 3, '3', 2, 95, '2026-06-08 11:41:35', 'user2', 'Lobby alarm'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-06-11 08:20:00', 'RESOLVED', 4, '4', 2, 50, '2026-06-11 08:20:50', 'admin', 'Server room alert'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-06-13 16:10:00', 'RESOLVED', 5, '5', 2, 110, '2026-06-13 16:11:50', 'user2', 'Back office intrusion'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-06-16 09:30:00', 'RESOLVED', 6, '6', 2, 70, '2026-06-16 09:31:10', 'admin', 'Vault room access'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-06-19 12:50:00', 'RESOLVED', 7, '7', 2, 45, '2026-06-19 12:50:45', 'user2', 'Emergency exit'),
('Zone 8 Alarm!', 'Zone 8 Alarm!', '2026-06-22 15:30:00', 'RESOLVED', 8, '8', 2, 80, '2026-06-22 15:31:20', 'admin', 'Parking area'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-06-09 07:10:00', 'RESOLVED', 1, '1', 3, 65, '2026-06-09 07:11:05', 'user1', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-06-14 13:45:00', 'RESOLVED', 2, '2', 3, 40, '2026-06-14 13:45:40', 'admin', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-06-17 10:20:00', 'RESOLVED', 3, '3', 3, 55, '2026-06-17 10:20:55', 'user1', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-06-21 16:40:00', 'RESOLVED', 4, '4', 3, 100, '2026-06-21 16:41:40', 'admin', 'Server room'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-06-23 08:55:00', 'RESOLVED', 5, '5', 3, 35, '2026-06-23 08:55:35', 'user1', 'Back office'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-06-24 11:15:00', 'RESOLVED', 1, '1', 4, 90, '2026-06-24 11:16:30', 'admin', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-06-25 14:30:00', 'RESOLVED', 2, '2', 4, 45, '2026-06-25 14:30:45', 'user1', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-06-26 09:45:00', 'RESOLVED', 3, '3', 4, 70, '2026-06-26 09:46:10', 'admin', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-06-27 17:20:00', 'RESOLVED', 4, '4', 4, 55, '2026-06-27 17:20:55', 'user1', 'Server room'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-06-28 10:05:00', 'RESOLVED', 1, '1', 5, 60, '2026-06-28 10:06:00', 'user2', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-06-29 13:40:00', 'RESOLVED', 2, '2', 5, 30, '2026-06-29 13:40:30', 'admin', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-06-30 08:30:00', 'RESOLVED', 3, '3', 5, 85, '2026-06-30 08:31:25', 'user2', 'Lobby');


-- ============================================================
-- JULY 2026 - Alert Logs (31 days)
-- ============================================================

-- System 1 - Zone Alarms
INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, pending_duration_seconds, resolved_at, resolved_by, resolution_description) VALUES
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-07-01 09:00:00', 'RESOLVED', 1, '1', 1, 120, '2026-07-01 09:02:00', 'user1', 'Morning check'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-07-02 14:30:00', 'RESOLVED', 2, '2', 1, 60, '2026-07-02 14:31:00', 'admin', 'Security test'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-07-03 11:15:00', 'RESOLVED', 3, '3', 1, 45, '2026-07-03 11:15:45', 'user1', 'False alarm'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-07-05 16:40:00', 'RESOLVED', 4, '4', 1, 90, '2026-07-05 16:41:30', 'admin', 'System check'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-07-07 08:20:00', 'RESOLVED', 5, '5', 1, 30, '2026-07-07 08:20:30', 'user1', 'Maintenance'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-07-10 13:10:00', 'RESOLVED', 6, '6', 1, 150, '2026-07-10 13:12:30', 'admin', 'Vault check'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-07-12 07:45:00', 'RESOLVED', 7, '7', 1, 75, '2026-07-12 07:46:15', 'user1', 'Exit opened'),
('Zone 8 Alarm!', 'Zone 8 Alarm!', '2026-07-15 10:30:00', 'RESOLVED', 8, '8', 1, 40, '2026-07-15 10:30:40', 'admin', 'Parking motion'),
('Zone 9 Alarm!', 'Zone 9 Alarm!', '2026-07-18 15:20:00', 'RESOLVED', 9, '9', 1, 55, '2026-07-18 15:20:55', 'user1', 'Store room'),
('Zone 10 Alarm!', 'Zone 10 Alarm!', '2026-07-20 12:00:00', 'RESOLVED', 10, '10', 1, 85, '2026-07-20 12:01:25', 'admin', 'Rest room'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-07-04 10:00:00', 'RESOLVED', 1, '1', 2, 50, '2026-07-04 10:00:50', 'user2', 'Front door'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-07-06 14:45:00', 'RESOLVED', 2, '2', 2, 35, '2026-07-06 14:45:35', 'admin', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-07-08 11:20:00', 'RESOLVED', 3, '3', 2, 95, '2026-07-08 11:21:35', 'user2', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-07-11 08:30:00', 'RESOLVED', 4, '4', 2, 60, '2026-07-11 08:31:00', 'admin', 'Server room'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-07-13 16:30:00', 'RESOLVED', 5, '5', 2, 110, '2026-07-13 16:31:50', 'user2', 'Back office'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-07-16 09:45:00', 'RESOLVED', 6, '6', 2, 70, '2026-07-16 09:46:10', 'admin', 'Vault room'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-07-19 12:30:00', 'RESOLVED', 7, '7', 2, 45, '2026-07-19 12:30:45', 'user2', 'Emergency exit'),
('Zone 8 Alarm!', 'Zone 8 Alarm!', '2026-07-22 15:15:00', 'RESOLVED', 8, '8', 2, 80, '2026-07-22 15:16:20', 'admin', 'Parking area'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-07-09 07:30:00', 'RESOLVED', 1, '1', 3, 65, '2026-07-09 07:31:05', 'user1', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-07-14 13:20:00', 'RESOLVED', 2, '2', 3, 40, '2026-07-14 13:20:40', 'admin', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-07-17 10:00:00', 'RESOLVED', 3, '3', 3, 55, '2026-07-17 10:00:55', 'user1', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-07-21 16:20:00', 'RESOLVED', 4, '4', 3, 100, '2026-07-21 16:21:40', 'admin', 'Server room'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-07-23 08:40:00', 'RESOLVED', 5, '5', 3, 35, '2026-07-23 08:40:35', 'user1', 'Back office'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-07-25 11:30:00', 'RESOLVED', 6, '6', 3, 70, '2026-07-25 11:31:10', 'admin', 'Vault room'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-07-27 14:50:00', 'RESOLVED', 7, '7', 3, 45, '2026-07-27 14:50:45', 'user1', 'Emergency exit'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-07-24 09:10:00', 'RESOLVED', 1, '1', 4, 90, '2026-07-24 09:11:30', 'admin', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-07-26 14:30:00', 'RESOLVED', 2, '2', 4, 45, '2026-07-26 14:30:45', 'user1', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-07-28 09:45:00', 'RESOLVED', 3, '3', 4, 70, '2026-07-28 09:46:10', 'admin', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-07-29 17:20:00', 'RESOLVED', 4, '4', 4, 55, '2026-07-29 17:20:55', 'user1', 'Server room'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-07-30 10:15:00', 'RESOLVED', 5, '5', 4, 60, '2026-07-30 10:16:00', 'admin', 'Back office'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-07-31 08:00:00', 'RESOLVED', 1, '1', 5, 50, '2026-07-31 08:00:50', 'user2', 'Main entrance');

-- ============================================================
-- ARM & DISARM Events - July
-- ============================================================

-- ARM Events
INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id) VALUES
('ARM', '8888#1A - System Armed', '2026-07-01 07:00:00', 'ARMED', 0, '00', 1),
('ARM', '8888#1A - System Armed', '2026-07-02 07:00:00', 'ARMED', 0, '00', 2),
('ARM', '8888#1A - System Armed', '2026-07-03 07:00:00', 'ARMED', 0, '00', 3),
('ARM', '8888#1A - System Armed', '2026-07-04 07:00:00', 'ARMED', 0, '00', 4),
('ARM', '8888#1A - System Armed', '2026-07-05 07:00:00', 'ARMED', 0, '00', 5),
('ARM', '8888#1A - System Armed', '2026-07-06 07:00:00', 'ARMED', 0, '00', 1),
('ARM', '8888#1A - System Armed', '2026-07-07 07:00:00', 'ARMED', 0, '00', 2);

-- DISARM Events
INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, resolved_at, resolved_by, resolution_description) VALUES
('DISARM', '8888#2A - System Disarmed', '2026-07-01 18:00:00', 'RESOLVED', 0, '00', 1, '2026-07-01 18:00:00', 'user1', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-07-02 18:00:00', 'RESOLVED', 0, '00', 2, '2026-07-02 18:00:00', 'user2', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-07-03 18:00:00', 'RESOLVED', 0, '00', 3, '2026-07-03 18:00:00', 'user1', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-07-04 18:00:00', 'RESOLVED', 0, '00', 4, '2026-07-04 18:00:00', 'admin', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-07-05 18:00:00', 'RESOLVED', 0, '00', 5, '2026-07-05 18:00:00', 'user2', 'System disarmed - end of day');

-- ============================================================
-- SIREN_STOP Events - July
-- ============================================================

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, resolved_at, resolved_by, resolution_description) VALUES
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-07-10 12:30:00', 'SIREN_STOP', 0, '00', 1, '2026-07-10 12:30:00', 'admin', 'Siren stopped - false alarm'),
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-07-15 14:20:00', 'SIREN_STOP', 0, '00', 3, '2026-07-15 14:20:00', 'user1', 'Siren stopped - maintenance'),
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-07-20 09:15:00', 'SIREN_STOP', 0, '00', 5, '2026-07-20 09:15:00', 'user2', 'Siren stopped - test'),
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-07-25 16:40:00', 'SIREN_STOP', 0, '00', 2, '2026-07-25 16:40:00', 'admin', 'Siren stopped - false alarm');

-- ============================================================
-- CALL Events - July
-- ============================================================

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id) VALUES
('CALL', 'Incoming call from 0771234567', '2026-07-11 10:30:00', 'CALL', 0, '00', 1),
('CALL', 'Incoming call from 0782582258', '2026-07-18 14:15:00', 'CALL', 0, '00', 2),
('CALL', 'Incoming call from 0771529201', '2026-07-22 11:45:00', 'CALL', 0, '00', 4),
('CALL', 'Incoming call from 0714868100', '2026-07-28 09:30:00', 'CALL', 0, '00', 5);



-- ============================================================
-- AUGUST 2026 - Alert Logs (1-28)
-- ============================================================

-- System 1 - Zone Alarms
INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, pending_duration_seconds, resolved_at, resolved_by, resolution_description) VALUES
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-08-01 08:15:00', 'RESOLVED', 1, '1', 1, 45, '2026-08-01 08:15:45', 'user1', 'Morning check'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-08-02 14:30:00', 'RESOLVED', 2, '2', 1, 60, '2026-08-02 14:31:00', 'admin', 'Security test'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-08-03 09:45:00', 'RESOLVED', 3, '3', 1, 30, '2026-08-03 09:45:30', 'user1', 'False alarm'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-08-04 16:20:00', 'RESOLVED', 4, '4', 1, 90, '2026-08-04 16:21:30', 'admin', 'Server room check'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-08-05 10:00:00', 'RESOLVED', 5, '5', 1, 35, '2026-08-05 10:00:35', 'user1', 'Back office'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-08-06 13:10:00', 'RESOLVED', 6, '6', 1, 150, '2026-08-06 13:12:30', 'admin', 'Vault room'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-08-07 07:30:00', 'RESOLVED', 7, '7', 1, 75, '2026-08-07 07:31:15', 'user1', 'Emergency exit'),
('Zone 8 Alarm!', 'Zone 8 Alarm!', '2026-08-08 11:20:00', 'RESOLVED', 8, '8', 1, 40, '2026-08-08 11:20:40', 'admin', 'Parking area'),
('Zone 9 Alarm!', 'Zone 9 Alarm!', '2026-08-09 15:30:00', 'RESOLVED', 9, '9', 1, 55, '2026-08-09 15:30:55', 'user1', 'Store room'),
('Zone 10 Alarm!', 'Zone 10 Alarm!', '2026-08-10 12:00:00', 'RESOLVED', 10, '10', 1, 85, '2026-08-10 12:01:25', 'admin', 'Rest room'),

-- System 2 - Zone Alarms
INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, pending_duration_seconds, resolved_at, resolved_by, resolution_description) VALUES
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-08-11 09:00:00', 'RESOLVED', 1, '1', 2, 50, '2026-08-11 09:00:50', 'user2', 'Front door'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-08-12 14:45:00', 'RESOLVED', 2, '2', 2, 35, '2026-08-12 14:45:35', 'admin', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-08-13 10:30:00', 'RESOLVED', 3, '3', 2, 95, '2026-08-13 10:31:35', 'user2', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-08-14 08:20:00', 'RESOLVED', 4, '4', 2, 60, '2026-08-14 08:21:00', 'admin', 'Server room'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-08-15 16:30:00', 'RESOLVED', 5, '5', 2, 110, '2026-08-15 16:31:50', 'user2', 'Back office'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-08-16 09:45:00', 'RESOLVED', 6, '6', 2, 70, '2026-08-16 09:46:10', 'admin', 'Vault room'),
('Zone 7 Alarm!', 'Zone 7 Alarm!', '2026-08-17 12:30:00', 'RESOLVED', 7, '7', 2, 45, '2026-08-17 12:30:45', 'user2', 'Emergency exit'),
('Zone 8 Alarm!', 'Zone 8 Alarm!', '2026-08-18 15:15:00', 'RESOLVED', 8, '8', 2, 80, '2026-08-18 15:16:20', 'admin', 'Parking area'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-08-19 07:10:00', 'RESOLVED', 1, '1', 3, 65, '2026-08-19 07:11:05', 'user1', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-08-20 13:20:00', 'RESOLVED', 2, '2', 3, 40, '2026-08-20 13:20:40', 'admin', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-08-21 10:00:00', 'RESOLVED', 3, '3', 3, 55, '2026-08-21 10:00:55', 'user1', 'Lobby'),
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-08-22 16:20:00', 'RESOLVED', 4, '4', 3, 100, '2026-08-22 16:21:40', 'admin', 'Server room'),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-08-23 08:40:00', 'RESOLVED', 5, '5', 3, 35, '2026-08-23 08:40:35', 'user1', 'Back office'),
('Zone 6 Alarm!', 'Zone 6 Alarm!', '2026-08-24 11:30:00', 'RESOLVED', 6, '6', 3, 70, '2026-08-24 11:31:10', 'admin', 'Vault room'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-08-25 09:10:00', 'RESOLVED', 1, '1', 4, 90, '2026-08-25 09:11:30', 'admin', 'Main entrance'),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-08-26 14:30:00', 'RESOLVED', 2, '2', 4, 45, '2026-08-26 14:30:45', 'user1', 'Cash counter'),
('Zone 3 Alarm!', 'Zone 3 Alarm!', '2026-08-27 09:45:00', 'RESOLVED', 3, '3', 4, 70, '2026-08-27 09:46:10', 'admin', 'Lobby'),
('Zone 1 Alarm!', 'Zone 1 Alarm!', '2026-08-28 08:15:00', 'RESOLVED', 1, '1', 5, 50, '2026-08-28 08:16:00', 'user2', 'Main entrance');

-- ============================================================
-- PENDING ALERTS (3 alerts - not resolved)
-- ============================================================

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id) VALUES
('Zone 4 Alarm!', 'Zone 4 Alarm!', '2026-08-28 10:30:00', 'PENDING', 4, '4', 1),
('Zone 2 Alarm!', 'Zone 2 Alarm!', '2026-08-28 11:15:00', 'PENDING', 2, '2', 3),
('Zone 5 Alarm!', 'Zone 5 Alarm!', '2026-08-28 12:00:00', 'PENDING', 5, '5', 5);

-- ============================================================
-- ARM & DISARM Events - August
-- ============================================================

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id) VALUES
('ARM', '8888#1A - System Armed', '2026-08-01 07:00:00', 'ARMED', 0, '00', 1),
('ARM', '8888#1A - System Armed', '2026-08-02 07:00:00', 'ARMED', 0, '00', 2),
('ARM', '8888#1A - System Armed', '2026-08-03 07:00:00', 'ARMED', 0, '00', 3),
('ARM', '8888#1A - System Armed', '2026-08-04 07:00:00', 'ARMED', 0, '00', 4),
('ARM', '8888#1A - System Armed', '2026-08-05 07:00:00', 'ARMED', 0, '00', 5);

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, resolved_at, resolved_by, resolution_description) VALUES
('DISARM', '8888#2A - System Disarmed', '2026-08-01 18:00:00', 'RESOLVED', 0, '00', 1, '2026-08-01 18:00:00', 'user1', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-08-02 18:00:00', 'RESOLVED', 0, '00', 2, '2026-08-02 18:00:00', 'user2', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-08-03 18:00:00', 'RESOLVED', 0, '00', 3, '2026-08-03 18:00:00', 'user1', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-08-04 18:00:00', 'RESOLVED', 0, '00', 4, '2026-08-04 18:00:00', 'admin', 'System disarmed - end of day'),
('DISARM', '8888#2A - System Disarmed', '2026-08-05 18:00:00', 'RESOLVED', 0, '00', 5, '2026-08-05 18:00:00', 'user2', 'System disarmed - end of day');

-- ============================================================
-- SIREN_STOP Events - August
-- ============================================================

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id, resolved_at, resolved_by, resolution_description) VALUES
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-08-07 09:30:00', 'SIREN_STOP', 0, '00', 2, '2026-08-07 09:30:00', 'admin', 'Siren stopped - false alarm'),
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-08-14 14:15:00', 'SIREN_STOP', 0, '00', 4, '2026-08-14 14:15:00', 'user1', 'Siren stopped - maintenance'),
('SIREN_STOP', '8888#5A - Siren Stopped', '2026-08-21 10:45:00', 'SIREN_STOP', 0, '00', 1, '2026-08-21 10:45:00', 'admin', 'Siren stopped - test');

-- ============================================================
-- CALL Events - August
-- ============================================================

INSERT INTO alert_logs (alert_type, raw_message, received_at, status, zone_number, zone_numbers, system_id) VALUES
('CALL', 'Incoming call from 0771234567', '2026-08-05 10:15:00', 'CALL', 0, '00', 1),
('CALL', 'Incoming call from 0782582258', '2026-08-12 14:30:00', 'CALL', 0, '00', 3),
('CALL', 'Incoming call from 0771529201', '2026-08-19 11:00:00', 'CALL', 0, '00', 5),
('CALL', 'Incoming call from 0714868100', '2026-08-25 09:45:00', 'CALL', 0, '00', 2);



-- Run June queries
-- Run July queries  
-- Run August queries

-- Verify count
SELECT COUNT(*) FROM alert_logs;
-- Expected: ~140 rows

-- Check distribution
SELECT 
    DATE(received_at) as date,
    COUNT(*) as count,
    status
FROM alert_logs 
GROUP BY DATE(received_at), status
ORDER BY date DESC;