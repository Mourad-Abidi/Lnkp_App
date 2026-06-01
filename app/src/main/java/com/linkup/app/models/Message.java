package com.linkup.app.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private String messageId;
    
    @SerializedName("sender_id")
    private String senderId;
    
    @SerializedName("receiver_id")
    private String receiverId;
    
    @SerializedName("message_text")
    private String messageText;
    
    @SerializedName("message_type")
    private String messageType; // TEXT, IMAGE, VIDEO, VOICE, FILE
    
    @SerializedName("media_url")
    private String mediaUrl;
    
    @SerializedName("file_name")
    private String fileName;
    
    @SerializedName("file_size")
    private String fileSize;
    
    @SerializedName("timestamp")
    private long timestamp;
    
    @SerializedName("read_status")
    private String readStatus; // "SENT", "DELIVERED", "READ"

    public Message() {}

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    
    public String getEncryptedContent() { return messageText; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Message fromJson(String json) {
        return new Gson().fromJson(json, Message.class);
    }
}
