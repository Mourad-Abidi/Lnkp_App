package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing user-specific settings.
 */
public class Settings {
    private long settingId;
    private long userId;
    private String theme;
    private String fontSize;
    private String wallpaper;
    private boolean notificationsEnabled;
    private boolean vibrationEnabled;
    private String ringtone;
    private boolean screenshotProtection;
    private boolean readReceipts;
    private String autoDownload;
    private String mediaQuality;

    public Settings() {}

    public long getSettingId() { return settingId; }
    public void setSettingId(long settingId) { this.settingId = settingId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }

    public String getWallpaper() { return wallpaper; }
    public void setWallpaper(String wallpaper) { this.wallpaper = wallpaper; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public boolean isVibrationEnabled() { return vibrationEnabled; }
    public void setVibrationEnabled(boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }

    public String getRingtone() { return ringtone; }
    public void setRingtone(String ringtone) { this.ringtone = ringtone; }

    public boolean isScreenshotProtection() { return screenshotProtection; }
    public void setScreenshotProtection(boolean screenshotProtection) { this.screenshotProtection = screenshotProtection; }

    public boolean isReadReceipts() { return readReceipts; }
    public void setReadReceipts(boolean readReceipts) { this.readReceipts = readReceipts; }

    public String getAutoDownload() { return autoDownload; }
    public void setAutoDownload(String autoDownload) { this.autoDownload = autoDownload; }

    public String getMediaQuality() { return mediaQuality; }
    public void setMediaQuality(String mediaQuality) { this.mediaQuality = mediaQuality; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Settings fromJson(String json) {
        return new Gson().fromJson(json, Settings.class);
    }
}
