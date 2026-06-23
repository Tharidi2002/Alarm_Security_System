package com.bank.atm.entity;

public enum AlertStatus {
    PENDING("Pending - New Alert"),
    ACKNOWLEDGED("Acknowledged - Viewed"),
    RESOLVED("Resolved - Action Taken"),
    IGNORED("Ignored - No Action Needed"),
    ESCALATED("Escalated - Sent to Higher Authority");
    
    private final String description;
    
    AlertStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}