package com.linkup.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.linkup.app.R;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.NotificationModel;

public class SharedContentDetailActivity extends BaseActivity {

    private String ownerId;
    private String content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_content_detail);

        ownerId = getIntent().getStringExtra("user_id");
        String userName = getIntent().getStringExtra("user_name");
        content = getIntent().getStringExtra("content");
        String mediaPath = getIntent().getStringExtra("media_path");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvPostText = findViewById(R.id.tvPostText);
        TextView tvCenterText = findViewById(R.id.tvCenterText);
        ImageView ivPostContent = findViewById(R.id.ivPostContent);
        View llDescriptionArea = findViewById(R.id.llDescriptionArea);
        
        if (tvUserName != null) tvUserName.setText(userName);
        
        boolean hasImage = mediaPath != null && !mediaPath.isEmpty();
        
        if (hasImage) {
            if (ivPostContent != null) {
                ivPostContent.setVisibility(View.VISIBLE);
                Glide.with(this).load(mediaPath).placeholder(R.drawable.my_background_6).into(ivPostContent);
            }
            if (tvCenterText != null) tvCenterText.setVisibility(View.GONE);
            if (llDescriptionArea != null) {
                llDescriptionArea.setVisibility(View.VISIBLE);
                if (tvPostText != null) tvPostText.setText(content);
            }
        } else {
            if (ivPostContent != null) {
                ivPostContent.setImageResource(R.drawable.my_background_6);
                ivPostContent.setVisibility(View.VISIBLE);
            }
            if (tvCenterText != null) {
                tvCenterText.setVisibility(View.VISIBLE);
                tvCenterText.setText(content);
            }
            if (llDescriptionArea != null) llDescriptionArea.setVisibility(View.GONE);
        }

        setupInteractions();
    }

    private void setupInteractions() {
        TextView btnLike = findViewById(R.id.btnLike);
        TextView btnDislike = findViewById(R.id.btnDislike);

        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                boolean isLiked = btnLike.getText().toString().contains("❤️");
                if (!isLiked) {
                    btnLike.setText("❤️ Liked");
                    if (btnDislike != null) btnDislike.setText("👎 Dislike");
                    notifyOwnerOfInteraction("liked");
                } else {
                    btnLike.setText("🤍 Like");
                }
            });
        }

        if (btnDislike != null) {
            btnDislike.setOnClickListener(v -> {
                boolean isDisliked = btnDislike.getText().toString().contains("Disliked");
                if (!isDisliked) {
                    btnDislike.setText("👎 Disliked");
                    if (btnLike != null) btnLike.setText("🤍 Like");
                    notifyOwnerOfInteraction("disliked");
                } else {
                    btnDislike.setText("👎 Dislike");
                }
            });
        }
    }

    private void notifyOwnerOfInteraction(String type) {
        String myName = appPrefs.getString("user_full_name", "Someone");
        String message = myName + " " + type + " your post.";

        // Send notification to the global manager so it appears in the Notification screen
        SharedDataManager.getInstance().addNotification(new NotificationModel(
            "New Interaction",
            message,
            "Now",
            R.drawable.ic_notification,
            R.drawable.app_logo,
            false,
            null
        ));

        // Simulated Remote Notification
        if (ownerId != null) {
            FirebaseDatabaseManager.getInstance().sendNotification(ownerId, message);
        }
        
        Toast.makeText(this, "Notification sent to " + (ownerId != null ? ownerId : "owner"), Toast.LENGTH_SHORT).show();
    }
}
