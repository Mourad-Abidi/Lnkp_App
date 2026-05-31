package com.linkup.app.models;

import com.google.gson.Gson;
import java.util.List;

/**
 * A master model representing the entire database state for JSON export/import.
 */
public class DatabaseExportModel {
    private List<User> users;
    private List<Message> chats;
    private List<Media> media;
    private List<Call> calls;
    private List<Security> security;
    private List<Settings> settings;
    private List<Device> devices;
    private List<HiddenChat> hiddenChats;
    private List<BlockedUser> blockedUsers;
    private List<Backup> backups;

    public DatabaseExportModel() {}

    // Getters and Setters
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public List<Message> getChats() { return chats; }
    public void setChats(List<Message> chats) { this.chats = chats; }

    public List<Media> getMedia() { return media; }
    public void setMedia(List<Media> media) { this.media = media; }

    public List<Call> getCalls() { return calls; }
    public void setCalls(List<Call> calls) { this.calls = calls; }

    public List<Security> getSecurity() { return security; }
    public void setSecurity(List<Security> security) { this.security = security; }

    public List<Settings> getSettings() { return settings; }
    public void setSettings(List<Settings> settings) { this.settings = settings; }

    public List<Device> getDevices() { return devices; }
    public void setDevices(List<Device> devices) { this.devices = devices; }

    public List<HiddenChat> getHiddenChats() { return hiddenChats; }
    public void setHiddenChats(List<HiddenChat> hiddenChats) { this.hiddenChats = hiddenChats; }

    public List<BlockedUser> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(List<BlockedUser> blockedUsers) { this.blockedUsers = blockedUsers; }

    public List<Backup> getBackups() { return backups; }
    public void setBackups(List<Backup> backups) { this.backups = backups; }

    /**
     * Converts the entire database structure to a single JSON string.
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * Creates a database export model from a JSON string.
     */
    public static DatabaseExportModel fromJson(String json) {
        return new Gson().fromJson(json, DatabaseExportModel.class);
    }
}
