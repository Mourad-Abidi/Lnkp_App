package com.linkup.app.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linkup.app.R;
import com.linkup.app.models.GhostMessage;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GhostInboxActivity extends BaseActivity {

    private RecyclerView rvGhostMessages;
    private TextView tvEmptyInbox;
    private List<GhostMessage> ghostList = new ArrayList<>();
    private GhostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ghost_inbox);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvGhostMessages = findViewById(R.id.rvGhostMessages);
        tvEmptyInbox = findViewById(R.id.tvEmptyInbox);

        if (rvGhostMessages != null) {
            rvGhostMessages.setLayoutManager(new LinearLayoutManager(this));
        }
        loadGhostMessages();
    }

    private void loadGhostMessages() {
        SharedPreferences prefs = getSharedPreferences("GhostInbox", MODE_PRIVATE);
        String json = prefs.getString("messages", "[]");
        Type listType = new TypeToken<ArrayList<GhostMessage>>() {}.getType();
        ghostList = new Gson().fromJson(json, listType);

        if (ghostList.isEmpty()) {
            if (tvEmptyInbox != null) tvEmptyInbox.setVisibility(View.VISIBLE);
        } else {
            if (tvEmptyInbox != null) tvEmptyInbox.setVisibility(View.GONE);
        }

        adapter = new GhostAdapter(ghostList);
        if (rvGhostMessages != null) {
            rvGhostMessages.setAdapter(adapter);
        }
    }

    private void openGhostMessage(GhostMessage message, int position) {
        long now = System.currentTimeMillis();
        if (now < message.getScheduledOpenTime()) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            String timeStr = sdf.format(new Date(message.getScheduledOpenTime()));
            Toast.makeText(this, "This message is locked until " + timeStr, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show the message content
        new AlertDialog.Builder(this)
                .setTitle("Anonymous Ghost Message")
                .setMessage(message.getContent())
                .setCancelable(false)
                .setPositiveButton("I've read it", (dialog, which) -> {
                    deleteMessagePermanently(position);
                })
                .show();
    }

    private void deleteMessagePermanently(int position) {
        ghostList.remove(position);
        saveListToPrefs();
        adapter.notifyItemRemoved(position);
        if (ghostList.isEmpty()) {
            if (tvEmptyInbox != null) tvEmptyInbox.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, "Message deleted permanently.", Toast.LENGTH_SHORT).show();
    }

    private void saveListToPrefs() {
        SharedPreferences prefs = getSharedPreferences("GhostInbox", MODE_PRIVATE);
        prefs.edit().putString("messages", new Gson().toJson(ghostList)).apply();
    }

    private class GhostAdapter extends RecyclerView.Adapter<GhostAdapter.ViewHolder> {
        private List<GhostMessage> messages;

        GhostAdapter(List<GhostMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ghost_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GhostMessage msg = messages.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.tvStatus.setText("Unlocks at: " + sdf.format(new Date(msg.getScheduledOpenTime())));

            long now = System.currentTimeMillis();
            if (now < msg.getScheduledOpenTime()) {
                holder.tvHint.setText("Locked (Wait for opening time)");
                holder.tvHint.setTextColor(0x88FFFFFF);
            } else {
                holder.tvHint.setText("Tap to read (One-time only)");
                holder.tvHint.setTextColor(getResources().getColor(R.color.primary));
            }

            holder.itemView.setOnClickListener(v -> openGhostMessage(msg, position));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatus, tvHint;

            ViewHolder(View itemView) {
                super(itemView);
                tvStatus = itemView.findViewById(R.id.tvGhostStatus);
                tvHint = itemView.findViewById(R.id.tvGhostHint);
            }
        }
    }
}
