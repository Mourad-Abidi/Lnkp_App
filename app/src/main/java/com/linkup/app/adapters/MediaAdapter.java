package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.MediaModel;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private List<MediaModel> mediaList;
    private OnMediaClickListener listener;

    public interface OnMediaClickListener {
        void onMediaClick(MediaModel media);
    }

    public MediaAdapter(List<MediaModel> mediaList, OnMediaClickListener listener) {
        this.mediaList = mediaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaModel media = mediaList.get(position);
        
        if (media.getType() == MediaModel.MediaType.IMAGE || media.getType() == MediaModel.MediaType.VIDEO) {
            holder.ivMedia.setVisibility(View.VISIBLE);
            holder.docLinkContainer.setVisibility(View.GONE);
            
            // In a real app, use Glide or Picasso
            holder.ivMedia.setImageResource(R.drawable.app_logo);
            
            if (media.getType() == MediaModel.MediaType.VIDEO) {
                holder.ivVideoIcon.setVisibility(View.VISIBLE);
            } else {
                holder.ivVideoIcon.setVisibility(View.GONE);
            }
        } else {
            holder.ivMedia.setVisibility(View.GONE);
            holder.ivVideoIcon.setVisibility(View.GONE);
            holder.docLinkContainer.setVisibility(View.VISIBLE);
            
            holder.tvMediaTitle.setText(media.getTitle());
            
            if (media.getType() == MediaModel.MediaType.DOCUMENT) {
                holder.ivTypeIcon.setImageResource(android.R.drawable.ic_menu_agenda);
            } else if (media.getType() == MediaModel.MediaType.LINK) {
                holder.ivTypeIcon.setImageResource(android.R.drawable.ic_menu_share);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMediaClick(media);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public void updateData(List<MediaModel> newList) {
        this.mediaList = newList;
        notifyDataSetChanged();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia, ivVideoIcon, ivTypeIcon;
        TextView tvMediaTitle;
        LinearLayout docLinkContainer;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            ivVideoIcon = itemView.findViewById(R.id.ivVideoIcon);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            tvMediaTitle = itemView.findViewById(R.id.tvMediaTitle);
            docLinkContainer = itemView.findViewById(R.id.docLinkContainer);
        }
    }
}
