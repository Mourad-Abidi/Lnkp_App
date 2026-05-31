package com.linkup.app.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.NotificationModel;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> originalList;
    private List<NotificationModel> filteredList;
    private OnDataChangedListener dataChangedListener;
    private boolean isFilteringUnread = false;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.originalList = notificationList;
        this.filteredList = new ArrayList<>(originalList);
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.dataChangedListener = listener;
    }

    public void toggleUnreadFilter() {
        isFilteringUnread = !isFilteringUnread;
        applyFilter();
    }

    private void applyFilter() {
        filteredList.clear();
        if (isFilteringUnread) {
            for (NotificationModel n : originalList) {
                if (!n.isRead()) filteredList.add(n);
            }
        } else {
            filteredList.addAll(originalList);
        }
        notifyDataSetChanged();
        if (dataChangedListener != null) dataChangedListener.onDataChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notification = filteredList.get(position);
        
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(notification.getTime());
        holder.ivTypeIcon.setImageResource(notification.getIconRes());
        holder.ivAvatar.setImageResource(notification.getAvatarRes());

        if (notification.isRead()) {
            holder.vUnreadIndicator.setVisibility(View.GONE);
            holder.container.setAlpha(0.6f);
        } else {
            holder.vUnreadIndicator.setVisibility(View.VISIBLE);
            holder.container.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (notification.getTargetActivity() != null) {
                Intent intent = new Intent(v.getContext(), notification.getTargetActivity());
                v.getContext().startActivity(intent);
            }
            if (!notification.isRead()) {
                notification.setRead(true);
                notifyItemChanged(position);
                if (dataChangedListener != null) dataChangedListener.onDataChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public List<NotificationModel> getNotificationList() {
        return filteredList;
    }

    public void markAsRead(int position) {
        filteredList.get(position).setRead(true);
        notifyItemChanged(position);
        if (dataChangedListener != null) dataChangedListener.onDataChanged();
    }

    public void deleteNotification(int position) {
        NotificationModel model = filteredList.get(position);
        originalList.remove(model);
        filteredList.remove(position);
        notifyItemRemoved(position);
        if (dataChangedListener != null) dataChangedListener.onDataChanged();
    }

    public void markAllAsRead() {
        for (NotificationModel n : originalList) {
            n.setRead(true);
        }
        applyFilter();
    }

    public void deleteAll() {
        originalList.clear();
        filteredList.clear();
        notifyDataSetChanged();
        if (dataChangedListener != null) dataChangedListener.onDataChanged();
    }

    public boolean hasUnread() {
        for (NotificationModel n : originalList) {
            if (!n.isRead()) return true;
        }
        return false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        ImageView ivTypeIcon;
        TextView tvTitle, tvMessage, tvTime;
        View vUnreadIndicator;
        View container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivNotifAvatar);
            ivTypeIcon = itemView.findViewById(R.id.ivNotifTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            vUnreadIndicator = itemView.findViewById(R.id.vUnreadIndicator);
            container = itemView.findViewById(R.id.llNotifContainer);
        }
    }
}
