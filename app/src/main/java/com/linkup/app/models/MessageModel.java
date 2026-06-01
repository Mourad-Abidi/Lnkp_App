package com.linkup.app.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages", indices = {@Index(value = {"cloudId"}, unique = true)})
public class MessageModel {
    public enum MessageType {
        TEXT, VOICE, IMAGE, STICKER, GIF, VIDEO, FILE, PULSE
    }

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String cloudId; // Supabase UUID
    private String message;
    private String time; // Formatted HH:mm
    private long timestamp; // Unix timestamp in millis
    private boolean isSent;
    private MessageType type;
    private String duration;
    private String chatPartnerName;
    private String chatPartnerId; 
    private String mediaUrl;
    private String senderName;
    
    private int replyToId = -1;
    private String replyToName;
    private String replyToContent;

    private boolean isSeen;
    private String originalSenderName;
    private String reaction;
    
    private boolean isWeighted;
    private String pulsePattern;
    
    @Ignore
    private boolean isSelected = false;

    public MessageModel() {
    }

    public MessageModel(String message, String time, boolean isSent, MessageType type, String duration, String chatPartnerName) {
        this.message = message;
        this.time = time;
        this.isSent = isSent;
        this.type = type;
        this.duration = duration;
        this.chatPartnerName = chatPartnerName;
        this.isSeen = false;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCloudId() { return cloudId; }
    public void setCloudId(String cloudId) { this.cloudId = cloudId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getChatPartnerName() { return chatPartnerName; }
    public void setChatPartnerName(String chatPartnerName) { this.chatPartnerName = chatPartnerName; }

    public String getChatPartnerId() { return chatPartnerId; }
    public void setChatPartnerId(String chatPartnerId) { this.chatPartnerId = chatPartnerId; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public int getReplyToId() { return replyToId; }
    public void setReplyToId(int replyToId) { this.replyToId = replyToId; }

    public String getReplyToName() { return replyToName; }
    public void setReplyToName(String replyToName) { this.replyToName = replyToName; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public boolean isSeen() { return isSeen; }
    public void setSeen(boolean seen) { isSeen = seen; }

    public String getOriginalSenderName() { return originalSenderName; }
    public void setOriginalSenderName(String originalSenderName) { this.originalSenderName = originalSenderName; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public boolean isWeighted() { return isWeighted; }
    public void setWeighted(boolean weighted) { isWeighted = weighted; }

    public String getPulsePattern() { return pulsePattern; }
    public void setPulsePattern(String pulsePattern) { this.pulsePattern = pulsePattern; }
}
