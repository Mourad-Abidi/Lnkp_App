package com.linkup.app.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Post {
    @SerializedName("id")
    public String postId;
    
    @SerializedName("user_name")
    public String userName;
    
    @SerializedName("user_id")
    public String userId;
    
    @SerializedName("content")
    public String content;
    
    @SerializedName("media_url")
    public String mediaPath;
    
    @SerializedName("created_at")
    public String timestamp;
    
    @SerializedName("expiry_at")
    public String expiryTimestamp;
    
    @SerializedName("is_seen")
    public boolean hasBeenSeen = false;

    public Post() {
        long now = System.currentTimeMillis();
        this.timestamp = String.valueOf(now);
        this.expiryTimestamp = String.valueOf(now + 86400000);
    }

    public Post(String postId, String userName, String userId, String content, String mediaPath, long expiryTimestamp) {
        this.postId = postId;
        this.userName = userName;
        this.userId = userId;
        this.content = content;
        this.mediaPath = mediaPath;
        this.expiryTimestamp = String.valueOf(expiryTimestamp);
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }

    public long getTimestampMillis() {
        return parseDate(timestamp, System.currentTimeMillis());
    }

    public long getExpiryTimestampMillis() {
        long ts = getTimestampMillis();
        return parseDate(expiryTimestamp, ts + 86400000);
    }

    private long parseDate(String dateStr, long fallback) {
        if (dateStr == null || dateStr.isEmpty() || "null".equalsIgnoreCase(dateStr)) return fallback;
        
        try {
            return Long.parseLong(dateStr);
        } catch (NumberFormatException ignored) {}

        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss"
        };

        // Pre-process ISO string to handle 'Z' and fractional seconds manually if needed
        String processed = dateStr.replace("Z", "").split("\\+")[0];
        if (processed.contains(".")) {
            int dotIndex = processed.lastIndexOf(".");
            if (processed.length() - dotIndex > 4) {
                processed = processed.substring(0, dotIndex + 4); // Keep only 3 decimal places for SSS
            }
        }

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(processed);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {}
        }

        return fallback;
    }
}
