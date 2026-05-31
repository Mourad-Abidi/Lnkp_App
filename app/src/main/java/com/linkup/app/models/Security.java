package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing security configurations for a user.
 */
public class Security {
    private long securityId;
    private long userId;
    private String passwordChangedDate;
    private String activePin;
    private boolean activeFingerprint;
    private boolean activeFaceUnlock;
    private boolean active2FA;
    private String trustedDevices; // JSON string
    private String sessionHistory; // JSON string
    private boolean emergencyLockStatus;

    public Security() {}

    public long getSecurityId() { return securityId; }
    public void setSecurityId(long securityId) { this.securityId = securityId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getPasswordChangedDate() { return passwordChangedDate; }
    public void setPasswordChangedDate(String passwordChangedDate) { this.passwordChangedDate = passwordChangedDate; }

    public String getActivePin() { return activePin; }
    public void setActivePin(String activePin) { this.activePin = activePin; }

    public boolean isActiveFingerprint() { return activeFingerprint; }
    public void setActiveFingerprint(boolean activeFingerprint) { this.activeFingerprint = activeFingerprint; }

    public boolean isActiveFaceUnlock() { return activeFaceUnlock; }
    public void setActiveFaceUnlock(boolean activeFaceUnlock) { this.activeFaceUnlock = activeFaceUnlock; }

    public boolean isActive2FA() { return active2FA; }
    public void setActive2FA(boolean active2FA) { this.active2FA = active2FA; }

    public String getTrustedDevices() { return trustedDevices; }
    public void setTrustedDevices(String trustedDevices) { this.trustedDevices = trustedDevices; }

    public String getSessionHistory() { return sessionHistory; }
    public void setSessionHistory(String sessionHistory) { this.sessionHistory = sessionHistory; }

    public boolean isEmergencyLockStatus() { return emergencyLockStatus; }
    public void setEmergencyLockStatus(boolean emergencyLockStatus) { this.emergencyLockStatus = emergencyLockStatus; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Security fromJson(String json) {
        return new Gson().fromJson(json, Security.class);
    }
}
