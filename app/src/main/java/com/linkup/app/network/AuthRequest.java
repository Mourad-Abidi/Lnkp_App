package com.linkup.app.network;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class AuthRequest {
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("data")
    private Map<String, Object> data;

    /**
     * Constructor for Email-based signup with metadata.
     */
    public AuthRequest(String fullName, String email, String password) {
        this.email = email;
        this.password = password;
        this.data = new HashMap<>();
        this.data.put("full_name", fullName);
        this.data.put("email", email);
        // Ensure the profile name is set to the Full Name provided during registration
        this.data.put("username", fullName);
    }

    /**
     * Constructor for Email login.
     */
    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
