package com.linkup.app.network;

import com.google.gson.annotations.SerializedName;
import com.linkup.app.models.User;

public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("expires_in")
    private long expiresIn;
    
    @SerializedName("user")
    private User user;

    @SerializedName("msg")
    private String msg;

    @SerializedName("message")
    private String message;

    @SerializedName("error_description")
    private String errorDescription;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public User getUser() {
        return user;
    }

    /**
     * Attempts to find an error message in the response body
     */
    public String getErrorMessage() {
        if (errorDescription != null) return errorDescription;
        if (message != null) return message;
        if (msg != null) return msg;
        return "Unknown error";
    }

    // Legacy support
    public boolean isError() {
        return getErrorMessage() != null && accessToken == null;
    }
    
    public String getMessage() {
        return getErrorMessage();
    }
}
