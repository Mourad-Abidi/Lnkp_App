package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing chats that are hidden from the main list.
 */
public class HiddenChat {
    private long hiddenChatId;
    private long userId;
    private long protectedChatId;
    private String unlockMethod; // "PIN", "FINGERPRINT", "FACE"

    public HiddenChat() {}

    public long getHiddenChatId() { return hiddenChatId; }
    public void setHiddenChatId(long hiddenChatId) { this.hiddenChatId = hiddenChatId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getProtectedChatId() { return protectedChatId; }
    public void setProtectedChatId(long protectedChatId) { this.protectedChatId = protectedChatId; }

    public String getUnlockMethod() { return unlockMethod; }
    public void setUnlockMethod(String unlockMethod) { this.unlockMethod = unlockMethod; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static HiddenChat fromJson(String json) {
        return new Gson().fromJson(json, HiddenChat.class);
    }
}
