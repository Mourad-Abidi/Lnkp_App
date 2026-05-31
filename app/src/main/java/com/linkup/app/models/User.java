package com.linkup.app.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private String userId;
    
    @SerializedName("public_key")
    private String publicKey; 
    
    private String username;
    
    @SerializedName("full_name")
    private String fullName;
    
    private String phone;
    private String email;
    
    @SerializedName("password_hash")
    private String passwordHash;
    
    @SerializedName("profile_photo")
    private String profilePhoto;
    
    @SerializedName("account_status")
    private String accountStatus;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("last_login")
    private String lastLogin;
    
    @SerializedName("biometric_enabled")
    private boolean biometricEnabled;
    
    @SerializedName("pin_enabled")
    private boolean pinEnabled;
    
    @SerializedName("face_unlock_enabled")
    private boolean faceUnlockEnabled;
    
    @SerializedName("two_factor_enabled")
    private boolean twoFactorEnabled;

    // Constructors
    public User() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public boolean isBiometricEnabled() { return biometricEnabled; }
    public void setBiometricEnabled(boolean biometricEnabled) { this.biometricEnabled = biometricEnabled; }

    public boolean isPinEnabled() { return pinEnabled; }
    public void setPinEnabled(boolean pinEnabled) { this.pinEnabled = pinEnabled; }

    public boolean isFaceUnlockEnabled() { return faceUnlockEnabled; }
    public void setFaceUnlockEnabled(boolean faceUnlockEnabled) { this.faceUnlockEnabled = faceUnlockEnabled; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    // JSON Conversion
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static User fromJson(String json) {
        return new Gson().fromJson(json, User.class);
    }
}
