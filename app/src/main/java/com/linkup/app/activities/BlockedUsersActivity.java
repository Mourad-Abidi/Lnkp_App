package com.linkup.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.models.ChatModel;
import java.util.ArrayList;
import java.util.List;

public class BlockedUsersActivity extends BaseActivity {

    private ChatAdapter adapter;
    private List<ChatModel> blockedList;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvBlockedUsers = findViewById(R.id.rvBlockedUsers);
        tvEmpty = findViewById(R.id.tvEmpty);

        blockedList = new ArrayList<>();
        // Demo blocked users
        blockedList.add(new ChatModel("Spammer Bot", "Blocked on Oct 10", "Now", 0, false));
        blockedList.add(new ChatModel("Unknown User", "Blocked on Oct 12", "Now", 0, false));

        adapter = new ChatAdapter(blockedList);
        rvBlockedUsers.setLayoutManager(new LinearLayoutManager(this));
        rvBlockedUsers.setAdapter(adapter);

        updateUI();
    }

    private void updateUI() {
        if (blockedList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }
}
