package com.linkup.app.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.annotation.NonNull;
import com.linkup.app.database.FirebaseDatabaseManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenceManager implements DefaultLifecycleObserver {
    private static final String TAG = "PresenceManager";
    private static PresenceManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isAppInForeground = false;
    private static final long HEARTBEAT_INTERVAL = 25000; // 25 seconds

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAppInForeground) {
                updatePresence(true);
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }
    };

    public static synchronized PresenceManager getInstance() {
        if (instance == null) instance = new PresenceManager();
        return instance;
    }

    public void init() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        isAppInForeground = true;
        updatePresence(true);
        handler.removeCallbacks(heartbeatRunnable);
        handler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppInForeground = false;
        handler.removeCallbacks(heartbeatRunnable);
        updatePresence(false);
    }

    private void updatePresence(boolean isOnline) {
        String userId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        if (userId == null || "unknown".equals(userId)) return;

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        Map<String, Object> data = new HashMap<>();
        data.put("is_online", isOnline);
        data.put("last_seen", System.currentTimeMillis());
        data.put("last_heartbeat_at", System.currentTimeMillis());

        api.updatePresence("eq." + userId, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Presence updated: online=" + isOnline);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to update presence", t);
            }
        });
    }
}
