package com.linkup.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.adapters.RecentUpdatesAdapter;
import com.linkup.app.core.LANManager;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.Post;
import com.linkup.app.network.SupabaseRealtimeManager;
import com.bumptech.glide.Glide;
import com.linkup.app.repository.MessageRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity implements LANManager.OnPeerDiscoveryListener, SharedDataManager.OnPostAddedListener {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    
    private ChatAdapter chatAdapter;
    private RecentUpdatesAdapter updatesAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private List<ChatModel> displayChatList = new ArrayList<>();
    private int activeFilter = 0; // 0: All, 1: Groups, 2: Calls

    private TextView tvCurrentDateTime, tvUsageTime;
    private View statsHeader;
    private String todayDateKey;
    private boolean isPrivacyMaskActive = false;
    private View stealthOverlay;
    private ImageView ivFabProfile;

    private final Handler statsHandler = new Handler();
    private long sessionStartTime;
    private long totalUsageTodayBase;

    private static final String PREF_USER_AVATAR = "user_avatar_uri";

    private SupabaseRealtimeManager.MessageListener realtimeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initUI();
        setupRecentUpdates();
        setupChatsList();
        
        startStatsUpdates();
        startGlobalListeners();
        
        requestNotificationPermission();

        // Initial sync logic
        if (appPrefs.getBoolean("needs_initial_sync", false)) {
            // New Architecture: Use Repository for initial sync
            FirebaseDatabaseManager.getInstance().fetchAllUsers(users -> {
                if (users != null) {
                    for (com.linkup.app.models.User user : users) {
                        MessageRepository.getInstance(this).syncHistory(user.getUserId(), null);
                    }
                    appPrefs.edit().putBoolean("needs_initial_sync", false).apply();
                    FirebaseDatabaseManager.getInstance().syncConversations();
                }
            });
        } else {
            FirebaseDatabaseManager.getInstance().syncConversations();
        }
        FirebaseDatabaseManager.getInstance().listenForPosts();

        // STEP 2: Start Real-time Listening
        String currentUserId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        Log.d(TAG, "Starting realtime listener for user: " + currentUserId);
        if (!"unknown".equals(currentUserId)) {
            realtimeListener = message -> {
                Log.d(TAG, "Realtime message received in MainActivity: " + message.getMessageText());
                runOnUiThread(() -> {
                    // Update global state which will trigger refreshChatsList via the groupListener
                    SharedDataManager.getInstance().handleIncomingMessage(message);
                });
            };
            SupabaseRealtimeManager.getInstance().startListening(currentUserId, realtimeListener);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted by user");
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Notification permission denied by user");
                Toast.makeText(this, "Notifications disabled. You might miss new messages.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseDatabaseManager.getInstance().syncPendingMessages();
        String savedAvatar = appPrefs.getString(PREF_USER_AVATAR, usagePrefs.getString(PREF_USER_AVATAR, null));
        if (savedAvatar != null) updateUserAvatar(savedAvatar);
        updateUserCardState();
        
        isPrivacyMaskActive = usagePrefs.getBoolean("privacy_mask_active", false);
        applyStealthUI();
        if (chatAdapter != null) chatAdapter.setMasked(isPrivacyMaskActive);
        
        // Sync conversations from cloud whenever app comes to foreground
        FirebaseDatabaseManager.getInstance().syncConversations();
        FirebaseDatabaseManager.getInstance().listenForPosts();

        refreshRecentUpdates();
        refreshChatsList();
    }

    private void initData() {
        sessionStartTime = System.currentTimeMillis();
        todayDateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        usagePrefs = getSharedPreferences("LinkUpUsage", Context.MODE_PRIVATE);
        
        totalUsageTodayBase = usagePrefs.getLong(todayDateKey, 0);
        isPrivacyMaskActive = usagePrefs.getBoolean("privacy_mask_active", false);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initUI() {
        statsHeader = findViewById(R.id.statsHeader);
        tvCurrentDateTime = findViewById(R.id.tvCurrentDateTime);
        tvUsageTime = findViewById(R.id.tvUsageTime);
        stealthOverlay = findViewById(R.id.stealthOverlay);
        ivFabProfile = findViewById(R.id.ivFabProfile);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                FirebaseDatabaseManager.getInstance().syncConversations(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                });
                FirebaseDatabaseManager.getInstance().listenForPosts();
            });
            swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        }

        if (statsHeader != null) {
            statsHeader.setVisibility(usagePrefs.getBoolean("isStatsVisible", true) ? View.VISIBLE : View.GONE);
            statsHeader.setOnLongClickListener(v -> { showWellbeingSummary(); return true; });
            statsHeader.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, AccountAnalysisActivity.class))));
        }

        TextView tvEnterTime = findViewById(R.id.tvEnterTime);
        if (tvEnterTime != null) {
            String entryTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(sessionStartTime));
            tvEnterTime.setText(getString(R.string.entry_time, entryTime));
        }

        findViewById(R.id.tvLAN).setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, LANChatActivity.class))));
        
        View cardCurrentUser = findViewById(R.id.cardCurrentUser);
        if (cardCurrentUser != null) {
            cardCurrentUser.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, ShareActivity.class))));
        }

        View llCreateNewGroup = findViewById(R.id.llCreateNewGroup);
        if (llCreateNewGroup != null) {
            llCreateNewGroup.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, CreateGroupActivity.class))));
        }

        setupToolbarActions();
        setupFilters();
        setupFloatingInterface();
        applyStealthUI();
        updateUserCardState();
    }

    private void setupRecentUpdates() {
        RecyclerView rvRecentUpdates = findViewById(R.id.rvRecentUpdates);
        if (rvRecentUpdates == null) return;

        rvRecentUpdates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        updatesAdapter = new RecentUpdatesAdapter(new ArrayList<>());
        
        updatesAdapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, SharedContentDetailActivity.class);
            intent.putExtra("post_id", post.postId);
            intent.putExtra("user_id", post.userId);
            intent.putExtra("user_name", post.userName);
            intent.putExtra("content", post.content);
            intent.putExtra("media_path", post.mediaPath);
            intent.putExtra("timestamp", post.getTimestampMillis());
            startActivity(intent);
        });

        rvRecentUpdates.setAdapter(updatesAdapter);

        SharedDataManager.getInstance().addPostListener(this);
        refreshRecentUpdates();
    }

    @Override
    public void onPostAdded(Post post) {
        refreshRecentUpdates();
        runOnUiThread(this::updateUserCardState);
    }

    private void refreshRecentUpdates() {
        runOnUiThread(() -> {
            Set<String> filterNames = new HashSet<>(SharedDataManager.getInstance().getGroupNamesSet());
            String myName = appPrefs.getString("user_full_name", "You");
            filterNames.add(myName.toLowerCase());
            filterNames.add("you");
            filterNames.add("me");

            String myId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
            List<Post> filteredPosts = SharedDataManager.getInstance().getFilteredPosts(filterNames, myId);
            if (updatesAdapter != null) {
                updatesAdapter.updatePosts(filteredPosts);
            }
        });
    }

    private void setupChatsList() {
        RecyclerView rvChats = findViewById(R.id.rvChats);
        if (rvChats == null) return;

        chatAdapter = new ChatAdapter(this, displayChatList, chat -> checkStealthAndRun(() -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("user_name", chat.getUserName());
            intent.putExtra("user_id", chat.getUserId());
            intent.putExtra("is_group", chat.isGroup());
            startActivity(intent);
        }));

        chatAdapter.setLongClickListener(chat -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Conversation")
                    .setMessage("Are you sure you want to delete this conversation with " + chat.getUserName() + "? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        Toast.makeText(this, "Deleting conversation...", Toast.LENGTH_SHORT).show();
                        MessageRepository.getInstance(this).deleteConversation(chat.getUserId(), () -> {
                            SharedDataManager.getInstance().removeGroup(chat.getUserId());
                            refreshChatsList();
                            Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(chatAdapter);

        if (chatAdapter != null) chatAdapter.setMasked(isPrivacyMaskActive);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                chatAdapter.notifyItemChanged(position);
            }
        });
        itemTouchHelper.attachToRecyclerView(rvChats);
    }

    @Override
    public void onPeerFound(String name, String ip) {
        runOnUiThread(() -> {
            ChatModel chat = SharedDataManager.getInstance().getGroup(name);
            if (chat != null) {
                chat.setIpAddress(ip);
                chat.markAsRead();
                refreshChatsList();
                Toast.makeText(this, "LAN bridge ready: " + name, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPeerLost(String name) {
        runOnUiThread(() -> {
            ChatModel chat = SharedDataManager.getInstance().getGroup(name);
            if (chat != null) {
                chat.setIpAddress(null);
                refreshChatsList();
            }
        });
    }

    private void startStatsUpdates() {
        statsHandler.post(new Runnable() {
            @Override
            public void run() {
                long totalToday = totalUsageTodayBase + (System.currentTimeMillis() - sessionStartTime);
                usagePrefs.edit().putLong(todayDateKey, totalToday).apply();

                String timeStr = new SimpleDateFormat("EEE, MMM d, HH:mm", Locale.getDefault()).format(new Date());
                if (tvCurrentDateTime != null) tvCurrentDateTime.setText(timeStr);

                long s = (totalToday / 1000) % 60;
                long m = (totalToday / 60000) % 60;
                long h = (totalToday / 3600000);
                if (tvUsageTime != null) tvUsageTime.setText(getString(R.string.usage_today, h, m, s));

                statsHandler.postDelayed(this, 1000);
            }
        });
    }

    private void showWellbeingSummary() {
        long totalToday = totalUsageTodayBase + (System.currentTimeMillis() - sessionStartTime);
        long m = (totalToday / 60000) % 60;
        long h = (totalToday / 3600000);
        new AlertDialog.Builder(this)
            .setTitle(R.string.wellbeing_summary)
            .setMessage(getString(R.string.wellbeing_msg, h, m))
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void setupToolbarActions() {
        View toolbarLogo = findViewById(R.id.toolbarLogo);
        if (toolbarLogo != null) {
            toolbarLogo.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, PersonalNotesActivity.class))));
        }

        View privacyMaskIcon = findViewById(R.id.privacyMaskIcon);
        if (privacyMaskIcon != null) {
            privacyMaskIcon.setOnClickListener(v -> {
                isPrivacyMaskActive = !isPrivacyMaskActive;
                usagePrefs.edit().putBoolean("privacy_mask_active", isPrivacyMaskActive).apply();
                applyStealthUI();
                if (chatAdapter != null) {
                    chatAdapter.setMasked(isPrivacyMaskActive);
                }
                Toast.makeText(this, isPrivacyMaskActive ? "Content Hidden" : "Content Visible", Toast.LENGTH_SHORT).show();
            });
        }

        View notificationIcon = findViewById(R.id.notificationIcon);
        if (notificationIcon != null) {
            notificationIcon.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, NotificationsActivity.class))));
        }

        View ivSearch = findViewById(R.id.ivSearch);
        if (ivSearch != null) {
            ivSearch.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, SearchActivity.class))));
        }
        View ivSettings = findViewById(R.id.ivSettings);
        if (ivSettings != null) {
            ivSettings.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, SettingsActivity.class))));
        }
    }

    private void setupFilters() {
        LinearLayout filterContainer = findViewById(R.id.filterContainer);
        if (filterContainer == null) return;

        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            final int index = i;
            filterContainer.getChildAt(i).setOnClickListener(v -> {
                activeFilter = index;
                updateFilterUI();
                refreshChatsList();
            });
        }
        updateFilterUI();
    }

    private void updateFilterUI() {
        LinearLayout filterContainer = findViewById(R.id.filterContainer);
        if (filterContainer == null) return;

        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            View child = filterContainer.getChildAt(i);
            if (!(child instanceof TextView)) continue;
            TextView tv = (TextView) child;
            if (i == activeFilter) {
                tv.setBackgroundResource(R.drawable.bg_glass_card);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setBackgroundResource(R.drawable.bg_glass_card_outline);
                tv.setTextColor(Color.GRAY);
            }
        }
    }

    private void refreshChatsList() {
        List<ChatModel> allChats = SharedDataManager.getInstance().getGroups();
        displayChatList.clear();

        for (ChatModel chat : allChats) {
            if (activeFilter == 0) displayChatList.add(chat);
            else if (activeFilter == 1 && chat.isGroup()) displayChatList.add(chat);
        }
        if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
    }

    private void setupFloatingInterface() {
        View fabProfile = findViewById(R.id.fabProfile);
        if (fabProfile != null) {
            fabProfile.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, UserProfileActivity.class))));
        }
        View fabAIChat = findViewById(R.id.fabAIChat);
        if (fabAIChat != null) {
            fabAIChat.setOnClickListener(v -> checkStealthAndRun(() -> startActivity(new Intent(this, AIChatActivity.class))));
        }
    }

    private void startGlobalListeners() {
        SharedDataManager.getInstance().setGroupListener(group -> refreshChatsList());
        LANManager.getInstance(this).start(this);
    }

    @Override
    protected void checkStealthAndRun(Runnable action) {
        if (isPrivacyMaskActive) {
            Toast.makeText(this, R.string.stealth_mode_active, Toast.LENGTH_SHORT).show();
        } else {
            super.checkStealthAndRun(action);
        }
    }

    private void applyStealthUI() {
        if (stealthOverlay != null) {
            stealthOverlay.setVisibility(isPrivacyMaskActive ? View.VISIBLE : View.GONE);
        }
    }

    private void updateUserAvatar(String uriString) {
        if (ivFabProfile == null) return;
        Glide.with(this).load(Uri.parse(uriString)).circleCrop().into(ivFabProfile);
    }

    private void updateUserCardState() {
        View card = findViewById(R.id.cardCurrentUser);
        if (card == null) return;

        ImageView ivAvatar = card.findViewById(R.id.ivUserAvatar);
        TextView tvName = card.findViewById(R.id.tvUserName);
        TextView tvStatus = card.findViewById(R.id.tvUserStatus);

        String name = appPrefs.getString("user_full_name", "LinkUp User");
        if (tvName != null) tvName.setText(name);

        String savedAvatar = appPrefs.getString(PREF_USER_AVATAR, usagePrefs.getString(PREF_USER_AVATAR, null));
        if (savedAvatar != null && ivAvatar != null) {
            Glide.with(this).load(Uri.parse(savedAvatar)).circleCrop().into(ivAvatar);
        }

        int postCount = SharedDataManager.getInstance().getPosts().size();
        if (tvStatus != null) tvStatus.setText(getString(R.string.status_updates_count, postCount));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        statsHandler.removeCallbacksAndMessages(null);
        LANManager.getInstance(this).stop(this);
        SharedDataManager.getInstance().removePostListener(this);
        if (realtimeListener != null) {
            SupabaseRealtimeManager.getInstance().stopListening(realtimeListener);
        }
    }
}
