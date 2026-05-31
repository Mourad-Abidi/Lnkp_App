package com.linkup.app.models;

public class NotificationModel {
    private String title;
    private String message;
    private String time;
    private int iconRes;
    private int avatarRes;
    private boolean isRead;
    private Class<?> targetActivity;

    public NotificationModel(String title, String message, String time, int iconRes, int avatarRes, boolean isRead, Class<?> targetActivity) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.iconRes = iconRes;
        this.avatarRes = avatarRes;
        this.isRead = isRead;
        this.targetActivity = targetActivity;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public int getIconRes() { return iconRes; }
    public int getAvatarRes() { return avatarRes; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public Class<?> getTargetActivity() { return targetActivity; }
}
