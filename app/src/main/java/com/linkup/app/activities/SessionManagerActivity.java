package com.linkup.app.activities;

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
import com.linkup.app.R;
import java.util.ArrayList;
import java.util.List;

public class SessionManagerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvOtherSessions = findViewById(R.id.rvOtherSessions);
        rvOtherSessions.setLayoutManager(new LinearLayoutManager(this));

        List<Session> sessions = new ArrayList<>();
        sessions.add(new Session("Windows PC", "Chrome Browser • London, UK", "Active 2h ago"));
        sessions.add(new Session("iPhone 13", "LinkUp App • Paris, FR", "Active yesterday"));

        rvOtherSessions.setAdapter(new SessionAdapter(sessions));

        findViewById(R.id.btnLogoutAll).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Logout all other devices?")
                .setMessage("This will end all active sessions except for this device.")
                .setPositiveButton("Logout All", (dialog, which) -> {
                    Toast.makeText(this, "Logged out from all other devices", Toast.LENGTH_SHORT).show();
                    sessions.clear();
                    rvOtherSessions.getAdapter().notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private static class Session {
        String device, location, lastActive;
        Session(String d, String l, String a) { device = d; location = l; lastActive = a; }
    }

    private static class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
        private List<Session> sessions;
        SessionAdapter(List<Session> s) { sessions = s; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Session s = sessions.get(position);
            holder.text1.setText(s.device);
            holder.text1.setTextColor(android.graphics.Color.WHITE);
            holder.text2.setText(s.location + " • " + s.lastActive);
            holder.text2.setTextColor(android.graphics.Color.LTGRAY);
        }

        @Override
        public int getItemCount() { return sessions.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
