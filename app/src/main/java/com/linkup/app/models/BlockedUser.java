package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing a blocked user.
 */
public class BlockedUser {
    private long blockId;
    private long userId;
    private long blockedUserId;
    private String blockedDate;

    public BlockedUser() {}

    public long getBlockId() { return blockId; }
    public void setBlockId(long blockId) { this.blockId = blockId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getBlockedUserId() { return blockedUserId; }
    public void setBlockedUserId(long blockedUserId) { this.blockedUserId = blockedUserId; }

    public String getBlockedDate() { return blockedDate; }
    public void setBlockedDate(String blockedDate) { this.blockedDate = blockedDate; }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static BlockedUser fromJson(String json) {
        return new Gson().fromJson(json, BlockedUser.class);
    }
}
