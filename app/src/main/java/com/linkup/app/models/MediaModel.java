package com.linkup.app.models;

public class MediaModel {
    private String mediaUrl;
    private String title;
    private String date;
    private MediaType type;

    public enum MediaType {
        IMAGE, VIDEO, DOCUMENT, LINK
    }

    public MediaModel(String mediaUrl, String title, String date, MediaType type) {
        this.mediaUrl = mediaUrl;
        this.title = title;
        this.date = date;
        this.type = type;
    }

    public String getMediaUrl() { return mediaUrl; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public MediaType getType() { return type; }
}
