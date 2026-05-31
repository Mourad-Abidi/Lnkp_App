package com.linkup.app.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.linkup.app.activities.LoginActivity;
import com.linkup.app.database.AppDatabase;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.AuthResponse;
import com.linkup.app.network.SupabaseClient;
import com.linkup.app.network.SupabaseConfig;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Manages account sessions and ensures absolute data isolation between users.
 * Acts as the single source of truth for authentication state.
 */
public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TOKEN = "supabase_token";
    private static final String KEY_REFRESH_TOKEN = "supabase_refresh_token";
    private static final String KEY_EXPIRES_AT = "supabase_token_expires_at";

    /**
     * Wipes all local data associated with the current session.
     * Call this before Login, after Logout, and before Registration.
     */
    public static void wipeSession(Context context) {
        Context appContext = context.getApplicationContext();
        Log.d(TAG, "Initiating full session wipe for account isolation...");

        // 1. Clear In-Memory Singletons
        SharedDataManager.getInstance().clearData();

        // 2. Clear Local SQLite Database (Messages, etc.)
        new Thread(() -> {
            try {
                AppDatabase.getInstance(appContext).clearAllTables();
                Log.d(TAG, "Local database wiped.");
            } catch (Exception e) {
                Log.e(TAG, "Error wiping database", e);
            }
        }).start();

        // 3. Clear All SharedPreferences Files
        String[] prefFiles = {
            "AppPrefs", 
            "UsagePrefs", 
            "LinkUpUsage", 
            "Settings", 
            "SecuritySettings", 
            "AISettings", 
            "GhostInbox", 
            "PersonalNotes"
        };

        for (String fileName : prefFiles) {
            SharedPreferences prefs = appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }
        
        Log.d(TAG, "All SharedPreferences cleared. Isolation guaranteed.");
    }

    public static String getAccessToken(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public static String getRefreshToken(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public static String getCurrentUserId(Context context) {
        if (context == null) return "unknown";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_ID, "unknown");
    }

    public static boolean isLoggedIn(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static boolean isTokenExpired(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        // Buffer of 60 seconds
        return System.currentTimeMillis() > (expiresAt - 60000);
    }

    /**
     * Refreshes the session synchronously using a clean OkHttpClient to avoid circular dependencies.
     */
    public static synchronized String refreshSessionSync(Context context) {
        String refreshToken = getRefreshToken(context);
        if (refreshToken == null) {
            Log.e(TAG, "No refresh token available");
            return null;
        }

        Log.d(TAG, "Attempting to refresh token...");
        
        // Use a dedicated OkHttpClient for refresh to avoid interceptor recursion
        OkHttpClient client = new OkHttpClient();
        
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("refresh_token", refreshToken);
        String json = new Gson().toJson(bodyMap);
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SupabaseConfig.URL + "/auth/v1/token?grant_type=refresh_token")
                .post(body)
                .header("apikey", SupabaseConfig.API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseData = response.body().string();
                AuthResponse auth = new Gson().fromJson(responseData, AuthResponse.class);
                saveSession(context, auth);
                Log.d(TAG, "Token refreshed successfully");
                return auth.getAccessToken();
            } else {
                Log.e(TAG, "Failed to refresh token: " + response.code());
                // Only logout if it's a clear 400 error (token invalid/revoked)
                if (response.code() == 400 || response.code() == 401) {
                    logout(context);
                }
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during token refresh", e);
            return null;
        }
    }

    public static void saveSession(Context context, AuthResponse auth) {
        if (context == null || auth == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        
        if (auth.getAccessToken() != null) {
            editor.putString(KEY_TOKEN, auth.getAccessToken());
        }
        if (auth.getRefreshToken() != null) {
            editor.putString(KEY_REFRESH_TOKEN, auth.getRefreshToken());
        }
        
        if (auth.getExpiresIn() > 0) {
            long expiresAt = System.currentTimeMillis() + (auth.getExpiresIn() * 1000);
            editor.putLong(KEY_EXPIRES_AT, expiresAt);
        }
        
        if (auth.getUser() != null) {
            editor.putString(KEY_USER_ID, auth.getUser().getUserId());
        }
        
        editor.apply();
        // Force reset client to use the new token
        SupabaseClient.resetClient();
    }

    public static void logout(Context context) {
        if (context == null) return;
        wipeSession(context);
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
