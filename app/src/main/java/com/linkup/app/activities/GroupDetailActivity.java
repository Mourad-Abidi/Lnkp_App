package com.linkup.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;
import com.linkup.app.adapters.SelectedParticipantAdapter;
import com.linkup.app.models.ChatModel;
import java.util.ArrayList;
import java.util.List;

public class GroupDetailActivity extends BaseActivity {

    private List<ChatModel> members;
    private SelectedParticipantAdapter adapter;
    private TextView tvMembersCount;
    private TextView tvGroupDescription;
    private TextView tvDisappearingStatus;
    private boolean isMuted = false;

    private final ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> selectedUsers = result.getData().getStringArrayListExtra("selected_users");
                    if (selectedUsers != null) {
                        for (String name : selectedUsers) {
                            boolean exists = false;
                            for (ChatModel member : members) {
                                if (member.getUserName().equals(name)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                members.add(new ChatModel(name, "Added", "Just now", 0, false, false));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        updateMemberCount();
                        Toast.makeText(this, selectedUsers.size() + " participants added", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        String groupName = getIntent().getStringExtra("group_name");
        if (groupName == null) groupName = "Secure Group";

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsingToolbar);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(groupName);
        }

        tvMembersCount = findViewById(R.id.tvMembersCount);
        tvGroupDescription = findViewById(R.id.group_description);
        tvDisappearingStatus = findViewById(R.id.tvDisappearingStatus);

        setupMembersList();
        setupAdditionalActions();
    }

    private void setupMembersList() {
        RecyclerView rvMembers = findViewById(R.id.rvMembers);
        members = new ArrayList<>();
        // Mock members
        members.add(new ChatModel("You (Admin)", "Active", "Now", 0, false, true));
        members.add(new ChatModel("Alice", "Encrypted session", "2m ago", 0, false, true));
        members.add(new ChatModel("Bob", "Offline", "1h ago", 0, false, false));
        members.add(new ChatModel("Charlie", "Away", "5m ago", 0, false, true));

        updateMemberCount();

        adapter = new SelectedParticipantAdapter(members, user -> {
            if (user.getUserName().contains("Admin")) {
                Toast.makeText(this, "You cannot remove yourself", Toast.LENGTH_SHORT).show();
            } else {
                members.remove(user);
                adapter.notifyDataSetChanged();
                updateMemberCount();
                Toast.makeText(this, user.getUserName() + " removed from group", Toast.LENGTH_SHORT).show();
            }
        });

        rvMembers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMembers.setAdapter(adapter);
    }

    private void updateMemberCount() {
        if (tvMembersCount != null) {
            String countText = "Members (" + members.size() + ")";
            tvMembersCount.setText(countText);
        }
    }

    private void setupAdditionalActions() {
        // Change Group Icon
        View ivGroupCover = findViewById(R.id.ivGroupCover);
        if (ivGroupCover != null) {
            ivGroupCover.setOnClickListener(v -> Toast.makeText(this, "Opening gallery to change group icon...", Toast.LENGTH_SHORT).show());
        }

        // Edit Description
        View cardDescription = findViewById(R.id.cardDescription);
        if (cardDescription != null) {
            cardDescription.setOnClickListener(v -> showEditDescriptionDialog());
        }

        // Media Gallery
        View cardMediaGallery = findViewById(R.id.cardMediaGallery);
        if (cardMediaGallery != null) {
            cardMediaGallery.setOnClickListener(v -> {
                Intent intent = new Intent(this, MediaGalleryActivity.class);
                intent.putExtra("user_name", "Group Media");
                startActivity(intent);
            });
        }

        // Mute Notifications
        View rlMuteNotifications = findViewById(R.id.rlMuteNotifications);
        SwitchMaterial switchMute = findViewById(R.id.switchMute);
        if (rlMuteNotifications != null && switchMute != null) {
            rlMuteNotifications.setOnClickListener(v -> {
                isMuted = !isMuted;
                switchMute.setChecked(isMuted);
                Toast.makeText(this, isMuted ? "Notifications muted" : "Notifications unmuted", Toast.LENGTH_SHORT).show();
            });
            switchMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isMuted = isChecked;
                Toast.makeText(this, isMuted ? "Notifications muted" : "Notifications unmuted", Toast.LENGTH_SHORT).show();
            });
        }

        // Encryption info
        View llEncryption = findViewById(R.id.llEncryption);
        if (llEncryption != null) {
            llEncryption.setOnClickListener(v -> Toast.makeText(this, "Verifying end-to-end encryption keys...", Toast.LENGTH_SHORT).show());
        }

        // Disappearing Messages
        View llDisappearingMessages = findViewById(R.id.llDisappearingMessages);
        if (llDisappearingMessages != null) {
            llDisappearingMessages.setOnClickListener(v -> {
                if (tvDisappearingStatus.getText().toString().equals("Off")) {
                    tvDisappearingStatus.setText("24 Hours");
                } else if (tvDisappearingStatus.getText().toString().equals("24 Hours")) {
                    tvDisappearingStatus.setText("7 Days");
                } else {
                    tvDisappearingStatus.setText("Off");
                }
                Toast.makeText(this, "Disappearing messages set to: " + tvDisappearingStatus.getText(), Toast.LENGTH_SHORT).show();
            });
        }

        // Add Member
        View btnAddMember = findViewById(R.id.btnAddMember);
        if (btnAddMember != null) {
            btnAddMember.setOnClickListener(v -> {
                Intent intent = new Intent(this, ContactPickerActivity.class);
                contactPickerLauncher.launch(intent);
            });
        }

        // Leave Group
        findViewById(R.id.btnLeaveGroup).setOnClickListener(v -> {
            Toast.makeText(this, "Leaving group...", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Report Group
        View btnReportGroup = findViewById(R.id.btnReportGroup);
        if (btnReportGroup != null) {
            btnReportGroup.setOnClickListener(v -> Toast.makeText(this, "Group reported for review", Toast.LENGTH_SHORT).show());
        }
    }

    private void showEditDescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Group Description");

        final EditText input = new EditText(this);
        input.setText(tvGroupDescription.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDescription = input.getText().toString();
            tvGroupDescription.setText(newDescription);
            Toast.makeText(this, "Description updated", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
