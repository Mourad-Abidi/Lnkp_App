package com.linkup.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.palette.graphics.Palette;
import com.linkup.app.R;
import com.linkup.app.core.NotificationHelper;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.security.SecurityUtils;

public abstract class BaseActivity extends AppCompatActivity {

    protected SharedPreferences settingsPrefs;
    protected SharedPreferences usagePrefs;
    protected SharedPreferences appPrefs;
    protected SharedPreferences securityPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        settingsPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        
        // Make Dark Mode the default if no theme is set yet
        if (!settingsPrefs.contains("app_theme")) {
            settingsPrefs.edit().putInt("app_theme", AppCompatDelegate.MODE_NIGHT_YES).apply();
        }
        
        int mode = settingsPrefs.getInt("app_theme", AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(mode);

        super.onCreate(savedInstanceState);
        
        usagePrefs = getSharedPreferences("UsagePrefs", Context.MODE_PRIVATE);
        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        securityPrefs = getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE);

        // Initialize Supabase Manager (Maintained for compatibility)
        FirebaseDatabaseManager.init(this);
        
        // Create Notification Channel
        NotificationHelper.createNotificationChannel(this);

        // Initialize Decentralized Identity
        SecurityUtils.generateIdentityKeyPair();

        applySecurityPolicies();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAppBackground();
        applySecurityPolicies();
    }

    private void applySecurityPolicies() {
        // Enforce Anti-Screenshot globally if enabled
        boolean antiScreenshot = securityPrefs.getBoolean("anti_screenshot", false);
        if (antiScreenshot) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyAppBackground();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyAppBackground();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyAppBackground();
    }

    protected void applyAppBackground() {
        int mode = settingsPrefs.getInt("app_theme", AppCompatDelegate.MODE_NIGHT_YES);
        boolean isDarkMode = false;
        
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            isDarkMode = true;
        } else if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        }

        if (isDarkMode) {
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            applyDynamicColors(Color.BLACK, Color.BLACK, Color.GRAY, Color.BLACK);
        } else {
            String bgName = settingsPrefs.getString("app_background_name", "my_background_1");
            int backgroundResId = getResources().getIdentifier(bgName, "drawable", getPackageName());
            
            if (backgroundResId == 0) {
                backgroundResId = R.drawable.my_background_1;
            }
            
            try {
                getWindow().getDecorView().setBackgroundResource(backgroundResId);
                extractColorsAndApply(backgroundResId);
            } catch (Exception e) {
                getWindow().getDecorView().setBackgroundResource(R.drawable.my_background_1);
            }
        }
        
        ViewGroup content = findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            View rootView = content.getChildAt(0);
            rootView.setBackground(null);
        }
    }

    private void extractColorsAndApply(int resId) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            if (bitmap != null) {
                Palette.from(bitmap).generate(palette -> {
                    if (palette != null) {
                        int primaryColor = palette.getVibrantColor(Color.BLUE);
                        int darkColor = palette.getDarkVibrantColor(Color.BLACK);
                        int lightColor = palette.getLightVibrantColor(Color.WHITE);
                        int mutedColor = palette.getMutedColor(primaryColor);

                        applyDynamicColors(primaryColor, darkColor, lightColor, mutedColor);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyDynamicColors(int primary, int dark, int light, int muted) {
        Window window = getWindow();
        window.setStatusBarColor(dark);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(Color.argb(180, Color.red(dark), Color.green(dark), Color.blue(dark)));
            toolbar.setTitleTextColor(Color.WHITE);
        }
    }

    protected boolean isStealthModeActive() {
        return settingsPrefs != null && settingsPrefs.getBoolean("stealth_mode_ui_active", false);
    }

    protected void checkStealthAndRun(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    protected boolean isGuest() {
        return appPrefs != null && appPrefs.getBoolean("is_guest", false);
    }
}
