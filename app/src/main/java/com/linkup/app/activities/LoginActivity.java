package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.linkup.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.linkup.app.core.SessionManager;
import com.linkup.app.network.SupabaseClient;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.AuthRequest;
import com.linkup.app.network.AuthResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.concurrent.Executor;

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private SharedPreferences securityPrefs;
    private SharedPreferences appPrefs;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        securityPrefs = getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE);

        if (appPrefs.getBoolean("is_logged_in", false)) {
            checkBiometricLogin();
        } else {
            initLoginUI();
        }
    }

    private void checkBiometricLogin() {
        boolean fingerprintEnabled = securityPrefs.getBoolean("security_fingerprint", false);
        boolean faceEnabled = securityPrefs.getBoolean("security_face", false);
        
        if (fingerprintEnabled || faceEnabled) {
            setupBiometricEngine(true);
            biometricPrompt.authenticate(promptInfo);
        } else {
            proceedToMain();
        }
    }

    private void setupBiometricEngine(boolean isAutoLogin) {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (isAutoLogin) initLoginUI();
                else Toast.makeText(LoginActivity.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                proceedToMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Identity Verification")
                .setSubtitle("Authenticate to access your account")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    private void proceedToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void initLoginUI() {
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);

        findViewById(R.id.loginButton).setOnClickListener(v -> performLogin());
        findViewById(R.id.registerButton).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        
        findViewById(R.id.guestButton).setOnClickListener(v -> {
            // Isolation: Wipe any residual data before starting a guest session
            SessionManager.wipeSession(this);
            appPrefs.edit().putBoolean("is_logged_in", true).putBoolean("is_guest", true).apply();
            proceedToMain();
        });

        startEntranceAnimations(findViewById(R.id.logoCard), findViewById(R.id.loginCard));
    }

    private void startEntranceAnimations(View... views) {
        long delay = 0;
        for (View view : views) {
            if (view != null) {
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
                anim.setStartOffset(delay);
                view.startAnimation(anim);
                delay += 150;
            }
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter all credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Connecting to cloud...", Toast.LENGTH_SHORT).show();
        
        AuthRequest loginRequest = new AuthRequest(email, password);
        ApiService apiService = SupabaseClient.getClient().create(ApiService.class);

        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Isolation: Wipe all local data from previous accounts before saving new credentials
                    SessionManager.wipeSession(LoginActivity.this);

                    AuthResponse auth = response.body();
                    SharedPreferences.Editor editor = appPrefs.edit();
                    editor.putBoolean("is_logged_in", true);
                    editor.putString("supabase_token", auth.getAccessToken());
                    if (auth.getUser() != null) {
                        editor.putString("user_id", auth.getUser().getUserId());
                        editor.putString("user_full_name", auth.getUser().getUsername());
                        editor.putString("user_email", auth.getUser().getEmail());
                    }
                    editor.putBoolean("needs_initial_sync", true);
                    editor.apply();
                    proceedToMain();
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleApiError(Response<AuthResponse> response) {
        String errorMsg = "Login Failed";
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                AuthResponse errorRes = new Gson().fromJson(errorJson, AuthResponse.class);
                if (errorRes != null) errorMsg = errorRes.getErrorMessage();
            }
        } catch (Exception ignored) {}
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }
}
