package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.linkup.app.R;
import com.linkup.app.core.SessionManager;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.AppDatabase;
import com.linkup.app.models.User;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.AuthRequest;
import com.linkup.app.network.AuthResponse;
import com.linkup.app.network.SupabaseClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.fullNameInput);
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        etConfirmPassword = findViewById(R.id.confirmPasswordInput);
        btnRegister = findViewById(R.id.registerButton);

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> performRegistration());
        }
    }

    private void performRegistration() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        Toast.makeText(this, "Establishing secure connection...", Toast.LENGTH_SHORT).show();

        AuthRequest authRequest = new AuthRequest(name, email, password);
        ApiService apiService = SupabaseClient.getClient().create(ApiService.class);
        
        apiService.registerUser(authRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Isolation: Wipe any residual data before setting up the new account session
                    SessionManager.wipeSession(RegisterActivity.this);
                    
                    AuthResponse auth = response.body();
                    String userId = auth.getUser().getUserId();
                    String token = auth.getAccessToken();
                    
                    // Manually push to profiles table as a fallback to ensure searchability
                    saveUserToProfiles(userId, name, email, token);
                } else {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Registration failed: Email already taken.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserToProfiles(String userId, String name, String email, String token) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName(name);
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        user.setAccountStatus("ACTIVE");

        ApiService apiService = SupabaseClient.getClient().create(ApiService.class);
        apiService.saveUserToDatabase(user, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Proceed regardless of profile save success, as trigger might have handled it
                handleSuccessfulRegistration(name, email, userId, token);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleSuccessfulRegistration(name, email, userId, token);
            }
        });
    }

    private void handleSuccessfulRegistration(String name, String email, String userId, String token) {
        appPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_full_name", name)
            .putString("user_email", email)
            .putString("user_id", userId)
            .putString("supabase_token", token)
            .putBoolean("needs_initial_sync", true)
            .apply();

        Toast.makeText(this, "Welcome to Lnkp!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
