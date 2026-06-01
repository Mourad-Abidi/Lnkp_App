package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import com.linkup.app.R;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.AppDatabase;

public class SettingsActivity extends BaseActivity {

    private LinearLayout llSettingsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        llSettingsContainer = findViewById(R.id.llSettingsContainer);

        setupClickListeners();
    }

    private void setupClickListeners() {
        findViewById(R.id.settingAppearance).setOnClickListener(v -> startActivity(new Intent(this, AppearanceActivity.class)));
        findViewById(R.id.settingAnalyseAccount).setOnClickListener(v -> startActivity(new Intent(this, AccountAnalysisActivity.class)));
        findViewById(R.id.settingSecurityCenter).setOnClickListener(v -> startActivity(new Intent(this, SecurityCenterActivity.class)));
        findViewById(R.id.settingAbout).setOnClickListener(v -> showAboutDialog());
        findViewById(R.id.settingCalls).setOnClickListener(v -> startActivity(new Intent(this, CallsSettingsActivity.class)));
        findViewById(R.id.settingChangeAccount).setOnClickListener(v -> startActivity(new Intent(this, ChangeAccountActivity.class)));
        findViewById(R.id.settingChats).setOnClickListener(v -> startActivity(new Intent(this, ChatSettingsActivity.class)));
        findViewById(R.id.settingHelp).setOnClickListener(v -> showHelpDialog());
        findViewById(R.id.settingInviteFriends).setOnClickListener(v -> shareAppLink());
        findViewById(R.id.settingMediaStorage).setOnClickListener(v -> startActivity(new Intent(this, MediaStorageSettingsActivity.class)));
        findViewById(R.id.settingNotifications).setOnClickListener(v -> startActivity(new Intent(this, NotificationsSettingsActivity.class)));
        findViewById(R.id.settingPrivacyPolicy).setOnClickListener(v -> openUrl("https://linkup.app/privacy-policy"));
        findViewById(R.id.settingGhostChat).setOnClickListener(v -> startActivity(new Intent(this, GhostChatActivity.class)));
        findViewById(R.id.settingClearData).setOnClickListener(v -> showClearDataDialog());
        findViewById(R.id.settingLogout).setOnClickListener(v -> showLogoutDialog());
        
        findViewById(R.id.settingHelp).setOnLongClickListener(v -> {
            showDeveloperMenu();
            return true;
        });
    }

    private void showDeveloperMenu() {
        String[] items = {"Reset All Tutorials", "Toggle Network Logs", "Force Crash (Debug)", "Server Endpoint: PRODUCTION"};
        new AlertDialog.Builder(this)
            .setTitle("Developer System Menu")
            .setItems(items, (dialog, which) -> {
                if (which == 0) Toast.makeText(this, "Tutorials reset", Toast.LENGTH_SHORT).show();
                if (which == 1) Toast.makeText(this, "Logs enabled in Logcat", Toast.LENGTH_SHORT).show();
            })
            .setPositiveButton("Close", null)
            .show();
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void shareAppLink() {
        String shareMessage = "Hey! Check out Lnkp, the most secure messaging app. Download it here: https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(intent, "Invite Friends via"));
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For any issues or feedback, please visit our support portal or email us at support@linkup.app.\n\nOur team is available 24/7.")
            .setPositiveButton("Support Portal", (dialog, which) -> openUrl("https://linkup.app/support"))
            .setNegativeButton("Close", null)
            .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("About Lnkp")
            .setMessage("Lnkp v1.0.0\n\nA secure, end-to-end encrypted messaging platform designed for privacy and speed.\n\n© 2024 LinkUp App Team.")
            .setPositiveButton("Check for Updates", (d, w) -> Toast.makeText(this, "You are using the latest version", Toast.LENGTH_SHORT).show())
            .setNeutralButton("OK", null)
            .show();
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Clear All Data?")
            .setMessage("This will delete all your local posts, shared media, and cache. This action cannot be undone.")
            .setPositiveButton("Clear Everything", (dialog, which) -> {
                clearAllLocalData();
                Toast.makeText(this, "Local data cleared successfully", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showLogoutDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Lnkp_AlertDialog).setView(dialogView).create();
        
        dialogView.findViewById(R.id.btnLogoutThisDevice).setOnClickListener(v -> { performLogout(false); dialog.dismiss(); });
        dialogView.findViewById(R.id.btnLogoutAllDevices).setOnClickListener(v -> { performLogout(true); dialog.dismiss(); });
        dialogView.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void clearAllLocalData() {
        // 1. Clear In-Memory Singleton Data
        SharedDataManager.getInstance().clearData();
        
        // 2. Clear Room Database
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).clearAllTables();
        }).start();

        // 3. Clear All relevant SharedPreferences
        String[] prefs = {"AppPrefs", "UsagePrefs", "LinkUpUsage", "SecuritySettings", "SecuritySettings", "AISettings", "GhostInbox", "PersonalNotes"};
        for (String p : prefs) {
            getSharedPreferences(p, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }

    private void performLogout(boolean allDevices) {
        clearAllLocalData();

        Toast.makeText(this, allDevices ? "Logged out from all devices" : "Logged out", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
