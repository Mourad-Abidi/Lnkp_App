package com.linkup.app.activities;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;

public class PrivacySettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_privacy);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupVisibilitySettings();
        setupMessagingSettings();
        setupManagementSettings();
    }

    private void setupVisibilitySettings() {
        RadioGroup rgLastSeen = findViewById(R.id.rgLastSeen);
        if (rgLastSeen != null) {
            int lastSeenType = settingsPrefs.getInt("privacy_last_seen", 1); // 0: Everyone, 1: Contacts, 2: Nobody
            if (lastSeenType == 0) rgLastSeen.check(R.id.rbLastSeenEveryone);
            else if (lastSeenType == 1) rgLastSeen.check(R.id.rbLastSeenContacts);
            else rgLastSeen.check(R.id.rbLastSeenNobody);

            rgLastSeen.setOnCheckedChangeListener((group, checkedId) -> {
                int type = 1;
                if (checkedId == R.id.rbLastSeenEveryone) type = 0;
                else if (checkedId == R.id.rbLastSeenNobody) type = 2;
                settingsPrefs.edit().putInt("privacy_last_seen", type).apply();
            });
        }

        SwitchMaterial switchOnlineStatus = findViewById(R.id.switchOnlineStatus);
        if (switchOnlineStatus != null) {
            switchOnlineStatus.setChecked(settingsPrefs.getBoolean("privacy_online_status", true));
            switchOnlineStatus.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("privacy_online_status", isChecked).apply());
        }

        RadioGroup rgProfilePhoto = findViewById(R.id.rgProfilePhoto);
        if (rgProfilePhoto != null) {
            int profileType = settingsPrefs.getInt("privacy_profile_photo", 0);
            if (profileType == 0) rgProfilePhoto.check(R.id.rbProfileEveryone);
            else if (profileType == 1) rgProfilePhoto.check(R.id.rbProfileContacts);
            else rgProfilePhoto.check(R.id.rbProfileNobody);

            rgProfilePhoto.setOnCheckedChangeListener((group, checkedId) -> {
                int type = 0;
                if (checkedId == R.id.rbProfileContacts) type = 1;
                else if (checkedId == R.id.rbProfileNobody) type = 2;
                settingsPrefs.edit().putInt("privacy_profile_photo", type).apply();
            });
        }
    }

    private void setupMessagingSettings() {
        SwitchMaterial switchReadReceipts = findViewById(R.id.switchReadReceipts);
        if (switchReadReceipts != null) {
            switchReadReceipts.setChecked(settingsPrefs.getBoolean("privacy_read_receipts", true));
            switchReadReceipts.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("privacy_read_receipts", isChecked).apply());
        }

        SwitchMaterial switchScreenshot = findViewById(R.id.switchScreenshotProtection);
        if (switchScreenshot != null) {
            switchScreenshot.setChecked(settingsPrefs.getBoolean("privacy_screenshot_protection", false));
            switchScreenshot.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("privacy_screenshot_protection", isChecked).apply());
        }
    }

    private void setupManagementSettings() {
        findViewById(R.id.tvBlockedUsers).setOnClickListener(v -> 
            Toast.makeText(this, "Blocked users list opened", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.tvHiddenChats).setOnClickListener(v -> 
            Toast.makeText(this, "Hidden chats list opened", Toast.LENGTH_SHORT).show());
    }
}
