package com.linkup.app.models;

/**
 * Model representing a chat session or a search result.
 */
public class ChatModel {
    private String userId; // Supabase UUID
    private String userName;
    private String lastMessage;
    private String time;
    private long lastMessageTimestamp; // Added for sorting
    private String phoneNumber;
    private String ipAddress;
    private String profilePhoto;
    private int unreadCount;
    private boolean isRead = false;
    private boolean hasNewShare = false; 
    private boolean isOnline = false;
    private boolean isSelected = false;
    private boolean isGroup = false;

    public ChatModel() {
    }

    public ChatModel(String userName, String lastMessage, String time, int unreadCount, boolean hasNewShare) {
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unreadCount = unreadCount;
        this.hasNewShare = hasNewShare;
    }

    public ChatModel(String userName, String lastMessage, String time, int unreadCount, boolean hasNewShare, boolean isOnline) {
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unreadCount = unreadCount;
        this.hasNewShare = hasNewShare;
        this.isOnline = isOnline;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }

    public boolean hasNewShare() { return hasNewShare; }
    public void setHasNewShare(boolean hasNewShare) { this.hasNewShare = hasNewShare; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { this.isOnline = online; }
    
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { this.isGroup = group; }

    public void markAsRead() {
        this.isRead = true;
        this.unreadCount = 0;
    }
}
