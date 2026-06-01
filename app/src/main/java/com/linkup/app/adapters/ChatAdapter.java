package com.linkup.app.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.linkup.app.R;
import com.linkup.app.activities.CallActivity;
import com.linkup.app.activities.ChatActivity;
import com.linkup.app.activities.LANChatRoomActivity;
import com.linkup.app.activities.UserProfileActivity;
import com.linkup.app.models.ChatModel;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatModel> chatList;
    private boolean isMasked = false;
    private boolean isSelectionMode = false;
    private boolean isCallList = false;
    private OnSelectionChangedListener selectionListener;
    private OnChatClickListener clickListener;
    private SharedPreferences settings;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public interface OnChatClickListener {
        void onChatClick(ChatModel chat);
    }

    public ChatAdapter(List<ChatModel> chatList) {
        this.chatList = chatList;
    }

    public ChatAdapter(List<ChatModel> chatList, boolean isCallList) {
        this.chatList = chatList;
        this.isCallList = isCallList;
    }

    public ChatAdapter(Context context, List<ChatModel> chatList, OnChatClickListener clickListener) {
        this.chatList = chatList;
        this.clickListener = clickListener;
    }

    public void setSelectionMode(boolean selectionMode, OnSelectionChangedListener listener) {
        this.isSelectionMode = selectionMode;
        this.selectionListener = listener;
    }

    public void setMasked(boolean masked) {
        this.isMasked = masked;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (settings == null) {
            settings = parent.getContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        
        // Apply Compact Mode from Settings
        boolean isCompact = settings != null && settings.getBoolean("chat_compact_mode", false);
        if (holder.ivUserAvatar != null) {
            holder.ivUserAvatar.setVisibility(isCompact ? View.GONE : View.VISIBLE);
            
            // Load profile photo if available
            if (chat.getProfilePhoto() != null && !chat.getProfilePhoto().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                    .load(chat.getProfilePhoto())
                    .placeholder(R.drawable.app_logo)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(R.drawable.app_logo);
            }

            holder.ivUserAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), UserProfileActivity.class);
                intent.putExtra("user_name", chat.getUserName());
                v.getContext().startActivity(intent);
            });
        }

        // Apply Privacy Masking
        boolean effectiveMask = isMasked || (settings != null && settings.getBoolean("privacy_mask_active", false));

        if (effectiveMask) {
            holder.tvUserName.setText("********");
            if (holder.tvLastMessage != null) {
                holder.tvLastMessage.setText("••••••••••••••••");
                holder.tvLastMessage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                holder.tvLastMessage.getPaint().setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));
            }
        } else {
            holder.tvUserName.setText(chat.getUserName());
            if (holder.tvLastMessage != null) {
                holder.tvLastMessage.setText(chat.getLastMessage());
                holder.tvLastMessage.getPaint().setMaskFilter(null);
            }
        }
        
        // Apply Font Size from Settings
        if (settings != null) {
            int fontProgress = settings.getInt("font_size_progress", 1);
            float size = 16f;
            if (fontProgress == 0) size = 13f;
            else if (fontProgress == 2) size = 20f;
            holder.tvUserName.setTextSize(size);
            if (holder.tvLastMessage != null) holder.tvLastMessage.setTextSize(size - 2);
        }

        if (holder.tvTime != null) {
            holder.tvTime.setText(chat.getTime());
        }

        if (holder.tvUnreadCount != null) {
            if (chat.getUnreadCount() > 0) {
                holder.tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnreadCount.setVisibility(View.GONE);
            }
        }

        if (holder.viewOnlineIndicator != null) {
            holder.viewOnlineIndicator.setVisibility(chat.isOnline() ? View.VISIBLE : View.GONE);
        }

        if (holder.ivLanIndicator != null) {
            holder.ivLanIndicator.setVisibility(chat.getIpAddress() != null ? View.VISIBLE : View.GONE);
        }

        if (isSelectionMode) {
            holder.itemView.setBackgroundResource(chat.isSelected() ? R.drawable.bg_selected_item : 0);
        } else {
            holder.itemView.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                chat.setSelected(!chat.isSelected());
                notifyItemChanged(position);
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(getSelectedCount());
                }
            } else if (clickListener != null) {
                clickListener.onChatClick(chat);
            } else {
                if (isCallList) {
                    Intent intent = new Intent(v.getContext(), CallActivity.class);
                    intent.putExtra("user_name", chat.getUserName());
                    v.getContext().startActivity(intent);
                } else if (chat.getIpAddress() != null) {
                    Intent intent = new Intent(v.getContext(), LANChatRoomActivity.class);
                    intent.putExtra("user_name", chat.getUserName());
                    intent.putExtra("ip_address", chat.getIpAddress());
                    v.getContext().startActivity(intent);
                } else {
                    Intent intent = new Intent(v.getContext(), ChatActivity.class);
                    intent.putExtra("user_name", chat.getUserName());
                    intent.putExtra("user_id", chat.getUserId());
                    intent.putExtra("is_group", chat.isGroup());
                    v.getContext().startActivity(intent);
                }
            }
        });
    }

    private int getSelectedCount() {
        int count = 0;
        for (ChatModel chat : chatList) {
            if (chat.isSelected()) count++;
        }
        return count;
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public List<ChatModel> getChatList() {
        return chatList;
    }

    public void setChatList(List<ChatModel> chatList) {
        this.chatList = chatList;
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvLastMessage, tvTime, tvUnreadCount;
        ImageView ivUserAvatar, ivLanIndicator;
        View viewOnlineIndicator;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage); 
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            ivLanIndicator = itemView.findViewById(R.id.ivLanIndicator);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            viewOnlineIndicator = itemView.findViewById(R.id.viewOnlineIndicator);
        }
    }
}
