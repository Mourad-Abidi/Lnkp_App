package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.Post;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;
import java.util.List;

public class RecentUpdatesAdapter extends RecyclerView.Adapter<RecentUpdatesAdapter.ViewHolder> {

    private List<Post> posts;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public RecentUpdatesAdapter(List<Post> posts) {
        this.posts = posts;
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void updatePosts(List<Post> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follower_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.tvStoryName.setText(post.userName);

        // Background story image
        if (post.mediaPath != null && !post.mediaPath.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(post.mediaPath)
                .placeholder(R.drawable.my_background_1)
                .centerCrop()
                .into(holder.ivStoryImage);
        } else {
            holder.ivStoryImage.setImageResource(R.drawable.my_background_1);
        }

        // Small user avatar
        Glide.with(holder.itemView.getContext())
            .load(R.drawable.app_logo) // Default avatar for now
            .circleCrop()
            .into(holder.ivStoryAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryImage;
        ShapeableImageView ivStoryAvatar;
        TextView tvStoryName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryImage = itemView.findViewById(R.id.ivStoryImage);
            ivStoryAvatar = itemView.findViewById(R.id.ivStoryAvatar);
            tvStoryName = itemView.findViewById(R.id.tvStoryName);
        }
    }
}
