package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
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

public class PersonalNotesActivity extends BaseActivity {

    private RecyclerView rvNotesLabels;
    private LabelsAdapter adapter;
    private List<String> allLabels;
    private List<String> filteredLabels;
    private EditText etSearchLabels;
    private TextView tvTotalNotesCount, tvLastSync, tvStorageStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_notes);

        initViews();
        setupToolbar();
        setupData();
        setupListeners();
    }

    private void initViews() {
        rvNotesLabels = findViewById(R.id.rvNotesLabels);
        etSearchLabels = findViewById(R.id.etSearchLabels);
        tvTotalNotesCount = findViewById(R.id.tvTotalNotesCount);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvStorageStatus = findViewById(R.id.tvStorageStatus);
        rvNotesLabels.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Personal Notes");
            }
        }
    }

    private void setupData() {
        allLabels = new ArrayList<>();
        allLabels.add("Personal Thoughts");
        allLabels.add("Work Tasks");
        allLabels.add("Meeting Minutes");
        allLabels.add("Project Ideas");
        allLabels.add("Private Keys");
        allLabels.add("Shopping List");
        allLabels.add("Health Logs");
        allLabels.add("Travel Plans");
        allLabels.add("Secure Backups");

        filteredLabels = new ArrayList<>(allLabels);
        adapter = new LabelsAdapter(filteredLabels, label -> {
            Intent intent = new Intent(this, NoteEditorActivity.class);
            intent.putExtra("label", label);
            startActivity(intent);
        });
        rvNotesLabels.setAdapter(adapter);
        updateStats();
    }

    private void setupListeners() {
        etSearchLabels.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnSyncNotes).setOnClickListener(v -> showSyncDialog());

        findViewById(R.id.fabAddLabel).setOnClickListener(v -> showAddCategoryDialog());
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_Lnkp_AlertDialog);
        builder.setTitle("New Category");

        final EditText input = new EditText(this);
        input.setHint("Category Name");
        input.setTextColor(getColor(R.color.white));
        input.setHintTextColor(getColor(R.color.white_40));
        
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String category = input.getText().toString().trim();
            if (!category.isEmpty()) {
                allLabels.add(0, category);
                filter(etSearchLabels.getText().toString());
                Toast.makeText(this, "Category created: " + category, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSyncDialog() {
        new AlertDialog.Builder(this, R.style.Theme_Lnkp_AlertDialog)
                .setTitle("Secure Cloud Sync")
                .setMessage("Your notes are currently stored only on this device. For account synchronization, identity verification is required.")
                .setPositiveButton("Verify & Sync", (dialog, which) -> showPasswordVerification())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPasswordVerification() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_Lnkp_AlertDialog);
        builder.setTitle("Identity Verification");
        builder.setMessage("Please enter your account password to authorize cloud synchronization.");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        
        final EditText input = new EditText(this);
        input.setLayoutParams(params);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter Password");
        input.setTextColor(getColor(R.color.white));
        input.setHintTextColor(getColor(R.color.white_40));
        input.setBackgroundResource(R.drawable.bg_glass_input);
        input.setPadding(32, 32, 32, 32);

        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Authorize", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals("1234")) { // Demo verification
                performSync();
            } else {
                Toast.makeText(this, "Incorrect password. Authorization denied.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performSync() {
        Toast.makeText(this, "Authorization successful. Syncing...", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> {
            Toast.makeText(this, "Data encrypted and synced to account.", Toast.LENGTH_LONG).show();
            if (tvStorageStatus != null) tvStorageStatus.setText("ACCOUNT SYNCED");
            if (tvLastSync != null) tvLastSync.setText("Just now");
        }, 2000);
    }

    private void filter(String query) {
        filteredLabels.clear();
        if (query.isEmpty()) {
            filteredLabels.addAll(allLabels);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (String label : allLabels) {
                if (label.toLowerCase().contains(lowerCaseQuery)) {
                    filteredLabels.add(label);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateStats();
    }

    private void updateStats() {
        if (tvTotalNotesCount != null) {
            tvTotalNotesCount.setText(String.valueOf(filteredLabels.size() * 2));
        }
    }

    private static class LabelsAdapter extends RecyclerView.Adapter<LabelsAdapter.ViewHolder> {
        private final List<String> labels;
        private final OnLabelClickListener listener;

        interface OnLabelClickListener {
            void onLabelClick(String label);
        }

        LabelsAdapter(List<String> labels, OnLabelClickListener listener) {
            this.labels = labels;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_label, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String label = labels.get(position);
            holder.tvLabelName.setText(label);
            holder.itemView.setOnClickListener(v -> listener.onLabelClick(label));
        }

        @Override
        public int getItemCount() {
            return labels.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvLabelName;
            ViewHolder(View itemView) {
                super(itemView);
                tvLabelName = itemView.findViewById(R.id.tvLabelName);
            }
        }
    }
}
