package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.linkup.app.R;
import com.linkup.app.adapters.ChatAdapter;
import com.linkup.app.models.ChatModel;
import java.util.ArrayList;
import java.util.List;

public class ConnectionsActivity extends BaseActivity {

    private RecyclerView rvConnections;
    private ChatAdapter adapter;
    private List<ChatModel> followersList = new ArrayList<>();
    private List<ChatModel> followingList = new ArrayList<>();
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections);

        String initialType = getIntent().getStringExtra("type"); 
        String userName = getIntent().getStringExtra("user_name");

        ImageView btnBack = findViewById(R.id.btnBack);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        rvConnections = findViewById(R.id.rvConnections);
        tvTitle = findViewById(R.id.tvTitle);

        if (tvTitle != null && userName != null) {
            tvTitle.setText(userName + "'s Connections");
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupDummyData();

        if (rvConnections != null) {
            rvConnections.setLayoutManager(new LinearLayoutManager(this));
        }

        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateList(tab.getPosition() == 0);
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });

            if ("following".equalsIgnoreCase(initialType)) {
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                if (tab != null) tab.select();
            } else {
                updateList(true);
            }
        } else {
            updateList(true);
        }
    }

    private void setupDummyData() {
        followersList.add(new ChatModel("John Smith", "Following you", "2h ago", 0, false));
        followersList.add(new ChatModel("Sarah Connor", "Following you", "5h ago", 0, false));
        followersList.add(new ChatModel("Tech Geek", "Following you", "Yesterday", 0, false));

        followingList.add(new ChatModel("LinkUp Official", "You are following", "", 0, false));
        followingList.add(new ChatModel("Design Team", "You are following", "", 0, false));
    }

    private void updateList(boolean isFollowers) {
        List<ChatModel> currentList = isFollowers ? followersList : followingList;
        adapter = new ChatAdapter(currentList);
        
        // Custom Interaction: Clicking a connection opens their profile
        // Note: ChatAdapter would ideally handle this, but we'll simulate the interaction here
        if (rvConnections != null) {
            rvConnections.setAdapter(adapter);
            // In a real app, the adapter's onClick would be set here or inside the ViewHolder
        }
    }
}
