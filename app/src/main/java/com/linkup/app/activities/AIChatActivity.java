package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.MessageAdapter;
import com.linkup.app.models.MessageModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIChatActivity extends BaseActivity implements MessageAdapter.OnMessageLongClickListener {

    private RecyclerView rvAIChat;
    private MessageAdapter adapter;
    private List<MessageModel> messageList;
    private EditText etAIMessage;
    private View aiTypingIndicator, aiRootLayout, svAttachments;
    private TextView tvStatus;
    private ChipGroup cgShortcuts;
    private ImageView btnToggleAttachments;

    private ChatFutures chatContext;
    private GenerativeModelFutures modelFutures;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private SharedPreferences aiPrefs;

    private static final String PREF_SELECTED_BG = "ai_chat_bg_res";

    // Activity Result Launchers
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleMediaSelection(uri, "Image"));

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleMediaSelection(uri, "Video"));

    private final ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleMediaSelection(uri, "File"));

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleMediaSelection(uri, "Audio"));

    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    addMessage("[Captured Image]", true);
                    analyzeBitmap(bitmap);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        aiPrefs = getSharedPreferences("AISettings", Context.MODE_PRIVATE);

        initViews();
        initializeVertexAIChat();

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, this);
        rvAIChat.setLayoutManager(new LinearLayoutManager(this));
        rvAIChat.setAdapter(adapter);

        addMessage("Hello! I'm your specialized AI assistant. How can I help you today?", false);

        loadShortcuts();
        applySavedBackground();
    }

    private void initializeVertexAIChat() {
        GenerativeModel gm = new GenerativeModel(
                "gemini-flash-latest",
                "AIzaSyDlTwdktBFFQM1QKLR9_dXrHgCgZd-I2mI",
                null,
                null,
                new RequestOptions(),
                null,
                null,
                null);
        modelFutures = GenerativeModelFutures.from(gm);
        chatContext = modelFutures.startChat();

        if (tvStatus != null) {
            tvStatus.setText("Status: Secure Connection Active");
            tvStatus.setTextColor(getColor(R.color.primary));
        }
    }

    private void initViews() {
        aiRootLayout = findViewById(R.id.aiRootLayout);
        rvAIChat = findViewById(R.id.rvAIChat);
        etAIMessage = findViewById(R.id.etAIMessage);
        aiTypingIndicator = findViewById(R.id.aiTypingIndicator);
        tvStatus = findViewById(R.id.tvProtocolStatus);
        cgShortcuts = findViewById(R.id.cgShortcuts);
        svAttachments = findViewById(R.id.svAttachments);
        btnToggleAttachments = findViewById(R.id.btnToggleAttachments);

        if (tvStatus != null) {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Syncing Protocol...");
        }

        View settingsBtn = findViewById(R.id.btnAISettings);
        if (settingsBtn != null)
            settingsBtn.setVisibility(View.VISIBLE);
    }

    private void applySavedBackground() {
        int savedBg = aiPrefs.getInt(PREF_SELECTED_BG, R.drawable.my_background_9);
        if (aiRootLayout != null) {
            aiRootLayout.setBackgroundResource(savedBg);
        }
    }

    private void loadShortcuts() {
        if (cgShortcuts == null)
            return;
        cgShortcuts.removeAllViews();

        Set<String> selectedKeys = aiPrefs.getStringSet("ai_selected_shortcuts", new HashSet<>());
        View svShortcuts = findViewById(R.id.svShortcuts);
        if (selectedKeys.isEmpty()) {
            if (svShortcuts != null)
                svShortcuts.setVisibility(View.GONE);
            return;
        }

        if (svShortcuts != null)
            svShortcuts.setVisibility(View.VISIBLE);

        Map<String, String> shortcutNames = new HashMap<>();
        shortcutNames.put("ai_math", "Math Solver");
        shortcutNames.put("ai_legal", "Legal Help");
        shortcutNames.put("ai_health", "Wellness");
        shortcutNames.put("ai_code", "Code Expert");
        shortcutNames.put("ai_finance", "Finance");
        shortcutNames.put("ai_language", "Translator");
        shortcutNames.put("ai_science", "Science");
        shortcutNames.put("ai_creative", "Creative");
        shortcutNames.put("ai_history", "History");
        shortcutNames.put("ai_travel", "Travel");
        shortcutNames.put("ai_security", "Security");
        shortcutNames.put("ai_business", "Business");

        for (String key : selectedKeys) {
            String label = shortcutNames.getOrDefault(key, "Assistant");
            addShortcutChip(label, key);
        }
    }

    private void addShortcutChip(String label, String key) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setChipBackgroundColorResource(R.color.glass_white);
        chip.setTextColor(getColor(R.color.white));
        chip.setChipStrokeColorResource(R.color.primary);
        chip.setChipStrokeWidth(2f);

        chip.setOnClickListener(v -> {
            String prompt = "Acting as an expert in " + label + ", ";
            etAIMessage.setText(prompt);
            etAIMessage.setSelection(prompt.length());
        });

        cgShortcuts.addView(chip);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View settingsBtn = findViewById(R.id.btnAISettings);
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, AISettingsActivity.class);
                startActivity(intent);
            });
        }

        if (btnToggleAttachments != null && svAttachments != null) {
            btnToggleAttachments.setOnClickListener(v -> toggleAttachments());
        }

        findViewById(R.id.btnSendAI).setOnClickListener(v -> {
            String query = etAIMessage.getText().toString().trim();
            if (!query.isEmpty()) {
                addMessage(query, true);
                etAIMessage.setText("");
                sendMessageToAI(query);
            }
        });

        setupAttachmentListeners();
    }

    private void toggleAttachments() {
        if (svAttachments.getVisibility() == View.VISIBLE) {
            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(200);
            svAttachments.startAnimation(fadeOut);
            svAttachments.setVisibility(View.GONE);
            btnToggleAttachments.animate().rotation(0).setDuration(200).start();
        } else {
            svAttachments.setVisibility(View.VISIBLE);
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300);
            svAttachments.startAnimation(fadeIn);
            btnToggleAttachments.animate().rotation(45).setDuration(200).start();
        }
    }

    private void setupAttachmentListeners() {
        if (findViewById(R.id.btnAttachImage) != null)
            findViewById(R.id.btnAttachImage).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        if (findViewById(R.id.btnAttachVideo) != null)
            findViewById(R.id.btnAttachVideo).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        if (findViewById(R.id.btnAttachFile) != null)
            findViewById(R.id.btnAttachFile).setOnClickListener(v -> pickFileLauncher.launch("*/*"));

        if (findViewById(R.id.btnAttachCamera) != null)
            findViewById(R.id.btnAttachCamera).setOnClickListener(v -> takePictureLauncher.launch(null));

        if (findViewById(R.id.btnAttachAudio) != null)
            findViewById(R.id.btnAttachAudio).setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));
    }

    private void handleMediaSelection(Uri uri, String type) {
        if (uri != null) {
            Toast.makeText(this, type + " attached successfully. Analyzing...", Toast.LENGTH_SHORT).show();
            addMessage("[Attached " + type + "]", true);

            if ("Image".equals(type)) {
                analyzeImage(uri);
            } else {
                sendMessageToAI("I've attached a " + type + " (URI: " + uri.toString()
                        + "). Please analyze its content and provide insights.");
            }
        }
    }

    private void analyzeBitmap(Bitmap bitmap) {
        aiTypingIndicator.setVisibility(View.VISIBLE);
        Content userContent = new Content.Builder()
                .addImage(bitmap)
                .addText("Please analyze this captured image and describe what you see.")
                .build();

        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(userContent);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    aiTypingIndicator.setVisibility(View.GONE);
                    String text = result.getText();
                    if (text != null)
                        addMessage(text, false);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    aiTypingIndicator.setVisibility(View.GONE);
                    addMessage("Vision Analysis Error: " + t.getMessage(), false);
                });
            }
        }, executor);
    }

    private void analyzeImage(Uri uri) {
        aiTypingIndicator.setVisibility(View.VISIBLE);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            analyzeBitmap(bitmap);
        } catch (Exception e) {
            aiTypingIndicator.setVisibility(View.GONE);
            addMessage("Error processing image: " + e.getMessage(), false);
        }
    }

    private void sendMessageToAI(String query) {
        aiTypingIndicator.setVisibility(View.VISIBLE);

        Content userContent = new Content.Builder()
                .addText(query)
                .build();

        ListenableFuture<GenerateContentResponse> response = chatContext.sendMessage(userContent);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    aiTypingIndicator.setVisibility(View.GONE);
                    String text = result.getText();
                    if (text != null) {
                        addMessage(text, false);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    aiTypingIndicator.setVisibility(View.GONE);
                    addMessage("Communication Error: " + t.getMessage(), false);
                });
            }
        }, executor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupListeners();
        loadShortcuts();
    }

    private void addMessage(String text, boolean isSent) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messageList.add(new MessageModel(text, time, isSent, MessageModel.MessageType.TEXT, null,
                isSent ? "Me" : "AI Specialist"));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvAIChat.smoothScrollToPosition(messageList.size() - 1);
    }

    @Override
    public void onMessageLongClick(MessageModel message, View itemView) {
    }

    @Override
    public void onSelectionChanged() {
    }

    @Override
    public void onReplySwiped(MessageModel message) {
    }

    @Override
    public void onReplyPreviewClick(MessageModel message) {
    }

    @Override
    public void onMediaClick(MessageModel message) {
    }
}
