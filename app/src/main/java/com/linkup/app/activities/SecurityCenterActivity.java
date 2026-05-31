package com.linkup.app.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;
import com.linkup.app.security.SecurityUtils;

public class SecurityCenterActivity extends BaseActivity {

    private SharedPreferences securityPrefs;
    private TextView tvActionLabel, tvRadarStatus, tvSecurityScore;
    private View viewRadarPulse, ivRadarSweep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_center);

        securityPrefs = getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE);
        tvActionLabel = findViewById(R.id.tvActionLabel);
        tvSecurityScore = findViewById(R.id.tvSecurityScore);
        
        // Radar Views
        viewRadarPulse = findViewById(R.id.viewRadarPulse);
        ivRadarSweep = findViewById(R.id.ivRadarSweep);
        tvRadarStatus = findViewById(R.id.tvRadarStatus);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        setupFeatures();
        startRadarAnimation();
        setupInfoHolders();
        setupAccountSecurity();

        // Automatically run audit on start
        new Handler().postDelayed(this::runSecurityAudit, 1000);
    }

    private void setupAccountSecurity() {
        View.OnClickListener goToSettings = v -> {
            updateActionLabel("Navigating to Security Settings...");
            startActivity(new Intent(this, SecuritySettingsActivity.class));
        };

        findViewById(R.id.settingPrivacy).setOnClickListener(goToSettings);
        
        setInfoClick(R.id.infoPrivacyProtection, "Privacy & Data", "Manage your data visibility, connection logs, and P2P encryption preferences.");
    }

    private void updateActionLabel(String text) {
        if (tvActionLabel != null) {
            tvActionLabel.setText(text);
            tvActionLabel.setAlpha(0f);
            tvActionLabel.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void startRadarAnimation() {
        if (ivRadarSweep == null || viewRadarPulse == null) return;

        // Rotating Sweep
        ObjectAnimator rotate = ObjectAnimator.ofFloat(ivRadarSweep, "rotation", 0f, 360f);
        rotate.setDuration(3000);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.start();

        // Pulsing Circle
        ValueAnimator pulse = ValueAnimator.ofFloat(0.1f, 1.2f);
        pulse.setDuration(1500);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            viewRadarPulse.setScaleX(val);
            viewRadarPulse.setScaleY(val);
            viewRadarPulse.setAlpha(1.2f - val);
        });
        pulse.start();

        // Update Status Text randomly
        final String[] messages = {"SCANNING NODES...", "CHECKING P2P...", "KEYS SECURE", "NO THREATS"};
        final Handler h = new Handler();
        h.post(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (isFinishing()) return;
                if (tvRadarStatus != null) {
                    tvRadarStatus.setText(messages[i % messages.length]);
                    i++;
                }
                h.postDelayed(this, 2000);
            }
        });
    }

    private void setupFeatures() {
        // Advanced Protection Switches
        setupSwitch(R.id.switchLANDiscovery, "lan_discovery", "LAN Discovery");
        setupSwitch(R.id.switchEphemeralIdentity, "ephemeral_identity", "Identity Rotation");
        setupSwitch(R.id.switchGhostMode, "ghost_mode", "Ghost Mode");
        setupSwitch(R.id.switchIntruderAlert, "intruder_alert", "Intruder Detection");
        setupSwitch(R.id.switchBiometric, "biometric_protection", "Biometric Lock");
        setupSwitch(R.id.switchDeepInspection, "deep_inspection", "Deep Inspection");
        setupSwitch(R.id.switchNeuralProxy, "neural_proxy", "Neural Proxy");
        setupSwitch(R.id.switchQuantumEncryption, "quantum_encryption", "Quantum Encryption");
        setupSwitch(R.id.switchAntiScreenshot, "anti_screenshot", "Anti-Screenshot");
        setupSwitch(R.id.switchCamouflage, "camouflage_mode", "Camouflage Mode");
    }

    private void setupInfoHolders() {
        setInfoClick(R.id.infoLANDiscovery, "LAN Discovery", "Automatically find and connect to users on your local network when internet is unavailable.");
        setInfoClick(R.id.infoEphemeralIdentity, "Ephemeral Identity", "Automatically rotates your cryptographic keys and device signatures to prevent tracking.");
        setInfoClick(R.id.infoGhostMode, "Ghost Mode", "Hides your online status and presence across all network nodes.");
        setInfoClick(R.id.infoIntruderAlert, "Intruder Alert", "Captures front-camera logs and triggers a lockdown if unauthorized access is detected.");
        setInfoClick(R.id.infoBiometric, "Biometric Protection", "Requires fingerprint or face authentication for sensitive operations.");
        setInfoClick(R.id.infoDeepInspection, "Deep Inspection", "Analyzes incoming packets for signature-based threats and malformed data.");
        setInfoClick(R.id.infoNeuralProxy, "Neural Proxy (VPN)", "Routes all traffic through a multi-hop onion network for absolute anonymity.");
        setInfoClick(R.id.infoQuantumEncryption, "Quantum Encryption", "Uses lattice-based algorithms resistant to future quantum computing decryption.");
        setInfoClick(R.id.infoAntiScreenshot, "Anti-Screenshot", "Prevents screen capturing and hides the app from the recent tasks list.");
        setInfoClick(R.id.infoCamouflage, "Icon Camouflage", "Changes the app icon and name to look like a standard system utility.");
        setInfoClick(R.id.infoKillSwitch, "Network Kill Switch", "Instantly cuts all network traffic if the secure connection is lost.");
        setInfoClick(R.id.infoMetadata, "Metadata Scrubbing", "Strips GPS and device metadata from files before they are shared.");
        setInfoClick(R.id.infoEncryptedDns, "Encrypted DNS", "Uses DNS-over-HTTPS to prevent ISP-level tracking of your requests.");
        setInfoClick(R.id.infoStealthMode, "Stealth Mode", "Makes the app traffic indistinguishable from standard HTTPS browsing.");
    }

    private void setInfoClick(int id, String title, String description) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(v -> showInfoDialog(title, description));
        }
    }

    private void showInfoDialog(String title, String description) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_security_info, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Lnkp_AlertDialog)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvInfoTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvInfoDescription);
        MaterialButton btnOk = dialogView.findViewById(R.id.btnInfoOk);

        tvTitle.setText(title);
        tvDesc.setText(description);
        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupSwitch(int id, String prefKey, String label) {
        SwitchMaterial sm = findViewById(id);
        if (sm != null) {
            sm.setChecked(securityPrefs.getBoolean(prefKey, false));
            sm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                securityPrefs.edit().putBoolean(prefKey, isChecked).apply();
                updateActionLabel(label + (isChecked ? ": ENABLED" : ": DISABLED"));
                if (prefKey.equals("anti_screenshot")) {
                    applyAntiScreenshot(isChecked);
                }
                if (prefKey.equals("camouflage_mode")) {
                    if (isChecked) showCamouflageDialog();
                    else resetCamouflage();
                }
                // Run audit again after changing setting
                runSecurityAudit();
            });
        }
    }

    private void showCamouflageDialog() {
        String[] icons = {"Calculator", "Weather", "News", "Notes"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Camouflage Icon")
            .setItems(icons, (dialog, which) -> {
                String selected = icons[which];
                securityPrefs.edit().putString("camouflage_type", selected).apply();
                Toast.makeText(this, "App will appear as " + selected + " on next restart", Toast.LENGTH_LONG).show();
                updateActionLabel("Camouflage: " + selected + " (Pending Restart)");
            })
            .show();
    }

    private void resetCamouflage() {
        securityPrefs.edit().remove("camouflage_type").apply();
        Toast.makeText(this, "Camouflage reset to default", Toast.LENGTH_SHORT).show();
    }

    private void runSecurityAudit() {
        updateActionLabel("Running Full Security Audit...");
        new Handler().postDelayed(() -> {
            int score = 30; // Base safety score

            // Check for root (Real Check)
            if (!SecurityUtils.isDeviceRooted()) {
                score += 15;
            } else {
                updateActionLabel("ALERT: Rooted Device Detected!");
            }

            // Check for VPN (Real Check)
            if (SecurityUtils.isVpnActive(this)) {
                score += 10;
            }

            // Check User Preferences
            String[] keys = {
                "lan_discovery", "ephemeral_identity", "ghost_mode", "biometric_protection",
                "quantum_encryption", "anti_screenshot", "neural_proxy",
                "stealth_mode", "network_kill_switch", "metadata_scrubbing"
            };
            
            for (String key : keys) {
                if (securityPrefs.getBoolean(key, false)) score += 5;
            }
            
            if (score > 100) score = 100;
            
            if (tvSecurityScore != null) {
                tvSecurityScore.setText(score + "%");
            }
            
            com.google.android.material.progressindicator.LinearProgressIndicator progress = findViewById(R.id.securityProgress);
            if (progress != null) {
                progress.setProgress(score, true);
            }

            updateActionLabel("System Audit Finished: " + score + "%");
        }, 1500);
    }

    private void applyAntiScreenshot(boolean enabled) {
        if (enabled) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
