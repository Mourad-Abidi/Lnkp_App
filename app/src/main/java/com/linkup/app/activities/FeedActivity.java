package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.linkup.app.R;
import com.linkup.app.adapters.FeedAdapter;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.FeedModel;
import com.linkup.app.models.Post;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedActivity extends BaseActivity implements SharedDataManager.OnPostAddedListener {

    private FeedAdapter adapter;
    private List<FeedModel> currentFeedList = new ArrayList<>();
    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        initViews();
        SharedDataManager.getInstance().addPostListener(this);
        loadRecentData();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                startActivity(new Intent(this, SearchActivity.class));
            });
        }

        rvFeed = findViewById(R.id.rvFeed);
        if (rvFeed != null) {
            rvFeed.setLayoutManager(new LinearLayoutManager(this));
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.primary);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadRecentData();
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        View fabCreate = findViewById(R.id.fabCreate);
        if (fabCreate != null) {
            fabCreate.setOnClickListener(v -> {
                startActivity(new Intent(this, ShareActivity.class));
            });
        }
    }

    private void loadRecentData() {
        currentFeedList.clear();
        
        Set<String> chattedUsers = new HashSet<>();
        for (ChatModel chat : SharedDataManager.getInstance().getGroups()) {
            if (chat.getUserName() != null) chattedUsers.add(chat.getUserName().toLowerCase());
        }
        
        String myName = appPrefs.getString("user_full_name", "").toLowerCase();
        if (!myName.isEmpty()) chattedUsers.add(myName);

        List<Post> userPosts = SharedDataManager.getInstance().getPosts();
        for (Post p : userPosts) {
            if (p.userName != null && (chattedUsers.contains(p.userName.toLowerCase()) || p.userId.equals(appPrefs.getString("user_id", "")))) {
                FeedModel.PostType type = (p.mediaPath == null || p.mediaPath.isEmpty()) ? 
                        FeedModel.PostType.TEXT : FeedModel.PostType.PHOTO;
                
                currentFeedList.add(new FeedModel(
                    p.userName, 
                    p.userId,
                    p.content, 
                    false, 
                    type, 
                    "Now", 
                    "",
                    p.mediaPath
                ));
            }
        }
        
        updateAdapter();
    }

    private void updateAdapter() {
        if (rvFeed == null) return;
        
        if (adapter == null) {
            adapter = new FeedAdapter(currentFeedList);
            rvFeed.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
            rvFeed.scrollToPosition(0);
        }
    }

    @Override
    public void onPostAdded(Post post) {
        runOnUiThread(this::loadRecentData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedDataManager.getInstance().removePostListener(this);
    }
}
