package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.User;
import java.util.ArrayList;
import java.util.List;

public class FindPeopleActivity extends BaseActivity {

    private TextInputEditText etSearchUser;
    private MaterialButton btnAddUser;
    private RecyclerView rvSearchResults;
    private LinearLayout llNoResults;
    
    private ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_people);

        initViews();
        setupAdapter();
        setupListeners();
        
        // Load initial global users as suggestions
        performSearch("");
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Find People");
        }

        etSearchUser = findViewById(R.id.etSearchUser);
        btnAddUser = findViewById(R.id.btnAddUser);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        llNoResults = findViewById(R.id.llNoResults);

        if (rvSearchResults != null) {
            rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupAdapter() {
        adapter = new ChatAdapter(this, new ArrayList<>(), chat -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("user_name", chat.getUserName());
            intent.putExtra("user_id", chat.getUserId());
            startActivity(intent);
        });
        
        if (rvSearchResults != null) {
            rvSearchResults.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        if (etSearchUser != null) {
            etSearchUser.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    performSearch(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnAddUser != null) {
            btnAddUser.setOnClickListener(v -> {
                String input = etSearchUser != null ? etSearchUser.getText().toString().trim() : "";
                if (input.isEmpty()) {
                    Toast.makeText(this, "Enter a name or email to invite", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Searching for user: " + input, Toast.LENGTH_SHORT).show();
                    performSearch(input);
                }
            });
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            // Fetch some global users as suggestions when search is empty
            FirebaseDatabaseManager.getInstance().fetchAllUsers(users -> updateUI(users));
            return;
        }

        // Real-time search in Supabase
        FirebaseDatabaseManager.getInstance().searchUsersByName(query, users -> updateUI(users));
    }

    private void updateUI(List<User> users) {
        List<ChatModel> chatModels = new ArrayList<>();
        String myId = FirebaseDatabaseManager.getInstance().getCurrentUserId();

        for (User user : users) {
            // Don't show current logged in user in results
            if (user.getUserId() != null && user.getUserId().equals(myId)) continue;
            
            ChatModel model = new ChatModel(
                user.getUsername() != null ? user.getUsername() : "Lnkp User",
                user.getAccountStatus() != null ? user.getAccountStatus() : "Online",
                user.getEmail() != null ? user.getEmail() : "Verified Profile",
                0,
                false
            );
            model.setUserId(user.getUserId());
            chatModels.add(model);
        }

        if (chatModels.isEmpty()) {
            if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
            adapter.setChatList(new ArrayList<>());
        } else {
            if (llNoResults != null) llNoResults.setVisibility(View.GONE);
            adapter.setChatList(chatModels);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
