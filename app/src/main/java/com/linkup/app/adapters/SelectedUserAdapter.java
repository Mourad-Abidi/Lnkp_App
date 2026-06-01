package com.linkup.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.ChatModel;
import java.util.List;

public class SelectedUserAdapter extends RecyclerView.Adapter<SelectedUserAdapter.ViewHolder> {

    private final List<ChatModel> selectedUsers;
    private final OnUserRemovedListener listener;

    public interface OnUserRemovedListener {
        void onUserRemoved(ChatModel user);
    }

    public SelectedUserAdapter(List<ChatModel> selectedUsers, OnUserRemovedListener listener) {
        this.selectedUsers = selectedUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatModel user = selectedUsers.get(position);
        holder.tvUserName.setText(user.getUserName());
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserRemoved(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
