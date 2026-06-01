package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.SelectedUserAdapter;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.ChatModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends BaseActivity {

    private TextInputEditText etGroupName, etGroupDescription;
    private ImageView ivGroupAvatar;
    private MaterialButton btnCreateGroup, btnAddNode;
    private SwitchMaterial swGhostMode, swEndToEnd;
    private RecyclerView rvSelectedParticipants;
    private CircularProgressIndicator progressCreate;

    private Uri selectedAvatarUri;
    private List<ChatModel> selectedUsers = new ArrayList<>();
    private SelectedUserAdapter participantsAdapter;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    ivGroupAvatar.setImageURI(uri);
                }
            });

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> pickedNames = result.getData().getStringArrayListExtra("selected_users");
                    if (pickedNames != null) {
                        for (String name : pickedNames) {
                            if (!isAlreadySelected(name)) {
                                selectedUsers.add(new ChatModel(name, "", "", 0, false));
                            }
                        }
                        updateParticipantsUI();
                    }
                }
            });

    private boolean isAlreadySelected(String name) {
        for (ChatModel user : selectedUsers) {
            if (user.getUserName().equals(name)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etGroupName = findViewById(R.id.etGroupName);
        etGroupDescription = findViewById(R.id.etGroupDescription);
        ivGroupAvatar = findViewById(R.id.ivGroupAvatar);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnAddNode = findViewById(R.id.btnAddNode);
        swGhostMode = findViewById(R.id.swGhostMode);
        swEndToEnd = findViewById(R.id.swEndToEnd);
        rvSelectedParticipants = findViewById(R.id.rvSelectedParticipants);
        progressCreate = findViewById(R.id.progressCreate);

        participantsAdapter = new SelectedUserAdapter(selectedUsers, user -> {
            selectedUsers.remove(user);
            updateParticipantsUI();
        });
        rvSelectedParticipants.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedParticipants.setAdapter(participantsAdapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Initialize New Group");
        }
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupListeners() {
        findViewById(R.id.fabChangeAvatar).setOnClickListener(v ->
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build())
        );

        btnAddNode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ContactPickerActivity.class);
            contactPickerLauncher.launch(intent);
        });

        btnCreateGroup.setOnClickListener(v -> validateAndCreateGroup());
    }

    private void validateAndCreateGroup() {
        String name = etGroupName.getText().toString().trim();
        String description = etGroupDescription.getText().toString().trim();
        
        if (name.length() < 3) {
            etGroupName.setError("Group name too short (min 3 chars)");
            return;
        }

        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "Select at least one participant", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Prepare participants list for Firebase
        List<String> participantNames = new ArrayList<>();
        for (ChatModel user : selectedUsers) {
            participantNames.add(user.getUserName());
        }

        // Create group in Firebase (Cloud Sync)
        FirebaseDatabaseManager.getInstance().createGroup(name, description, participantNames);

        new android.os.Handler().postDelayed(() -> {
            showLoading(false);

            // Local update for immediate UI feedback
            ChatModel newGroup = new ChatModel(name, "Group created", "Just now", 0, false);
            newGroup.setGroup(true);
            SharedDataManager.getInstance().addGroup(newGroup);

            Toast.makeText(this, "Group Protocol Initialized: " + name, Toast.LENGTH_LONG).show();
            finish();
        }, 1500);
    }

    private void updateParticipantsUI() {
        participantsAdapter.notifyDataSetChanged();
        btnAddNode.setText("NODES: " + selectedUsers.size());
        rvSelectedParticipants.setVisibility(selectedUsers.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean isLoading) {
        btnCreateGroup.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        progressCreate.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateGroup.setEnabled(!isLoading);
        btnAddNode.setEnabled(!isLoading);
    }
}
