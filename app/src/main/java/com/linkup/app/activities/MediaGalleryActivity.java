package com.linkup.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.linkup.app.R;
import com.linkup.app.adapters.MediaAdapter;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.MediaModel;
import com.linkup.app.models.Message;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.SupabaseClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MediaGalleryActivity extends BaseActivity implements MediaAdapter.OnMediaClickListener {

    private static final String TAG = "MediaGalleryActivity";
    private RecyclerView rvMediaGallery;
    private MediaAdapter adapter;
    private List<MediaModel> allMediaList = new ArrayList<>();
    private TabLayout tabLayout;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_gallery);

        userName = getIntent().getStringExtra("user_name");
        if (userName == null) userName = "User";

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Shared with " + userName);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tabLayout);
        rvMediaGallery = findViewById(R.id.rvMediaGallery);
        
        setupRecyclerView();
        setupTabs();
        
        loadMediaFromSupabase();
    }

    private void setupRecyclerView() {
        adapter = new MediaAdapter(new ArrayList<>(), this);
        rvMediaGallery.setLayoutManager(new GridLayoutManager(this, 3)); 
        rvMediaGallery.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterMedia(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private String getChatId() {
        String myId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        if (myId.compareTo(userName) < 0) return myId + "_" + userName;
        else return userName + "_" + myId;
    }

    private void loadMediaFromSupabase() {
        ApiService apiService = SupabaseClient.getClient().create(ApiService.class);
        // Fetching messages for the specific chat that have media
        apiService.getMessages(getChatId(), "created_at.desc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allMediaList.clear();
                    for (Message message : response.body()) {
                        processMessage(message);
                    }
                    runOnUiThread(() -> filterMedia(tabLayout.getSelectedTabPosition()));
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e(TAG, "Failed to load media", t);
            }
        });
    }

    private void processMessage(Message message) {
        String type = message.getMessageType();
        if (type == null) return;

        MediaModel.MediaType mediaType = null;
        if (type.equals("IMAGE")) mediaType = MediaModel.MediaType.IMAGE;
        else if (type.equals("VIDEO")) mediaType = MediaModel.MediaType.VIDEO;
        else if (type.equals("DOCUMENT")) mediaType = MediaModel.MediaType.DOCUMENT;
        else if (type.equals("LINK")) mediaType = MediaModel.MediaType.LINK;
        
        if (mediaType != null) {
            String time = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(message.getTimestamp()));
            String content = message.getEncryptedContent();
            if (content == null || content.isEmpty()) {
                content = message.getMessageText();
            }
            MediaModel model = new MediaModel(content, type.toLowerCase(), time, mediaType);
            allMediaList.add(model);
        }
    }

    private void filterMedia(int position) {
        List<MediaModel> filteredList = new ArrayList<>();
        
        if (position == 0) { // Media (Images & Videos)
            rvMediaGallery.setLayoutManager(new GridLayoutManager(this, 3));
            for (MediaModel m : allMediaList) {
                if (m.getType() == MediaModel.MediaType.IMAGE || m.getType() == MediaModel.MediaType.VIDEO) {
                    filteredList.add(m);
                }
            }
        } else if (position == 1) { // Docs
            rvMediaGallery.setLayoutManager(new LinearLayoutManager(this));
            for (MediaModel m : allMediaList) {
                if (m.getType() == MediaModel.MediaType.DOCUMENT) {
                    filteredList.add(m);
                }
            }
        } else if (position == 2) { // Links
            rvMediaGallery.setLayoutManager(new LinearLayoutManager(this));
            for (MediaModel m : allMediaList) {
                if (m.getType() == MediaModel.MediaType.LINK) {
                    filteredList.add(m);
                }
            }
        }
        
        adapter.updateData(filteredList);
    }

    @Override
    public void onMediaClick(MediaModel media) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (media.getType() == MediaModel.MediaType.LINK) {
                intent.setData(Uri.parse(media.getMediaUrl()));
            } else if (media.getType() == MediaModel.MediaType.DOCUMENT) {
                intent.setDataAndType(Uri.parse(media.getMediaUrl()), "application/*");
            } else {
                intent.setDataAndType(Uri.parse(media.getMediaUrl()), media.getType() == MediaModel.MediaType.VIDEO ? "video/*" : "image/*");
            }
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
