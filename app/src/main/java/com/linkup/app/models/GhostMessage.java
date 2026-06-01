package com.linkup.app.models;

import com.google.gson.Gson;

public class GhostMessage {
    private String id;
    private String senderId; // Should be "Anonymous" or hidden
    private String receiverId;
    private String receiverName;
    private String content;
    private String contentType; // "TEXT"
    private long scheduledOpenTime;
    private long sentTimestamp;
    private boolean isRead;

    public GhostMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getScheduledOpenTime() { return scheduledOpenTime; }
    public void setScheduledOpenTime(long scheduledOpenTime) { this.scheduledOpenTime = scheduledOpenTime; }

    public long getSentTimestamp() { return sentTimestamp; }
    public void setSentTimestamp(long sentTimestamp) { this.sentTimestamp = sentTimestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static GhostMessage fromJson(String json) {
        return new Gson().fromJson(json, GhostMessage.class);
    }
}
