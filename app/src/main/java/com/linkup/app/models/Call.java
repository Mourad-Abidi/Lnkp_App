package com.linkup.app.models;

import com.google.gson.Gson;

/**
 * Model representing voice and video calls.
 */
public class Call {
    private long callId;
    private long callerId;
    private long receiverId;
    private String callType; // "VOICE" or "VIDEO"
    private String callStatus; // "MISSED", "COMPLETED", "REJECTED"
    private long duration;
    private long timestamp;

    public Call() {
    }

    public long getCallId() { return callId; }
    public void setCallId(long callId) { this.callId = callId; }

    public long getCallerId() { return callerId; }
    public void setCallerId(long callerId) { this.callerId = callerId; }

    public long getReceiverId() { return receiverId; }
    public void setReceiverId(long receiverId) { this.receiverId = receiverId; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // JSON Conversion
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Call fromJson(String json) {
        return new Gson().fromJson(json, Call.class);
    }
}
