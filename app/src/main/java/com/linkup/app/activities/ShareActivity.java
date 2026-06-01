package com.linkup.app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.linkup.app.R;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.Post;

import java.util.Locale;
import java.util.UUID;

public class ShareActivity extends BaseActivity {

    private EditText etShareContent;
    private ImageView ivSelectedMedia;
    private View flMediaContainer;
    private TextView tvCharCount;
    private ChipGroup chipGroupDuration;
    private View loadingOverlay;
    private Uri selectedImageUri = null;
    private static final int MAX_CHARS = 5000;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (flMediaContainer != null) flMediaContainer.setVisibility(View.VISIBLE);
                    if (ivSelectedMedia != null) Glide.with(this).load(uri).into(ivSelectedMedia);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etShareContent = findViewById(R.id.etShareContent);
        ivSelectedMedia = findViewById(R.id.ivSelectedMedia);
        flMediaContainer = findViewById(R.id.flMediaContainer);
        tvCharCount = findViewById(R.id.tvCharCount);
        chipGroupDuration = findViewById(R.id.chipGroupDuration);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        if (etShareContent != null) {
            etShareContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCharCount != null) {
                        tvCharCount.setText(String.format(Locale.getDefault(), "%d/%d", s.length(), MAX_CHARS));
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > MAX_CHARS) {
                        etShareContent.setText(s.subSequence(0, MAX_CHARS));
                        etShareContent.setSelection(MAX_CHARS);
                    }
                }
            });
        }

        View btnSelectImage = findViewById(R.id.btnSelectImage);
        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));
        }

        View btnRemoveMedia = findViewById(R.id.btnRemoveMedia);
        if (btnRemoveMedia != null) {
            btnRemoveMedia.setOnClickListener(v -> {
                if (flMediaContainer != null) flMediaContainer.setVisibility(View.GONE);
                selectedImageUri = null;
            });
        }

        View btnPost = findViewById(R.id.btnPost);
        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                String content = etShareContent != null ? etShareContent.getText().toString().trim() : "";
                if (content.isEmpty() && selectedImageUri == null) {
                    Toast.makeText(this, R.string.please_enter_content, Toast.LENGTH_SHORT).show();
                    return;
                }

                showLoading(true);
                if (selectedImageUri != null) {
                    uploadImageAndPost(content);
                } else {
                    createAndUploadPost(content, null);
                }
            });
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        View btnPost = findViewById(R.id.btnPost);
        if (btnPost != null) btnPost.setEnabled(!show);
    }

    private void uploadImageAndPost(String content) {
        FirebaseDatabaseManager.getInstance().uploadLargeFile(selectedImageUri, new FirebaseDatabaseManager.OnImageUploadListener() {
            @Override
            public void onSuccess(String imageUrl) {
                createAndUploadPost(content, imageUrl);
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(ShareActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAndUploadPost(String content, String imageUrl) {
        long expiryTimestamp = calculateExpiry();
        String postId = UUID.randomUUID().toString();
        String myName = appPrefs.getString("user_full_name", "LinkUp User");
        String myId = FirebaseDatabaseManager.getInstance().getCurrentUserId();

        Post newPost = new Post(postId, myName, myId, content, imageUrl, expiryTimestamp);
        FirebaseDatabaseManager.getInstance().uploadPost(newPost);

        showLoading(false);
        Toast.makeText(this, R.string.posted_successfully, Toast.LENGTH_SHORT).show();
        finish();
    }

    private long calculateExpiry() {
        long durationMillis = 24 * 60 * 60 * 1000L; // Default 1 day
        if (chipGroupDuration == null) return System.currentTimeMillis() + durationMillis;

        int checkedId = chipGroupDuration.getCheckedChipId();
        if (checkedId == R.id.chip3Days) {
            durationMillis = 3 * 24 * 60 * 60 * 1000L;
        } else if (checkedId == R.id.chip1Week) {
            durationMillis = 7 * 24 * 60 * 60 * 1000L;
        }
        
        return System.currentTimeMillis() + durationMillis;
    }
}
