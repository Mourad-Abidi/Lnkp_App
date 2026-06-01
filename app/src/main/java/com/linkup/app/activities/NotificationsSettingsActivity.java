package com.linkup.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;

public class NotificationsSettingsActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_notifications);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupAlertsAndSounds();
        setupAdvancedNotifications();
        setupManagement();
    }

    private void setupAlertsAndSounds() {
        findViewById(R.id.btnPreviewMessageSound).setOnClickListener(v -> 
            Toast.makeText(this, "Playing Message Sound: Skyline", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.btnPreviewCallRingtone).setOnClickListener(v -> 
            Toast.makeText(this, "Playing Ringtone: LinkUp Remix", Toast.LENGTH_SHORT).show());

        SwitchMaterial switchVibrate = findViewById(R.id.switchVibrateNotifications);
        if (switchVibrate != null) {
            switchVibrate.setChecked(settingsPrefs.getBoolean("notif_vibrate", true));
            switchVibrate.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("notif_vibrate", isChecked).apply());
        }
    }

    private void setupAdvancedNotifications() {
        SwitchMaterial switchPopup = findViewById(R.id.switchPopupNotifications);
        if (switchPopup != null) {
            switchPopup.setChecked(settingsPrefs.getBoolean("notif_popup", false));
            switchPopup.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("notif_popup", isChecked).apply());
        }

        RadioGroup rgPreview = findViewById(R.id.rgNotificationPreview);
        if (rgPreview != null) {
            boolean hideContent = settingsPrefs.getBoolean("notif_hide_content", false);
            if (hideContent) {
                ((RadioButton)findViewById(R.id.rbHideContent)).setChecked(true);
            } else {
                ((RadioButton)findViewById(R.id.rbShowContent)).setChecked(true);
            }

            rgPreview.setOnCheckedChangeListener((group, checkedId) -> {
                boolean hide = (checkedId == R.id.rbHideContent);
                settingsPrefs.edit().putBoolean("notif_hide_content", hide).apply();
            });
        }
    }

    private void setupManagement() {
        View tvMuteGroups = findViewById(R.id.tvMuteGroups);
        if (tvMuteGroups != null) {
            tvMuteGroups.setOnClickListener(v -> {
                Toast.makeText(this, "Group muting settings opened", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
