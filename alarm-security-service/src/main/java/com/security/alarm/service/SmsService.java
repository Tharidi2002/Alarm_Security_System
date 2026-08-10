package com.security.alarm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${sms.gateway.type:http}")
    private String gatewayType;

    @Value("${sms.gateway.http.url:}")
    private String httpGatewayUrl;

    @Value("${sms.gateway.http.api-key:}")
    private String apiKey;

    @Value("${sms.gateway.http.sender-id:ALARM-SYS}")
    private String senderId;

    @Value("${sms.gateway.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${sms.gateway.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${sms.gateway.twilio.phone-number:}")
    private String twilioPhoneNumber;

    /**
     * Send SMS to the specified phone number
     */
    public boolean sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.error("Phone number is empty");
            return false;
        }

        if (message == null || message.trim().isEmpty()) {
            logger.error("Message is empty");
            return false;
        }

        logger.info("Sending SMS to: {}, message: {}", phoneNumber, message);

        try {
            switch (gatewayType.toLowerCase()) {
                case "twilio":
                    return sendViaTwilio(phoneNumber, message);
                case "http":
                    return sendViaHttp(phoneNumber, message);
                default:
                    logger.error("Unknown SMS gateway type: {}", gatewayType);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Failed to send SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send SMS via HTTP Gateway
     */
    private boolean sendViaHttp(String phoneNumber, String message) throws Exception {
        String encodedMessage = URLEncoder.encode(message, "UTF-8");
        String urlString = httpGatewayUrl + 
            "?apiKey=" + apiKey + 
            "&to=" + phoneNumber + 
            "&from=" + senderId + 
            "&text=" + encodedMessage;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        boolean success = responseCode >= 200 && responseCode < 300;

        if (success) {
            logger.info("SMS sent successfully via HTTP Gateway");
        } else {
            logger.error("HTTP Gateway returned error code: {}", responseCode);
        }

        conn.disconnect();
        return success;
    }

    /**
     * Send SMS via Twilio
     */
    private boolean sendViaTwilio(String phoneNumber, String message) {
        try {
            // Twilio SDK implementation
            logger.warn("Twilio not configured - using fallback HTTP gateway");
            return sendViaHttp(phoneNumber, message);
        } catch (Exception e) {
            logger.error("Twilio send failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send command to Z8B Panel
     */
    public boolean sendPanelCommand(String panelSimNumber, String panelPassword, String commandCode) {
        if (panelSimNumber == null || panelSimNumber.trim().isEmpty()) {
            logger.error("Panel SIM number is empty");
            return false;
        }

        String command = (panelPassword != null ? panelPassword : "8888") + "#" + commandCode;
        logger.info("Sending panel command: {} to: {}", command, panelSimNumber);
        return sendSms(panelSimNumber, command);
    }

    /**
     * Send Arm command to panel
     */
    public boolean sendArmCommand(String panelSimNumber, String panelPassword) {
        return sendPanelCommand(panelSimNumber, panelPassword, "1A");
    }

    /**
     * Send Disarm command to panel
     */
    public boolean sendDisarmCommand(String panelSimNumber, String panelPassword) {
        return sendPanelCommand(panelSimNumber, panelPassword, "2A");
    }

    /**
     * Send Siren Stop command to panel
     */
    public boolean sendSirenStopCommand(String panelSimNumber, String panelPassword) {
        return sendPanelCommand(panelSimNumber, panelPassword, "5A");
    }

    /**
     * Send Part Arm command to panel
     */
    public boolean sendPartArmCommand(String panelSimNumber, String panelPassword) {
        return sendPanelCommand(panelSimNumber, panelPassword, "3A");
    }

    /**
     * Send SOS command to panel
     */
    public boolean sendSosCommand(String panelSimNumber, String panelPassword) {
        return sendPanelCommand(panelSimNumber, panelPassword, "4A");
    }
}