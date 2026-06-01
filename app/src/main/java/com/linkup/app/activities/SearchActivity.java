package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.User;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class SearchActivity extends BaseActivity {

    private EditText etSearch;
    private ImageButton btnClearSearch;
    private RecyclerView rvSearchResults;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout llEmptySearch;
    private TextView tvSearchHint;
    private TextView tvEmptySubtitle;
    private ProgressBar pbLoading;
    
    private ChatAdapter resultsAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String currentQuery = "";
    private Call<List<User>> currentCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupAdapter();
        setupListeners();
        
        // Initial fetch
        performSearch("");
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        llEmptySearch = findViewById(R.id.llEmptySearch);
        tvSearchHint = findViewById(R.id.tvSearchHint);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        pbLoading = findViewById(R.id.pbLoading);

        if (rvSearchResults != null) {
            rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        }

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary);
            swipeRefresh.setOnRefreshListener(() -> performSearch(currentQuery));
        }
    }

    private void setupAdapter() {
        resultsAdapter = new ChatAdapter(this, new ArrayList<>(), chat -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("user_name", chat.getUserName());
            intent.putExtra("user_id", chat.getUserId());
            startActivity(intent);
        });
        
        if (rvSearchResults != null) {
            rvSearchResults.setAdapter(resultsAdapter);
        }
    }

    private void setupListeners() {
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearch != null) etSearch.setText("");
            });
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s.toString().trim();
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> performSearch(currentQuery);
                    searchHandler.postDelayed(searchRunnable, 400); // 400ms debounce
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void performSearch(String query) {
        if (currentCall != null) {
            currentCall.cancel();
        }

        if (!swipeRefresh.isRefreshing()) {
            showLoading(true);
        }

        FirebaseDatabaseManager.OnUsersFetchedListener listener = users -> {
            showLoading(false);
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            
            if (users == null) {
                // If the error persists after changing to 'profiles', it might be an RLS issue
                showError("Could not connect to profiles. Check database settings.");
                return;
            }
            
            Log.d("SearchActivity", "Fetched " + users.size() + " users");
            updateUI(users, query.isEmpty() ? "No other users found in the system." : "No results for '" + query + "'");
        };

        if (query.isEmpty()) {
            currentCall = FirebaseDatabaseManager.getInstance().fetchAllUsers(listener);
        } else {
            currentCall = FirebaseDatabaseManager.getInstance().searchUsersByName(query, listener);
        }
    }

    private void showLoading(boolean loading) {
        if (pbLoading != null) pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            if (llEmptySearch != null) llEmptySearch.setVisibility(View.GONE);
            if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        showEmptyState(true, "Connection Issue", message);
    }

    private void updateUI(List<User> users, String emptyMessage) {
        List<ChatModel> chatModels = new ArrayList<>();
        String currentUserId = FirebaseDatabaseManager.getInstance().getCurrentUserId();

        for (User user : users) {
            // Exclude current user
            if (user.getUserId() != null && user.getUserId().equals(currentUserId)) {
                continue;
            }

            // Map data with fallback logic
            String displayName = user.getFullName();
            if (displayName == null || displayName.isEmpty()) displayName = user.getUsername();
            if (displayName == null || displayName.isEmpty()) displayName = "New User";

            String subtitle = user.getAccountStatus();
            if (subtitle == null || subtitle.isEmpty()) {
                subtitle = user.getUsername() != null ? "@" + user.getUsername() : "Active User";
            }

            ChatModel model = new ChatModel(displayName, subtitle, "", 0, false, true);
            model.setUserId(user.getUserId());
            model.setProfilePhoto(user.getProfilePhoto());
            chatModels.add(model);
        }

        if (chatModels.isEmpty()) {
            showEmptyState(true, "Nothing to show", emptyMessage);
            resultsAdapter.setChatList(new ArrayList<>());
        } else {
            showEmptyState(false, "", "");
            resultsAdapter.setChatList(chatModels);
        }
    }

    private void showEmptyState(boolean show, String title, String subtitle) {
        if (llEmptySearch != null) {
            llEmptySearch.setVisibility(show ? View.VISIBLE : View.GONE);
            if (tvSearchHint != null) tvSearchHint.setText(title);
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText(subtitle);
        }
        if (rvSearchResults != null) rvSearchResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        if (currentCall != null) currentCall.cancel();
        super.onDestroy();
    }
}
