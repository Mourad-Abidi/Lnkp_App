package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.models.ChatModel;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuggestionsActivity extends BaseActivity {

    private SharedPreferences appPrefs;
    private Set<String> acceptedFollowers;
    private SuggestionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        acceptedFollowers = appPrefs.getStringSet("accepted_followers", new HashSet<>());

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvSuggestions = findViewById(R.id.rvSuggestions);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (rvSuggestions != null) {
            rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
            setupSuggestions(rvSuggestions);
        }
    }

    private void setupSuggestions(RecyclerView rv) {
        List<ChatModel> suggestionsList = new ArrayList<>();
        
        // Mock data for suggestions. In a real app, these would come from a server/DB.
        suggestionsList.add(new ChatModel("Alice Johnson", "Requested to follow you", "", 0, false));
        suggestionsList.add(new ChatModel("Bob Miller", "Mutual friend with John Smith", "", 0, false));
        suggestionsList.add(new ChatModel("Diana Prince", "From your contacts", "", 0, false));

        adapter = new SuggestionsAdapter(suggestionsList);
        rv.setAdapter(adapter);
    }

    private class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
        private List<ChatModel> list;

        SuggestionsAdapter(List<ChatModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion_action, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatModel model = list.get(position);
            holder.tvName.setText(model.getUserName());
            holder.tvReason.setText(model.getLastMessage());

            boolean isAccepted = acceptedFollowers.contains(model.getUserName());
            
            if (isAccepted) {
                holder.btnAccept.setText("ACCEPTED");
                holder.btnAccept.setEnabled(false);
                holder.btnRefuse.setVisibility(View.GONE);
            } else {
                holder.btnAccept.setText("ACCEPT");
                holder.btnAccept.setEnabled(true);
                holder.btnRefuse.setVisibility(View.VISIBLE);
                
                holder.btnAccept.setOnClickListener(v -> {
                    acceptedFollowers.add(model.getUserName());
                    appPrefs.edit().putStringSet("accepted_followers", new HashSet<>(acceptedFollowers)).apply();
                    notifyItemChanged(position);
                    Toast.makeText(SuggestionsActivity.this, model.getUserName() + " accepted!", Toast.LENGTH_SHORT).show();
                });

                holder.btnRefuse.setOnClickListener(v -> {
                    list.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(SuggestionsActivity.this, "Request refused", Toast.LENGTH_SHORT).show();
                });
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SuggestionsActivity.this, UserProfileActivity.class);
                intent.putExtra("user_name", model.getUserName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvReason;
            MaterialButton btnAccept, btnRefuse;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvReason = itemView.findViewById(R.id.tvLastMessage);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnRefuse = itemView.findViewById(R.id.btnRefuse);
            }
        }
    }
}
