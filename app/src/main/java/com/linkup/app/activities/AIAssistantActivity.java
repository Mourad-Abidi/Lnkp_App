package com.linkup.app.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.linkup.app.R;
import com.linkup.app.models.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIAssistantActivity extends BaseActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private List<Message> messageList;
    private TextInputEditText etMessage;
    private View btnSend;
    private ImageView ivAISettings;
    private TextView tvTypingIndicator;
    private ChipGroup cgShortcuts;
    private View svShortcuts;
    private ChatFutures chatHistory;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private SharedPreferences aiPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        aiPrefs = getSharedPreferences("AISettings", Context.MODE_PRIVATE);

        initViews();
        setupChat();
        setupAI();
        setupListeners();
        loadShortcuts();
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        ivAISettings = findViewById(R.id.ivAISettings);
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator);
        cgShortcuts = findViewById(R.id.cgShortcuts);
        svShortcuts = findViewById(R.id.svShortcuts);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadShortcuts() {
        if (cgShortcuts == null) return;
        cgShortcuts.removeAllViews();
        
        Set<String> selectedKeys = aiPrefs.getStringSet("ai_selected_shortcuts", new HashSet<>());
        if (selectedKeys.isEmpty()) {
            if (svShortcuts != null) svShortcuts.setVisibility(View.GONE);
            return;
        }
        
        if (svShortcuts != null) svShortcuts.setVisibility(View.VISIBLE);
        
        Map<String, String> shortcutNames = new HashMap<>();
        shortcutNames.put("ai_math", "Math & Logic");
        shortcutNames.put("ai_legal", "Legal Advice");
        shortcutNames.put("ai_health", "Health Help");
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
            String label = shortcutNames.getOrDefault(key, "Specialist");
            addShortcutChip(label, key);
        }
    }

    private void addShortcutChip(String label, String key) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setChipBackgroundColorResource(R.color.glass_white);
        chip.setTextColor(getColor(R.color.white));
        chip.setChipStrokeColorResource(R.color.primary);
        chip.setChipStrokeWidth(3f);
        
        chip.setOnClickListener(v -> {
            String prompt = "As an expert in " + label + ", ";
            etMessage.setText(prompt);
            etMessage.setSelection(prompt.length());
        });
        
        cgShortcuts.addView(chip);
    }

    private void setupChat() {
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
    }

    private void setupAI() {
        GenerativeModel gm = new GenerativeModel(
                "gemini-flash-latest",
                "AIzaSyDlTwdktBFFQM1QKLR9_dXrHgCgZd-I2mI",
                null,
                null,
                new RequestOptions(),
                null,
                null,
                null
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        chatHistory = model.startChat();
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        
        if (ivAISettings != null) {
            ivAISettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, AISettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void sendMessage() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Internet connection required for AI assistance.", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Message userMsg = new Message();
        userMsg.setMessageText(text);
        userMsg.setMessageType("USER");
        userMsg.setTimestamp(System.currentTimeMillis());
        addMessage(userMsg);
        
        etMessage.setText("");
        showTyping(true);

        Content content = new Content.Builder().addText(text).build();
        ListenableFuture<GenerateContentResponse> response = chatHistory.sendMessage(content);
        
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    showTyping(false);
                    String aiText = result.getText();
                    Message aiMsg = new Message();
                    aiMsg.setMessageText(aiText != null ? aiText : "I'm having trouble understanding that. Could you rephrase?");
                    aiMsg.setMessageType("AI");
                    aiMsg.setTimestamp(System.currentTimeMillis());
                    addMessage(aiMsg);
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    showTyping(false);
                    Toast.makeText(AIAssistantActivity.this, "AI is currently unavailable. Please check your connection.", Toast.LENGTH_SHORT).show();
                });
            }
        }, executor);
    }

    private void addMessage(Message msg) {
        messageList.add(msg);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.smoothScrollToPosition(messageList.size() - 1);
    }

    private void showTyping(boolean show) {
        tvTypingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ai_assistant, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_chat) {
            messageList.clear();
            adapter.notifyDataSetChanged();
            setupAI();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadShortcuts();
    }

    private class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_USER = 1;
        private static final int TYPE_AI = 2;
        private List<Message> messages;

        ChatAdapter(List<Message> messages) { this.messages = messages; }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).getMessageType().equals("USER") ? TYPE_USER : TYPE_AI;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            if (viewType == TYPE_USER) {
                return new UserViewHolder(getLayoutInflater().inflate(R.layout.item_message_user, parent, false));
            }
            return new AIViewHolder(getLayoutInflater().inflate(R.layout.item_message_ai, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message msg = messages.get(position);
            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).bind(msg);
            } else {
                ((AIViewHolder) holder).bind(msg);
            }
        }

        @Override
        public int getItemCount() { return messages.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvTimestamp;
            UserViewHolder(View v) {
                super(v);
                tvMessage = v.findViewById(R.id.tvMessage);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                v.setOnLongClickListener(v1 -> copyMessage(tvMessage.getText().toString()));
            }
            void bind(Message m) { 
                tvMessage.setText(m.getMessageText());
                tvTimestamp.setText(android.text.format.DateFormat.format("hh:mm a", m.getTimestamp()));
            }
        }

        class AIViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvTimestamp;
            AIViewHolder(View v) {
                super(v);
                tvMessage = v.findViewById(R.id.tvMessage);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                v.setOnLongClickListener(v1 -> copyMessage(tvMessage.getText().toString()));
            }
            void bind(Message m) { 
                tvMessage.setText(m.getMessageText());
                tvTimestamp.setText(android.text.format.DateFormat.format("hh:mm a", m.getTimestamp()));
            }
        }

        private boolean copyMessage(String text) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI Message", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(AIAssistantActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}
