package com.linkup.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.linkup.app.R;
import com.linkup.app.activities.UserProfileActivity;
import com.linkup.app.activities.SharedContentDetailActivity;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.models.FeedModel;
import com.linkup.app.models.NotificationModel;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private List<FeedModel> feedList;
    private SharedPreferences settings;
    private SharedPreferences appPrefs;

    public FeedAdapter(List<FeedModel> feedList) {
        this.feedList = feedList;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (settings == null) {
            settings = parent.getContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        }
        if (appPrefs == null) {
            appPrefs = parent.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedModel feed = feedList.get(position);
        Context context = holder.itemView.getContext();
        
        holder.tvCenterText.setVisibility(View.GONE);
        holder.llDescriptionArea.setVisibility(View.VISIBLE);
        holder.ivPostContent.setVisibility(View.VISIBLE);

        boolean isMasked = settings != null && settings.getBoolean("privacy_mask_active", false);
        int fontProgress = settings != null ? settings.getInt("font_size_progress", 1) : 1;
        float baseSize = (fontProgress == 0) ? 11f : (fontProgress == 2 ? 18f : 14f);

        if (isMasked) {
            holder.tvUserName.setText("********");
            holder.tvPostText.setText("••••••••••••••••••••••••");
            holder.tvCenterText.setText("••••••••");
        } else {
            holder.tvUserName.setText(feed.getUserName());
            holder.tvPostText.setText(feed.getContent());
            holder.tvCenterText.setText(feed.getContent());
        }

        holder.tvUserName.setTextSize(baseSize + 2);
        holder.tvPostText.setTextSize(baseSize);
        if (feed.getTimestamp() != null) holder.tvTime.setText(feed.getTimestamp());
        
        boolean hasImage = feed.getImageUrl() != null && !feed.getImageUrl().isEmpty();
        
        if (!hasImage) {
            holder.ivPostContent.setImageResource(R.drawable.my_background_6); 
            holder.tvCenterText.setVisibility(View.VISIBLE);
            holder.llDescriptionArea.setVisibility(View.GONE); 
            holder.tvPostType.setText("SHARED WORDS");
        } else {
            Glide.with(context).load(feed.getImageUrl()).placeholder(R.drawable.my_background_6).into(holder.ivPostContent);
            holder.tvCenterText.setVisibility(View.GONE);
            holder.llDescriptionArea.setVisibility(View.VISIBLE);
            holder.tvPostType.setText("RECOMMENDED A PHOTO");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SharedContentDetailActivity.class);
            intent.putExtra("post_id", "id_" + position);
            intent.putExtra("user_id", feed.getUserId());
            intent.putExtra("user_name", feed.getUserName());
            intent.putExtra("content", feed.getContent());
            intent.putExtra("media_path", feed.getImageUrl());
            context.startActivity(intent);
        });

        String myName = appPrefs != null ? appPrefs.getString("user_full_name", "Someone") : "Someone";

        holder.btnLike.setOnClickListener(v -> {
            boolean isLiked = holder.btnLike.getText().toString().contains("❤️");
            if (!isLiked) {
                holder.btnLike.setText("❤️ Liked");
                holder.btnDislike.setText("👎 Dislike");
                sendInteractionNotification(feed.getUserId(), myName + " liked your post.");
            } else {
                holder.btnLike.setText("🤍 Like");
            }
        });

        holder.btnDislike.setOnClickListener(v -> {
            boolean isDisliked = holder.btnDislike.getText().toString().contains("Disliked");
            if (!isDisliked) {
                holder.btnDislike.setText("👎 Disliked");
                holder.btnLike.setText("🤍 Like");
                sendInteractionNotification(feed.getUserId(), myName + " disliked your post.");
            } else {
                holder.btnDislike.setText("👎 Dislike");
            }
        });
    }

    private void sendInteractionNotification(String targetUserId, String message) {
        SharedDataManager.getInstance().addNotification(new NotificationModel(
            "Interaction", message, "Now", R.drawable.ic_notification, R.drawable.app_logo, false, null
        ));
    }

    @Override
    public int getItemCount() {
        return feedList.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvPostText, tvCenterText, tvTime, tvPostType, btnLike, btnDislike;
        android.widget.ImageView ivPostContent, ivUserAvatar;
        View llDescriptionArea;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvPostText = itemView.findViewById(R.id.tvPostText);
            tvCenterText = itemView.findViewById(R.id.tvCenterText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPostType = itemView.findViewById(R.id.tvPostType);
            ivPostContent = itemView.findViewById(R.id.ivPostContent);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            llDescriptionArea = itemView.findViewById(R.id.llDescriptionArea);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnDislike = itemView.findViewById(R.id.btnDislike);
        }
    }
}
