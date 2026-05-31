package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing a database backup record.
 */
public class Backup {
    private long backupId;
    private long userId;
    private String backupDate;
    private String backupPath;
    private String backupType; // "LOCAL", "CLOUD"

    public Backup() {}

    public long getBackupId() { return backupId; }
    public void setBackupId(long backupId) { this.backupId = backupId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getBackupDate() { return backupDate; }
    public void setBackupDate(String backupDate) { this.backupDate = backupDate; }

    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }

    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Backup fromJson(String json) {
        return new Gson().fromJson(json, Backup.class);
    }
}
