package com.linkup.app.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;

public class CallsSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_calls);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupCallSettings();
    }

    private void setupCallSettings() {
        // Incoming Calls Section
        SwitchMaterial switchVibrate = findViewById(R.id.switchVibrate);
        if (switchVibrate != null) {
            switchVibrate.setChecked(settingsPrefs.getBoolean("call_vibrate", true));
            switchVibrate.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("call_vibrate", isChecked).apply());
        }

        Spinner spinnerRingtone = findViewById(R.id.spinnerRingtone);
        if (spinnerRingtone != null) {
            String[] ringtones = {"Default", "Digital", "Marimba", "Neon", "Silent"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ringtones);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRingtone.setAdapter(adapter);
            spinnerRingtone.setSelection(settingsPrefs.getInt("ringtone_pos", 0));
            spinnerRingtone.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    settingsPrefs.edit().putInt("ringtone_pos", pos).apply();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        // Call Quality Section
        SwitchMaterial switchDataSaver = findViewById(R.id.switchDataSaver);
        if (switchDataSaver != null) {
            switchDataSaver.setChecked(settingsPrefs.getBoolean("call_data_saver", false));
            switchDataSaver.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("call_data_saver", isChecked).apply());
        }

        // Privacy Section
        SwitchMaterial switchCallerId = findViewById(R.id.switchCallerId);
        if (switchCallerId != null) {
            switchCallerId.setChecked(settingsPrefs.getBoolean("show_caller_id", true));
            switchCallerId.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("show_caller_id", isChecked).apply());
        }

        SwitchMaterial switchBlockUnknown = findViewById(R.id.switchBlockUnknown);
        if (switchBlockUnknown != null) {
            switchBlockUnknown.setChecked(settingsPrefs.getBoolean("block_unknown_calls", false));
            switchBlockUnknown.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("block_unknown_calls", isChecked).apply());
        }
    }
}
