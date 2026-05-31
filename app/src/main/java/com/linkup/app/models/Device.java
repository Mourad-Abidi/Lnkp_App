package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing a device that has logged into a user's account.
 */
public class Device {
    private long deviceId;
    private long userId;
    private String deviceName;
    private String deviceModel;
    private String loginDate;
    private String ipAddress;
    private String sessionStatus; // "ACTIVE", "EXPIRED", "REVOKED"

    public Device() {}

    public long getDeviceId() { return deviceId; }
    public void setDeviceId(long deviceId) { this.deviceId = deviceId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getLoginDate() { return loginDate; }
    public void setLoginDate(String loginDate) { this.loginDate = loginDate; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Device fromJson(String json) {
        return new Gson().fromJson(json, Device.class);
    }
}
