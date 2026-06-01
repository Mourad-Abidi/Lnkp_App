package com.linkup.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "intruder_alerts")
public class IntruderAlert {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long timestamp;
    private String deviceName;
    private String imagePath; // Path to the captured photo

    public IntruderAlert(long timestamp, String deviceName, String imagePath) {
        this.timestamp = timestamp;
        this.deviceName = deviceName;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
