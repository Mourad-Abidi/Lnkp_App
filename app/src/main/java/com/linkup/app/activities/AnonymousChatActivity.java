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
import com.linkup.app.models.ChatModel;
import java.util.ArrayList;
import java.util.List;

public class AnonymousChatActivity extends BaseActivity {

    private TextInputEditText etSearchUser;
    private MaterialButton btnAddUser;
    private RecyclerView rvSearchResults;
    private LinearLayout llNoResults;
    private List<ChatModel> mockAnonymousRooms;
    private ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Anonymous Chat");
        }

        etSearchUser = findViewById(R.id.etSearchUser);
        btnAddUser = findViewById(R.id.btnAddUser);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        llNoResults = findViewById(R.id.llNoResults);

        setupData();
        setupListeners();
    }

    private void setupData() {
        mockAnonymousRooms = new ArrayList<>();
        mockAnonymousRooms.add(new ChatModel("Privacy Hub", "5 members online", "Active", 0, false));
        mockAnonymousRooms.add(new ChatModel("Crypto Talk", "12 members online", "Busy", 0, false));
        mockAnonymousRooms.add(new ChatModel("Ghost Network", "2 members online", "Quiet", 0, false));
        
        adapter = new ChatAdapter(mockAnonymousRooms);
        if (rvSearchResults != null) {
            rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
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
                Toast.makeText(this, "Searching for random anonymous partner...", Toast.LENGTH_LONG).show();
                new android.os.Handler().postDelayed(() -> {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("user_name", "Anonymous Ghost");
                    startActivity(intent);
                }, 2000);
            });
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            if (llNoResults != null) llNoResults.setVisibility(View.GONE);
            adapter.setChatList(mockAnonymousRooms);
            return;
        }

        List<ChatModel> filtered = new ArrayList<>();
        for (ChatModel room : mockAnonymousRooms) {
            if (room.getUserName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(room);
            }
        }

        if (filtered.isEmpty()) {
            if (llNoResults != null) llNoResults.setVisibility(View.VISIBLE);
            adapter.setChatList(new ArrayList<>());
        } else {
            if (llNoResults != null) llNoResults.setVisibility(View.GONE);
            adapter.setChatList(filtered);
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
