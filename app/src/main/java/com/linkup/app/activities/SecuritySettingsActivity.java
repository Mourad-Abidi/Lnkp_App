package com.linkup.app.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.linkup.app.R;
import java.util.concurrent.Executor;

public class SecuritySettingsActivity extends BaseActivity {

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private SwitchMaterial switchFinger, switchFace, switchDeviceCode, switchPin;
    private String pendingBiometricType = ""; 
    private Runnable onAuthSuccessAction;
    private Runnable onAuthCancelAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_security);
        
        initToolbar();
        setupBiometricEngine();
        setupPasswordSection();
        setupPinSection();
        setupBiometricsSection();
        setupAutoLogoutSection();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Security Settings");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupBiometricEngine() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (onAuthCancelAction != null) onAuthCancelAction.run();
                else resetSwitches();
                
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(SecuritySettingsActivity.this, errString, Toast.LENGTH_SHORT).show();
                }
                clearAuthCallbacks();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                handleAuthSuccess();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Identity Verification")
                .setSubtitle("Confirm your identity to proceed")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    private void clearAuthCallbacks() {
        pendingBiometricType = "";
        onAuthSuccessAction = null;
        onAuthCancelAction = null;
    }

    private void handleAuthSuccess() {
        if ("fingerprint".equals(pendingBiometricType)) {
            settingsPrefs.edit().putBoolean("security_fingerprint", true).apply();
            Toast.makeText(this, "Fingerprint protection enabled", Toast.LENGTH_SHORT).show();
        } else if ("face".equals(pendingBiometricType)) {
            settingsPrefs.edit().putBoolean("security_face", true).apply();
            Toast.makeText(this, "Face ID protection enabled", Toast.LENGTH_SHORT).show();
        } else if ("device_code".equals(pendingBiometricType)) {
            settingsPrefs.edit().putBoolean("security_device_code", true).apply();
            Toast.makeText(this, "Device lock protection enabled", Toast.LENGTH_SHORT).show();
        } else if ("generic_verify".equals(pendingBiometricType) && onAuthSuccessAction != null) {
            onAuthSuccessAction.run();
        }
        clearAuthCallbacks();
    }

    private void resetSwitches() {
        if ("fingerprint".equals(pendingBiometricType) && switchFinger != null) switchFinger.setChecked(false);
        else if ("face".equals(pendingBiometricType) && switchFace != null) switchFace.setChecked(false);
        else if ("device_code".equals(pendingBiometricType) && switchDeviceCode != null) switchDeviceCode.setChecked(false);
    }

    private void setupPasswordSection() {
        TextInputEditText etCurrent = findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = findViewById(R.id.etConfirmPassword);
        TextInputLayout tilCurrent = findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNew = findViewById(R.id.tilNewPassword);

        if (etNew != null && tilNew != null) {
            etNew.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updatePasswordStrength(s.toString(), tilNew);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        findViewById(R.id.btnSavePassword).setOnClickListener(v -> {
            String current = etCurrent.getText().toString();
            String p1 = etNew.getText().toString();
            String p2 = etConfirm.getText().toString();
            String savedPassword = settingsPrefs.getString("security_password", "");

            if (!savedPassword.isEmpty() && !savedPassword.equals(current)) {
                if (tilCurrent != null) tilCurrent.setError("Incorrect current password");
                return;
            }
            if (tilCurrent != null) tilCurrent.setError(null);

            if (p1.length() < 6) {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p1.equals(p2)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyIdentity(() -> {
                settingsPrefs.edit().putString("security_password", p1).apply();
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                etCurrent.setText(""); etNew.setText(""); etConfirm.setText("");
            });
        });
    }

    private void updatePasswordStrength(String password, TextInputLayout til) {
        if (password.length() < 6) {
            til.setHelperText("Too short (min 6)");
            til.setHelperTextColor(ContextCompat.getColorStateList(this, android.R.color.holo_red_light));
        } else if (password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")) {
            til.setHelperText("Strong password");
            til.setHelperTextColor(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
        } else {
            til.setHelperText("Medium strength");
            til.setHelperTextColor(ContextCompat.getColorStateList(this, android.R.color.holo_orange_light));
        }
    }

    private void setupPinSection() {
        switchPin = findViewById(R.id.switchPinLock);
        TextInputEditText etPin = findViewById(R.id.etAppPin);
        View btnUpdatePin = findViewById(R.id.btnUpdatePin);

        if (switchPin != null) {
            switchPin.setChecked(settingsPrefs.getBoolean("security_pin_enabled", false));
            switchPin.setOnCheckedChangeListener((b, isChecked) -> {
                if (isChecked && settingsPrefs.getString("security_pin_value", "").isEmpty()) {
                    Toast.makeText(this, "Please set a PIN first", Toast.LENGTH_SHORT).show();
                    switchPin.setChecked(false);
                } else if (!isChecked && settingsPrefs.getBoolean("security_pin_enabled", false)) {
                    verifyIdentity(() -> {
                        settingsPrefs.edit().putBoolean("security_pin_enabled", false).apply();
                    }, () -> switchPin.setChecked(true));
                } else {
                    settingsPrefs.edit().putBoolean("security_pin_enabled", isChecked).apply();
                }
            });
        }

        if (btnUpdatePin != null) {
            btnUpdatePin.setOnClickListener(v -> {
                String pin = etPin.getText().toString();
                if (pin.length() >= 4) {
                    verifyIdentity(() -> {
                        settingsPrefs.edit().putString("security_pin_value", pin).apply();
                        settingsPrefs.edit().putBoolean("security_pin_enabled", true).apply();
                        if (switchPin != null) switchPin.setChecked(true);
                        etPin.setText("");
                        Toast.makeText(this, "App PIN updated", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(this, "Enter at least 4 digits", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void verifyIdentity(Runnable onSuccess) { verifyIdentity(onSuccess, null); }

    private void verifyIdentity(Runnable onSuccess, Runnable onCancel) {
        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            pendingBiometricType = "generic_verify";
            onAuthSuccessAction = onSuccess;
            onAuthCancelAction = onCancel;
            biometricPrompt.authenticate(promptInfo);
        } else {
            onSuccess.run();
        }
    }

    private void setupBiometricsSection() {
        switchFinger = findViewById(R.id.switchFingerprint);
        switchFace = findViewById(R.id.switchFaceUnlock);
        switchDeviceCode = findViewById(R.id.switchDeviceCode);

        BiometricManager bm = BiometricManager.from(this);
        boolean bioAvail = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
        boolean devAvail = bm.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;

        configureBiometricSwitch(switchFinger, "security_fingerprint", "fingerprint", bioAvail);
        configureBiometricSwitch(switchFace, "security_face", "face", bioAvail);
        configureBiometricSwitch(switchDeviceCode, "security_device_code", "device_code", devAvail);
    }

    private void configureBiometricSwitch(SwitchMaterial sw, String prefKey, String type, boolean available) {
        if (sw == null) return;
        sw.setChecked(settingsPrefs.getBoolean(prefKey, false));
        if (!available) { sw.setEnabled(false); sw.setAlpha(0.5f); return; }

        sw.setOnCheckedChangeListener((b, isChecked) -> {
            boolean current = settingsPrefs.getBoolean(prefKey, false);
            if (isChecked && !current) {
                pendingBiometricType = type;
                biometricPrompt.authenticate(promptInfo);
            } else if (!isChecked && current) {
                verifyIdentity(() -> {
                    settingsPrefs.edit().putBoolean(prefKey, false).apply();
                }, () -> sw.setChecked(true));
            }
        });
    }

    private void setupAutoLogoutSection() {
        Spinner spinnerTimer = findViewById(R.id.spinnerLogoutTimer);
        if (spinnerTimer != null) {
            String[] times = {"Immediately", "1 Minute", "5 Minutes", "1 Hour", "Never"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, times);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTimer.setAdapter(adapter);
            spinnerTimer.setSelection(settingsPrefs.getInt("security_logout_timer", 4));
            spinnerTimer.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    settingsPrefs.edit().putInt("security_logout_timer", pos).apply();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }
    }
}
