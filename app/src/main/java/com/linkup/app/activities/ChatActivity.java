package com.linkup.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.MessageAdapter;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.AppDatabase;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.Message;
import com.linkup.app.models.MessageModel;
import com.linkup.app.models.User;
import com.linkup.app.network.SupabaseRealtimeManager;
import com.bumptech.glide.Glide;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<MessageModel> messageList;
    private List<MessageModel> fullMessageList;
    
    private EditText etMessage;
    private String chatPartnerName = "Partner";
    private String chatPartnerId;
    private String chatPartnerAvatar;
    private View cvAttachmentOptions;
    
    private long lastFetchedTimestamp = 0;
    private SupabaseRealtimeManager.MessageListener realtimeListener;

    // Launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;

    // Vocal Recording
    private MediaRecorder mediaRecorder;
    private String audioPath;
    private boolean isRecording = false;
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private int recordTimeSeconds = 0;
    private Runnable recordRunnable;

    private final MessageAdapter.OnMessageLongClickListener messageListener = new MessageAdapter.OnMessageLongClickListener() {
        @Override
        public void onMessageLongClick(MessageModel message, View itemView) { }
        @Override
        public void onMediaClick(MessageModel message) {
            if (message.getMediaUrl() != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMediaUrl())));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initData();
        initUI();
        initLaunchers();
        setupListeners();
        
        loadPartnerProfile();
        loadMessages();
        setupRealtime();
    }

    private void initData() {
        chatPartnerName = getIntent().getStringExtra("user_name");
        chatPartnerId = getIntent().getStringExtra("user_id");
        if (chatPartnerName == null) chatPartnerName = "Unknown";
        messageList = new ArrayList<>();
        fullMessageList = new ArrayList<>();
    }

    private void initUI() {
        TextView tvUserNameToolbar = findViewById(R.id.tvUserNameToolbar);
        if (tvUserNameToolbar != null) tvUserNameToolbar.setText(chatPartnerName);

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        cvAttachmentOptions = findViewById(R.id.cvAttachmentOptions);

        adapter = new MessageAdapter(messageList, messageListener);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void initLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) uploadAndSendMedia(uri);
            }
        });

        fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) uploadAndSendFile(uri);
            }
        });
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> {
            if (isRecording) stopAndUploadRecording();
            else sendMessage();
        });
        
        findViewById(R.id.btnAdd).setOnClickListener(v -> toggleAttachmentPanel());
        findViewById(R.id.btnGallery).setOnClickListener(v -> { toggleAttachmentPanel(); pickMedia(); });
        findViewById(R.id.btnFile).setOnClickListener(v -> { toggleAttachmentPanel(); pickFile(); });
        findViewById(R.id.btnVocal).setOnClickListener(v -> { toggleAttachmentPanel(); toggleVoiceRecording(); });
        findViewById(R.id.btnTheme).setOnClickListener(v -> { toggleAttachmentPanel(); showThemeSelector(); });

        View btnMore = findViewById(R.id.btnMore);
        if (btnMore != null) btnMore.setOnClickListener(v -> showChatOptions(btnMore));
    }

    // --- PERMISSIONS ---
    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] perms = {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
            if (ContextCompat.checkSelfPermission(this, perms[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, perms, 100);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return false;
            }
        }
        return true;
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 100) pickMedia();
            else if (requestCode == 200) toggleVoiceRecording();
        }
    }

    // --- MEDIA HANDLING ---
    private void pickMedia() {
        if (!checkStoragePermissions()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/* video/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        galleryLauncher.launch(intent);
    }

    private void pickFile() {
        if (!checkStoragePermissions()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        fileLauncher.launch(intent);
    }

    private void uploadAndSendMedia(Uri uri) {
        Toast.makeText(this, "Uploading media...", Toast.LENGTH_SHORT).show();
        String mimeType = getContentResolver().getType(uri);
        MessageModel.MessageType type = (mimeType != null && mimeType.startsWith("video")) 
                ? MessageModel.MessageType.VIDEO : MessageModel.MessageType.IMAGE;

        FirebaseDatabaseManager.getInstance().uploadLargeFile(uri, new FirebaseDatabaseManager.OnImageUploadListener() {
            @Override
            public void onSuccess(String imageUrl) {
                sendAttachmentMessage(imageUrl, type, null, null);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAndSendFile(Uri uri) {
        Toast.makeText(this, "Uploading file...", Toast.LENGTH_SHORT).show();
        String fileName = getFileName(uri);
        FirebaseDatabaseManager.getInstance().uploadLargeFile(uri, new FirebaseDatabaseManager.OnImageUploadListener() {
            @Override
            public void onSuccess(String fileUrl) {
                sendAttachmentMessage(fileUrl, MessageModel.MessageType.FILE, fileName, "File");
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "File upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleVoiceRecording() {
        if (!isRecording) {
            if (checkAudioPermission()) startRecording();
        } else {
            stopAndUploadRecording();
        }
    }

    private void startRecording() {
        try {
            audioPath = getExternalCacheDir().getAbsolutePath() + "/vocal_" + System.currentTimeMillis() + ".m4a";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            showRecordingUI(true);
            startRecordingTimer();
        } catch (Exception e) {
            Log.e(TAG, "Recording failed", e);
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAndUploadRecording() {
        if (!isRecording) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        } catch (Exception ignored) {}
        
        isRecording = false;
        showRecordingUI(false);
        stopRecordingTimer();

        final String duration = String.format(Locale.getDefault(), "%d:%02d", recordTimeSeconds / 60, recordTimeSeconds % 60);
        Uri uri = Uri.fromFile(new File(audioPath));
        Toast.makeText(this, "Sending vocal...", Toast.LENGTH_SHORT).show();
        
        FirebaseDatabaseManager.getInstance().uploadLargeFile(uri, new FirebaseDatabaseManager.OnImageUploadListener() {
            @Override
            public void onSuccess(String audioUrl) {
                sendAttachmentMessage(audioUrl, MessageModel.MessageType.VOICE, "Vocal Message", duration);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "Voice upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAttachmentMessage(String url, MessageModel.MessageType type, String fileName, String sizeOrDuration) {
        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        String msgId = UUID.randomUUID().toString();

        MessageModel local = new MessageModel(fileName != null ? fileName : "Sent an attachment", time, true, type, sizeOrDuration, chatPartnerName);
        local.setCloudId(msgId);
        local.setChatPartnerId(chatPartnerId);
        local.setTimestamp(timestamp);
        local.setMediaUrl(url);
        saveAndDisplayMessage(local);

        Message cloudMsg = new Message();
        cloudMsg.setMessageId(msgId);
        cloudMsg.setSenderId(FirebaseDatabaseManager.getInstance().getCurrentUserId());
        cloudMsg.setReceiverId(chatPartnerId);
        cloudMsg.setMessageText(fileName);
        cloudMsg.setMediaUrl(url);
        cloudMsg.setTimestamp(timestamp);
        cloudMsg.setMessageType(type.name());
        cloudMsg.setFileSize(sizeOrDuration);
        FirebaseDatabaseManager.getInstance().sendMessage(cloudMsg);
    }

    // --- HELPERS ---
    private void showThemeSelector() {
        String[] themes = {"Light", "Dark", "System Default"};
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setItems(themes, (dialog, which) -> {
                    int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    if (which == 0) mode = AppCompatDelegate.MODE_NIGHT_NO;
                    else if (which == 1) mode = AppCompatDelegate.MODE_NIGHT_YES;
                    settingsPrefs.edit().putInt("app_theme", mode).apply();
                    AppCompatDelegate.setDefaultNightMode(mode);
                    recreate();
                }).show();
    }

    private void showRecordingUI(boolean show) {
        findViewById(R.id.llInputGroup).setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.llRecordingUI).setVisibility(show ? View.VISIBLE : View.GONE);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton)findViewById(R.id.btnSend))
                .setImageResource(show ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_menu_send);
    }

    private void startRecordingTimer() {
        recordTimeSeconds = 0;
        recordRunnable = new Runnable() {
            @Override
            public void run() {
                recordTimeSeconds++;
                int mins = recordTimeSeconds / 60;
                int secs = recordTimeSeconds % 60;
                ((TextView)findViewById(R.id.tvRecordingTime)).setText(String.format(Locale.getDefault(), "%d:%02d", mins, secs));
                recordHandler.postDelayed(this, 1000);
            }
        };
        recordHandler.postDelayed(recordRunnable, 1000);
    }

    private void stopRecordingTimer() {
        if (recordRunnable != null) recordHandler.removeCallbacks(recordRunnable);
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void toggleAttachmentPanel() {
        if (cvAttachmentOptions != null) {
            cvAttachmentOptions.setVisibility(cvAttachmentOptions.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");

        long timestamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        String msgId = UUID.randomUUID().toString();

        MessageModel local = new MessageModel(text, time, true, MessageModel.MessageType.TEXT, null, chatPartnerName);
        local.setCloudId(msgId);
        local.setChatPartnerId(chatPartnerId);
        local.setTimestamp(timestamp);
        saveAndDisplayMessage(local);

        Message cloudMsg = new Message();
        cloudMsg.setMessageId(msgId);
        cloudMsg.setSenderId(FirebaseDatabaseManager.getInstance().getCurrentUserId());
        cloudMsg.setReceiverId(chatPartnerId);
        cloudMsg.setMessageText(text);
        cloudMsg.setTimestamp(timestamp);
        cloudMsg.setMessageType("TEXT");
        cloudMsg.setReadStatus("SENT");
        FirebaseDatabaseManager.getInstance().sendMessage(cloudMsg);
    }

    private void saveAndDisplayMessage(MessageModel message) {
        messageList.add(message);
        fullMessageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
        updateMainChatList(message);
        AsyncTask.execute(() -> AppDatabase.getInstance(this).messageDao().insert(message));
    }

    private void loadPartnerProfile() {
        if (TextUtils.isEmpty(chatPartnerId)) return;
        FirebaseDatabaseManager.getInstance().fetchUserById(chatPartnerId, users -> {
            if (users != null && !users.isEmpty()) {
                User user = users.get(0);
                chatPartnerAvatar = user.getProfilePhoto();
                chatPartnerName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                runOnUiThread(() -> {
                    TextView tvUserNameToolbar = findViewById(R.id.tvUserNameToolbar);
                    if (tvUserNameToolbar != null) tvUserNameToolbar.setText(chatPartnerName);
                    ImageView ivAvatar = findViewById(R.id.ivUserAvatarToolbar);
                    if (ivAvatar != null && chatPartnerAvatar != null) {
                        Glide.with(this).load(chatPartnerAvatar).circleCrop().placeholder(R.drawable.app_logo).into(ivAvatar);
                    }
                    if (adapter != null) adapter.setPartnerAvatarUri(chatPartnerAvatar);
                });
            }
        });
    }

    private void loadMessages() {
        if (TextUtils.isEmpty(chatPartnerId)) return;
        AsyncTask.execute(() -> {
            List<MessageModel> list = AppDatabase.getInstance(this).messageDao().getMessagesForChat(chatPartnerId);
            runOnUiThread(() -> {
                fullMessageList.clear(); fullMessageList.addAll(list);
                messageList.clear(); messageList.addAll(list);
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    rvMessages.scrollToPosition(messageList.size() - 1);
                    for (MessageModel m : list) if (m.getTimestamp() > lastFetchedTimestamp) lastFetchedTimestamp = m.getTimestamp();
                }
                fetchMissedMessages();
            });
        });
    }

    private void fetchMissedMessages() {
        FirebaseDatabaseManager.getInstance().fetchMessagesSince(chatPartnerId, lastFetchedTimestamp, messages -> {
            if (messages != null && !messages.isEmpty()) syncWithCloud(messages);
        });
    }

    private void syncWithCloud(List<Message> cloudMessages) {
        String currentUserId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        boolean hasNew = false;
        Set<String> ids = new HashSet<>();
        for (MessageModel m : fullMessageList) if (m.getCloudId() != null) ids.add(m.getCloudId());

        for (Message msg : cloudMessages) {
            if (msg.getMessageId() != null && ids.contains(msg.getMessageId())) continue;
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(msg.getTimestamp()));
            MessageModel.MessageType type = MessageModel.MessageType.TEXT;
            try { if (msg.getMessageType() != null) type = MessageModel.MessageType.valueOf(msg.getMessageType()); } catch (Exception ignored) {}

            MessageModel model = new MessageModel(msg.getMessageText(), time, msg.getSenderId().equals(currentUserId), type, msg.getFileSize(), chatPartnerName);
            model.setCloudId(msg.getMessageId()); model.setChatPartnerId(chatPartnerId);
            model.setTimestamp(msg.getTimestamp()); model.setMediaUrl(msg.getMediaUrl());
            if (msg.getTimestamp() > lastFetchedTimestamp) lastFetchedTimestamp = msg.getTimestamp();
            messageList.add(model); fullMessageList.add(model);
            AsyncTask.execute(() -> AppDatabase.getInstance(this).messageDao().insert(model));
            hasNew = true;
        }
        if (hasNew) runOnUiThread(() -> { adapter.notifyDataSetChanged(); rvMessages.scrollToPosition(messageList.size() - 1); });
    }

    private void updateMainChatList(MessageModel lastMessage) {
        ChatModel chat = new ChatModel(chatPartnerName, lastMessage.getMessage(), lastMessage.getTime(), 0, false);
        chat.setUserId(chatPartnerId); chat.setLastMessageTimestamp(lastMessage.getTimestamp());
        chat.setProfilePhoto(chatPartnerAvatar); SharedDataManager.getInstance().addGroup(chat);
    }

    private void setupRealtime() {
        realtimeListener = message -> {
            if (message.getSenderId() != null && message.getSenderId().equals(chatPartnerId)) {
                handleIncomingCloudMessage(message);
            }
        };
    }

    private void handleIncomingCloudMessage(Message msg) {
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(msg.getTimestamp()));
        MessageModel.MessageType type = MessageModel.MessageType.TEXT;
        try { if (msg.getMessageType() != null) type = MessageModel.MessageType.valueOf(msg.getMessageType()); } catch (Exception ignored) {}

        MessageModel model = new MessageModel(msg.getMessageText(), timeStr, false, type, msg.getFileSize(), chatPartnerName);
        model.setCloudId(msg.getMessageId()); model.setChatPartnerId(chatPartnerId);
        model.setTimestamp(msg.getTimestamp()); model.setMediaUrl(msg.getMediaUrl());

        runOnUiThread(() -> {
            for (MessageModel m : fullMessageList) if (msg.getMessageId() != null && msg.getMessageId().equals(m.getCloudId())) return;
            messageList.add(model); fullMessageList.add(model);
            adapter.notifyItemInserted(messageList.size() - 1); rvMessages.scrollToPosition(messageList.size() - 1);
            updateMainChatList(model);
            AsyncTask.execute(() -> AppDatabase.getInstance(this).messageDao().insert(model));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedDataManager.getInstance().setActiveChatUserId(chatPartnerId);
        String uid = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        if (!"unknown".equals(uid)) SupabaseRealtimeManager.getInstance().startListening(uid, realtimeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedDataManager.getInstance().setActiveChatUserId(null);
        SupabaseRealtimeManager.getInstance().stopListening(realtimeListener);
        if (isRecording) stopAndUploadRecording();
    }

    private void showChatOptions(View anchor) {
        View menuView = LayoutInflater.from(this).inflate(R.layout.menu_chat_options, null);
        PopupWindow popupWindow = new PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        menuView.findViewById(R.id.menuViewProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class).putExtra("user_name", chatPartnerName).putExtra("user_id", chatPartnerId));
            popupWindow.dismiss();
        });
        popupWindow.setElevation(20);
        popupWindow.showAsDropDown(anchor, 0, 0);
    }
}
