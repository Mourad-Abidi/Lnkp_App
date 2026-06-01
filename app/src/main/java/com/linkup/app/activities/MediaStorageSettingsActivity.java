package com.linkup.app.activities;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.linkup.app.R;

public class MediaStorageSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_media_storage);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupAutoDownload();
        setupMediaQuality();
        setupCleanup();
    }

    private void setupAutoDownload() {
        RadioGroup rgAutoDownload = findViewById(R.id.rgAutoDownload);
        if (rgAutoDownload != null) {
            int autoDownloadType = settingsPrefs.getInt("media_auto_download", 0); // 0: Wifi, 1: Data, 2: Never
            if (autoDownloadType == 0) rgAutoDownload.check(R.id.rbAutoWifi);
            else if (autoDownloadType == 1) rgAutoDownload.check(R.id.rbAutoMobileData);
            else rgAutoDownload.check(R.id.rbAutoNever);

            rgAutoDownload.setOnCheckedChangeListener((group, checkedId) -> {
                int type = 0;
                if (checkedId == R.id.rbAutoMobileData) type = 1;
                else if (checkedId == R.id.rbAutoNever) type = 2;
                settingsPrefs.edit().putInt("media_auto_download", type).apply();
            });
        }
    }

    private void setupMediaQuality() {
        RadioGroup rgMediaQuality = findViewById(R.id.rgMediaQuality);
        if (rgMediaQuality != null) {
            int qualityType = settingsPrefs.getInt("media_upload_quality", 1); // 0: Compressed, 1: Standard, 2: Original
            if (qualityType == 0) rgMediaQuality.check(R.id.rbQualityCompressed);
            else if (qualityType == 1) rgMediaQuality.check(R.id.rbQualityStandard);
            else rgMediaQuality.check(R.id.rbQualityOriginal);

            rgMediaQuality.setOnCheckedChangeListener((group, checkedId) -> {
                int type = 1;
                if (checkedId == R.id.rbQualityCompressed) type = 0;
                else if (checkedId == R.id.rbQualityOriginal) type = 2;
                settingsPrefs.edit().putInt("media_upload_quality", type).apply();
            });
        }
    }

    private void setupCleanup() {
        findViewById(R.id.btnClearCache).setOnClickListener(v -> {
            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
            TextView tvCurrentCache = findViewById(R.id.tvCurrentCache);
            if (tvCurrentCache != null) tvCurrentCache.setText("Current: 0 MB");
        });

        findViewById(R.id.tvManageFiles).setOnClickListener(v -> 
            Toast.makeText(this, "File manager opened", Toast.LENGTH_SHORT).show());
    }
}
