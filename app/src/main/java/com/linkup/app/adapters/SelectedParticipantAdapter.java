package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.ChatModel;
import java.util.List;

public class SelectedParticipantAdapter extends RecyclerView.Adapter<SelectedParticipantAdapter.ViewHolder> {

    private List<ChatModel> selectedUsers;
    private OnRemoveClickListener removeClickListener;

    public interface OnRemoveClickListener {
        void onRemoveClick(ChatModel user);
    }

    public SelectedParticipantAdapter(List<ChatModel> selectedUsers, OnRemoveClickListener listener) {
        this.selectedUsers = selectedUsers;
        this.removeClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatModel user = selectedUsers.get(position);
        holder.tvName.setText(user.getUserName());
        // In a real app, load avatar here
        
        holder.ivRemove.setOnClickListener(v -> {
            if (removeClickListener != null) {
                removeClickListener.onRemoveClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivAvatar, ivRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSelectedName);
            ivAvatar = itemView.findViewById(R.id.ivSelectedAvatar);
            ivRemove = itemView.findViewById(R.id.ivRemoveParticipant);
        }
    }
}
