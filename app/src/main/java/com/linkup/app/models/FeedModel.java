package com.linkup.app.models;

public class FeedModel {
    public enum PostType {
        PHOTO, VIDEO, MOVIE, BOOK, PLACE, LINK, MUSIC, TEXT
    }

    private String userName;
    private String userId; // Owner of the content
    private String content;
    private boolean isOfficial;
    private PostType postType;
    private String timestamp;
    private String categoryTitle; 
    private String imageUrl;

    public FeedModel() {
    }

    public FeedModel(String userName, String userId, String content, boolean isOfficial, PostType postType, String timestamp, String categoryTitle, String imageUrl) {
        this.userName = userName;
        this.userId = userId;
        this.content = content;
        this.isOfficial = isOfficial;
        this.postType = postType;
        this.timestamp = timestamp;
        this.categoryTitle = categoryTitle;
        this.imageUrl = imageUrl;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }

    public PostType getPostType() { return postType; }
    public void setPostType(PostType postType) { this.postType = postType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getCategoryTitle() { return categoryTitle; }
    public void setCategoryTitle(String categoryTitle) { this.categoryTitle = categoryTitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
