package com.linkup.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;

public class ChatSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_chats);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupChatBehavior();
        setupMessageManagement();
        setupHistoryActions();
    }

    private void setupChatBehavior() {
        SwitchMaterial switchEnterIsSend = findViewById(R.id.switchEnterIsSend);
        if (switchEnterIsSend != null) {
            switchEnterIsSend.setChecked(settingsPrefs.getBoolean("enter_is_send", false));
            switchEnterIsSend.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsPrefs.edit().putBoolean("enter_is_send", isChecked).apply();
            });
        }

        SwitchMaterial switchSaveToGallery = findViewById(R.id.switchSaveToGallery);
        if (switchSaveToGallery != null) {
            switchSaveToGallery.setChecked(settingsPrefs.getBoolean("save_to_gallery", true));
            switchSaveToGallery.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsPrefs.edit().putBoolean("save_to_gallery", isChecked).apply();
            });
        }
    }

    private void setupMessageManagement() {
        Spinner spinnerAutoDelete = findViewById(R.id.spinnerAutoDelete);
        if (spinnerAutoDelete != null) {
            String[] options = {"Off", "24 Hours", "7 Days", "30 Days"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAutoDelete.setAdapter(adapter);
            
            int savedPos = settingsPrefs.getInt("auto_delete_pos", 0);
            spinnerAutoDelete.setSelection(savedPos);
            
            spinnerAutoDelete.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    settingsPrefs.edit().putInt("auto_delete_pos", position).apply();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        SwitchMaterial switchAutoBackup = findViewById(R.id.switchAutoBackup);
        if (switchAutoBackup != null) {
            switchAutoBackup.setChecked(settingsPrefs.getBoolean("auto_backup", false));
            switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsPrefs.edit().putBoolean("auto_backup", isChecked).apply();
            });
        }

        View btnBackupNow = findViewById(R.id.btnBackupNow);
        if (btnBackupNow != null) {
            btnBackupNow.setOnClickListener(v -> {
                Toast.makeText(this, "Backing up chats...", Toast.LENGTH_SHORT).show();
                new android.os.Handler().postDelayed(() -> {
                    Toast.makeText(this, "Backup completed successfully", Toast.LENGTH_SHORT).show();
                }, 2000);
            });
        }
    }

    private void setupHistoryActions() {
        findViewById(R.id.tvArchiveChats).setOnClickListener(v -> showConfirmDialog("Archive all chats?", "This will move all chats to archive."));
        findViewById(R.id.tvClearAllChats).setOnClickListener(v -> showConfirmDialog("Clear all chats?", "This will delete all messages in all chats. This cannot be undone."));
        findViewById(R.id.tvDeleteAllChats).setOnClickListener(v -> showConfirmDialog("Delete all chats?", "This will delete all chats and their contents. This cannot be undone."));
    }

    private void showConfirmDialog(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Proceed", (dialog, which) -> {
                Toast.makeText(this, "Action performed successfully", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
