package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.linkup.app.R;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.AppDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.linkup.app.models.User;
import com.linkup.app.network.SupabaseClient;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.AuthRequest;
import com.linkup.app.network.AuthResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;

public class VerificationActivity extends BaseActivity {

    private EditText etVerificationCode;
    private TextView tvVerificationMsg;
    private String destination; // Phone number
    private boolean isRegistration;

    private String userName;
    private SharedPreferences appPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        destination = getIntent().getStringExtra("destination");
        isRegistration = getIntent().getBooleanExtra("is_registration", false);

        userName = getIntent().getStringExtra("user_name");

        etVerificationCode = findViewById(R.id.etVerificationCode);
        tvVerificationMsg = findViewById(R.id.tvVerificationMsg);
        MaterialButton btnVerify = findViewById(R.id.btnVerify);
        MaterialButton btnResendCode = findViewById(R.id.btnResendCode);

        if (destination != null) {
            tvVerificationMsg.setText(getString(R.string.enter_code_msg, destination));
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        btnVerify.setOnClickListener(v -> performVerification());

        btnResendCode.setOnClickListener(v -> {
            Toast.makeText(this, R.string.sending_code, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                Toast.makeText(this, R.string.code_sent, Toast.LENGTH_SHORT).show();
            }, 1500);
        });
    }

    private void performVerification() {
        String code = etVerificationCode.getText().toString().trim();

        if (TextUtils.isEmpty(code) || code.length() < 6) {
            Toast.makeText(this, R.string.invalid_code, Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulating verification logic (123456 is the magic code for demo/testing)
        if (code.equals("123456")) {
            Toast.makeText(this, "Identity Verified, connecting to cloud...", Toast.LENGTH_SHORT).show();
            
            ApiService apiService = SupabaseClient.getClient().create(ApiService.class);
            String password = getIntent().getStringExtra("password");

            if (isRegistration) {
                // Using Phone for AuthRequest
                AuthRequest request = new AuthRequest(userName, destination, password);
                
                apiService.registerUser(request).enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse auth = response.body();
                            String userId = (auth.getUser() != null) ? auth.getUser().getUserId() : destination;
                            saveUserToCloudDatabase(userId, userName, destination);
                        } else {
                            handleApiError(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        Toast.makeText(VerificationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Login with Phone
                AuthRequest request = new AuthRequest(destination, password);
                apiService.login(request).enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            handleSuccessfulLogin(response.body().getUser());
                        } else {
                            handleApiError(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        Toast.makeText(VerificationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else {
            Toast.makeText(this, R.string.invalid_code, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToCloudDatabase(String userId, String name, String phone) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(name);
        user.setPhone(phone);
        user.setEmail(null);
        user.setAccountStatus("ACTIVE");
        user.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

        ApiService apiService = SupabaseClient.getClient().create(ApiService.class);
        apiService.saveUserToDatabase(user, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                handleSuccessfulLogin(user);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleSuccessfulLogin(user);
            }
        });
    }

    private void handleApiError(Response<AuthResponse> response) {
        String errorMsg = "Cloud API error";
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                AuthResponse errorRes = new Gson().fromJson(errorJson, AuthResponse.class);
                if (errorRes != null) {
                    errorMsg = errorRes.getErrorMessage();
                }
            }
        } catch (Exception ignored) {}
        Toast.makeText(VerificationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
    }

    private void handleSuccessfulLogin(User user) {
        // Reset all usage counters and preferences for the new session/account if needed
        if (isRegistration) {
            String[] prefsToClear = {
                "LinkUpUsage", "UsagePrefs", "AISettings", "GhostInbox", "PersonalNotes", "SecuritySettings"
            };
            for (String prefName : prefsToClear) {
                getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply();
            }
            SharedDataManager.getInstance().clearData();
            try {
                new Thread(() -> AppDatabase.getInstance(getApplicationContext()).clearAllTables()).start();
                getApplicationContext().deleteDatabase("LinkUpSecure.db");
            } catch (Exception ignored) {}
        }

        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putBoolean("is_logged_in", true);
        
        if (user != null) {
            editor.putString("user_id", user.getUserId());
            editor.putString("user_full_name", user.getUsername());
            editor.putString("user_phone", user.getPhone());
            editor.putString("user_avatar_uri", user.getProfilePhoto());
            editor.putString("user_bio", user.getAccountStatus());
        }
        
        editor.apply();

        Intent intent = new Intent(this, DonationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
