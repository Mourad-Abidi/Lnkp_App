package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.linkup.app.R;
import com.linkup.app.models.Post;
import java.util.List;

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.ViewHolder> {

    private List<Post> posts;

    public ProfilePostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        
        // Telegram style: tight grid of images
        if (post.mediaPath != null && !post.mediaPath.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(post.mediaPath)
                .placeholder(R.drawable.my_background_1)
                .centerCrop()
                .into(holder.ivPostImage);
            holder.ivPostIcon.setVisibility(View.GONE);
        } else {
            // If it's a text-only post, show a placeholder with a lock icon (Encrypted vibe)
            holder.ivPostImage.setImageResource(R.drawable.bg_gradient_transparent_black);
            holder.ivPostIcon.setVisibility(View.VISIBLE);
            holder.ivPostIcon.setImageResource(android.R.drawable.ic_lock_lock);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage, ivPostIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivPostIcon = itemView.findViewById(R.id.ivPostIcon);
        }
    }
}
