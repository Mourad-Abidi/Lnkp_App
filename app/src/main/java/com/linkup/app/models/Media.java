package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing media associated with messages.
 */
public class Media {
    private long mediaId;
    private long chatId;
    private String filePath;
    private String mediaType;
    private long fileSize;
    private long duration;
    private String quality;

    public Media() {}

    public long getMediaId() { return mediaId; }
    public void setMediaId(long mediaId) { this.mediaId = mediaId; }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    // JSON Conversion
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Media fromJson(String json) {
        return new Gson().fromJson(json, Media.class);
    }
}
