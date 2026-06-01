package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class AccountAnalysisActivity extends BaseActivity {

    private LinearLayout llAnalysisLoading, containerTopChats;
    private View llAnalysisContent, statsHeader;
    private TextView tvRecentActivity;
    private TextView tvCurrentDateTime, tvUsageTime, tvTotalTimeSpent, tvTotalIdleTime;
    private TextView tvImagesCount, tvVideosCount, tvFilesCount, tvGifsCount, tvEmojisCount, tvWordsCount;
    private TextView tvWifiUsage, tvMobileUsage, tvKeyRotations, tvGhostMessages, tvIdentityState;
    private SwitchMaterial statsSwitch;
    private MaterialButton btnExploreMedia, btnAnalyzeNetwork, btnDeepScan;
    
    private Handler statsHandler = new Handler();
    private long sessionStartTime;
    private SharedPreferences usagePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_analysis);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        usagePrefs = getSharedPreferences("LinkUpUsage", Context.MODE_PRIVATE);
        sessionStartTime = System.currentTimeMillis();

        // Initialize Views
        llAnalysisLoading = findViewById(R.id.llAnalysisLoading);
        llAnalysisContent = findViewById(R.id.llAnalysisContent);
        containerTopChats = findViewById(R.id.containerTopChats);
        statsHeader = findViewById(R.id.statsHeader);
        statsSwitch = findViewById(R.id.statsSwitch);
        
        btnExploreMedia = findViewById(R.id.btnViewGallery); 
        btnAnalyzeNetwork = findViewById(R.id.btnAnalyzeNetwork);
        btnDeepScan = findViewById(R.id.btnDeepScan);

        tvRecentActivity = findViewById(R.id.tvRecentActivity);
        
        tvCurrentDateTime = findViewById(R.id.tvCurrentDateTime);
        tvUsageTime = findViewById(R.id.tvUsageTime);
        tvTotalTimeSpent = findViewById(R.id.tvTotalTimeSpent);
        tvTotalIdleTime = findViewById(R.id.tvTotalIdleTime);

        tvImagesCount = findViewById(R.id.tvImagesCount);
        tvVideosCount = findViewById(R.id.tvVideosCount);
        tvFilesCount = findViewById(R.id.tvFilesCount);
        tvGifsCount = findViewById(R.id.tvGifsCount);
        tvEmojisCount = findViewById(R.id.tvEmojisCount);
        tvWordsCount = findViewById(R.id.tvWordsCount);

        tvWifiUsage = findViewById(R.id.tvWifiUsage);
        tvMobileUsage = findViewById(R.id.tvMobileUsage);
        tvKeyRotations = findViewById(R.id.tvKeyRotations);
        tvGhostMessages = findViewById(R.id.tvGhostMessages);
        tvIdentityState = findViewById(R.id.tvIdentityState);

        setupListeners();
        startAnalysis();
    }

    private void setupListeners() {
        if (statsSwitch != null) {
            boolean isStatsVisible = usagePrefs.getBoolean("isStatsVisible", true);
            statsSwitch.setChecked(isStatsVisible);
            statsHeader.setVisibility(isStatsVisible ? View.VISIBLE : View.GONE);
            
            statsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                statsHeader.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                usagePrefs.edit().putBoolean("isStatsVisible", isChecked).apply();
            });
        }

        btnExploreMedia.setOnClickListener(v -> {
            startActivity(new Intent(this, MediaGalleryActivity.class));
        });

        // Network Analysis placeholder
        btnAnalyzeNetwork.setOnClickListener(v -> {
            Toast.makeText(this, "Analyzing secure network nodes...", Toast.LENGTH_SHORT).show();
        });

        // Deep Scan placeholder
        btnDeepScan.setOnClickListener(v -> {
            Toast.makeText(this, "Scanning core hardware architecture...", Toast.LENGTH_SHORT).show();
        });
    }

    private void startAnalysis() {
        llAnalysisLoading.setVisibility(View.VISIBLE);
        llAnalysisContent.setVisibility(View.GONE);

        new Handler().postDelayed(() -> {
            llAnalysisLoading.setVisibility(View.GONE);
            llAnalysisContent.setVisibility(View.VISIBLE);
            populateData();
            startLiveStats();
        }, 1500); 
    }

    private void populateData() {
        // Fetch values from SharedPreferences, defaulting to zero for new accounts
        tvTotalTimeSpent.setText(usagePrefs.getString("total_time_spent", "0h 0m"));
        tvTotalIdleTime.setText(usagePrefs.getString("total_idle_time", "0h 0m"));

        tvImagesCount.setText(String.valueOf(usagePrefs.getInt("images_count", 0)));
        tvVideosCount.setText(String.valueOf(usagePrefs.getInt("videos_count", 0)));
        tvFilesCount.setText(String.valueOf(usagePrefs.getInt("files_count", 0)));
        tvGifsCount.setText(String.valueOf(usagePrefs.getInt("gifs_count", 0)));
        tvEmojisCount.setText(String.valueOf(usagePrefs.getInt("emojis_count", 0)));
        tvWordsCount.setText(usagePrefs.getString("words_count", "0"));

        tvWifiUsage.setText(usagePrefs.getString("wifi_usage", "0 MB"));
        tvMobileUsage.setText(usagePrefs.getString("mobile_usage", "0 MB"));
        
        // Identity and security states
        tvKeyRotations.setText(String.valueOf(usagePrefs.getInt("key_rotations", 0)));
        tvGhostMessages.setText(String.valueOf(usagePrefs.getInt("ghost_messages_count", 0)));
        tvIdentityState.setText(usagePrefs.getString("identity_state", "Secure"));

        tvRecentActivity.setText(usagePrefs.getString("recent_activity_summary", "No recent activity"));

        containerTopChats.removeAllViews();
        // For new account, we don't add dummy entries unless they exist in data
    }

    private void addTopChatEntry(String name, String type, String level) {
        View view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, containerTopChats, false);
        TextView text1 = view.findViewById(android.R.id.text1);
        TextView text2 = view.findViewById(android.R.id.text2);
        
        text1.setText(String.format("%s (%s)", name, level));
        text1.setTextColor(getResources().getColor(R.color.white));
        text2.setText(type);
        text2.setTextColor(getResources().getColor(R.color.grey_400));
        
        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("user_name", name);
            startActivity(intent);
        });
        
        containerTopChats.addView(view);
    }

    private void startLiveStats() {
        statsHandler.post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                String currentDateTime = new SimpleDateFormat("EEE, MMM dd | HH:mm:ss", Locale.getDefault()).format(new Date(now));
                tvCurrentDateTime.setText(currentDateTime);

                String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
                long todayBase = usagePrefs.getLong(todayKey, 0);
                long totalMillis = todayBase + (now - sessionStartTime);
                long seconds = (totalMillis / 1000) % 60;
                long minutes = (totalMillis / (1000 * 60)) % 60;
                long hours = (totalMillis / (1000 * 60 * 60));

                tvUsageTime.setText(getString(R.string.usage_today, hours, minutes, seconds));

                statsHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        statsHandler.removeCallbacksAndMessages(null);
    }
}
