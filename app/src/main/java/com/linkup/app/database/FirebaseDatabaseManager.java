package com.linkup.app.database;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.linkup.app.core.SharedDataManager;
import com.linkup.app.core.AppExecutors;
import com.linkup.app.models.ChatModel;
import com.linkup.app.models.Message;
import com.linkup.app.models.MessageModel;
import com.linkup.app.models.Post;
import com.linkup.app.models.User;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.SupabaseClient;
import com.linkup.app.network.SupabaseConfig;
import com.linkup.app.security.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirebaseDatabaseManager {
    private static final String TAG = "FirebaseDBManager";
    private static FirebaseDatabaseManager instance;
    private static Context appContext;
    
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    public interface OnMessagesFetchedListener {
        void onMessagesFetched(List<Message> messages);
    }

    public interface OnUsersFetchedListener {
        void onUsersFetched(List<User> users);
    }

    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    public static synchronized FirebaseDatabaseManager getInstance() {
        if (instance == null) instance = new FirebaseDatabaseManager();
        return instance;
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Context getContext() {
        return appContext;
    }

    public String getCurrentUserId() {
        if (appContext == null) return "unknown";
        return appContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("user_id", "unknown");
    }

    private boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.isEmpty() || "unknown".equals(uuid) || uuid.contains("<")) return false;
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void syncConversations() {
        syncConversations(null);
    }

    public void syncConversations(Runnable onComplete) {
        String currentId = getCurrentUserId();
        if (!isValidUuid(currentId)) {
            Log.w(TAG, "Sync skipped: Invalid current user ID: " + currentId);
            if (onComplete != null) onComplete.run();
            return;
        }

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        String orFilter = "(sender_id.eq." + currentId + ",receiver_id.eq." + currentId + ")";
        
        api.getMessagesByFilter(orFilter, "*", "timestamp.desc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Message> uniqueChats = new HashMap<>();
                    for (Message msg : response.body()) {
                        String partnerId = msg.getSenderId().equals(currentId) ? msg.getReceiverId() : msg.getSenderId();
                        if (partnerId != null && !uniqueChats.containsKey(partnerId)) {
                            uniqueChats.put(partnerId, msg);
                        }
                    }
                    
                    for (Map.Entry<String, Message> entry : uniqueChats.entrySet()) {
                        processMessageForMainList(entry.getValue(), entry.getKey());
                    }
                } else {
                    Log.e(TAG, "Sync Conversations Failed: " + response.code() + " " + response.message());
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e(TAG, "Sync failed (Network/Timeout)", t);
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public void fetchAndStoreAllMessages(Runnable onComplete) {
        String currentId = getCurrentUserId();
        if (!isValidUuid(currentId)) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        String orFilter = "(sender_id.eq." + currentId + ",receiver_id.eq." + currentId + ")";

        api.getMessagesByFilter(orFilter, "*", "timestamp.asc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        MessageDao dao = AppDatabase.getInstance(appContext).messageDao();
                        for (Message msg : response.body()) {
                            if (!dao.exists(msg.getMessageId())) {
                                String timeStr = formatMessageTime(msg.getTimestamp());
                                MessageModel.MessageType type = MessageModel.MessageType.TEXT;
                                try {
                                    if (msg.getMessageType() != null) type = MessageModel.MessageType.valueOf(msg.getMessageType());
                                } catch (Exception ignored) {}

                                String partnerId = msg.getSenderId().equals(currentId) ? msg.getReceiverId() : msg.getSenderId();
                                MessageModel model = new MessageModel(
                                        SecurityUtils.decrypt(msg.getMessageText()),
                                        timeStr,
                                        msg.getSenderId().equals(currentId),
                                        type,
                                        msg.getFileSize(),
                                        "Partner" // Placeholder, will be updated by syncConversations
                                );
                                model.setCloudId(msg.getMessageId());
                                model.setChatPartnerId(partnerId);
                                model.setTimestamp(msg.getTimestamp());
                                model.setMediaUrl(SecurityUtils.decrypt(msg.getMediaUrl()));
                                model.setSeen("READ".equals(msg.getReadStatus()));
                                dao.insert(model);
                            }
                        }
                        if (onComplete != null) AppExecutors.getInstance().mainThread().execute(onComplete);
                    });
                } else {
                    if (onComplete != null) onComplete.run();
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void processMessageForMainList(Message msg, String partnerId) {
        fetchUserById(partnerId, users -> {
            if (users != null && !users.isEmpty()) {
                User partner = users.get(0);
                String tempName = partner.getFullName() != null && !partner.getFullName().isEmpty() 
                             ? partner.getFullName() : partner.getUsername();
                if (tempName == null || tempName.isEmpty()) tempName = "Unknown";
                final String finalName = tempName;

                AppExecutors.getInstance().diskIO().execute(() -> {
                    int unreadCount = AppDatabase.getInstance(appContext).messageDao().getUnreadCount(partnerId);
                    
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        ChatModel chat = new ChatModel(finalName, SecurityUtils.decrypt(msg.getMessageText()), formatMessageTime(msg.getTimestamp()), unreadCount, unreadCount == 0);
                        chat.setUserId(partnerId);
                        chat.setProfilePhoto(partner.getProfilePhoto());
                        chat.setLastMessageTimestamp(msg.getTimestamp());
                        chat.setUnreadCount(unreadCount);
                        SharedDataManager.getInstance().addGroup(chat, false);
                    });
                });
            }
        });
    }

    public String formatMessageTime(long timestamp) {
        if (timestamp <= 0) return "Now";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 86400000) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        } else if (diff < 604800000) {
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(timestamp));
        } else {
            return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date(timestamp));
        }
    }

    public void fetchMessagesSince(String partnerId, long sinceTimestamp, OnMessagesFetchedListener listener) {
        String currentId = getCurrentUserId();
        if (!isValidUuid(currentId) || !isValidUuid(partnerId)) {
            Log.w(TAG, "Fetch skipped: Invalid IDs - Me: " + currentId + ", Partner: " + partnerId);
            listener.onMessagesFetched(new ArrayList<>());
            return;
        }

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        
        String filter;
        if (sinceTimestamp > 0) {
            filter = "(and(sender_id.eq." + currentId + ",receiver_id.eq." + partnerId + ",timestamp.gt." + sinceTimestamp + ")," +
                     "and(sender_id.eq." + partnerId + ",receiver_id.eq." + currentId + ",timestamp.gt." + sinceTimestamp + "))";
        } else {
            filter = "(and(sender_id.eq." + currentId + ",receiver_id.eq." + partnerId + ")," +
                     "and(sender_id.eq." + partnerId + ",receiver_id.eq." + currentId + "))";
        }

        api.getMessagesByFilter(filter, "*", "timestamp.asc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onMessagesFetched(response.body());
                } else {
                    Log.e(TAG, "Fetch failed: " + response.code());
                    listener.onMessagesFetched(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e(TAG, "Network error fetching messages", t);
                listener.onMessagesFetched(new ArrayList<>());
            }
        });
    }

    public void fetchMessages(String partnerId, OnMessagesFetchedListener listener) {
        fetchMessagesSince(partnerId, 0, listener);
    }

    public void sendMessage(String text, String receiverId) {
        Message message = new Message();
        message.setSenderId(getCurrentUserId());
        message.setReceiverId(receiverId);
        message.setMessageText(text);
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType("TEXT");
        message.setReadStatus("SENT");
        
        sendMessage(message);
    }

    public void sendMessage(Message message) {
        ApiService api = SupabaseClient.getClient().create(ApiService.class);

        Map<String, Object> payload = new HashMap<>();
        String msgId = message.getMessageId() != null ? message.getMessageId() : UUID.randomUUID().toString();
        payload.put("id", msgId);
        payload.put("sender_id", message.getSenderId());
        payload.put("receiver_id", message.getReceiverId());
        payload.put("message_text", message.getMessageText());
        payload.put("timestamp", message.getTimestamp());
        payload.put("message_type", message.getMessageType());
        payload.put("read_status", message.getReadStatus());
        
        if (message.getMediaUrl() != null) payload.put("media_url", message.getMediaUrl());
        if (message.getFileName() != null) payload.put("file_name", message.getFileName());
        if (message.getFileSize() != null) payload.put("file_size", message.getFileSize());

        Log.d(TAG, "Sending message to Supabase: " + payload);

        api.sendMessage(payload, "return=representation")
                .enqueue(new Callback<List<Message>>() {
                    @Override
                    public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "SUCCESS: Message saved.");
                            updateLocalStatus(msgId, "SENT");
                        } else {
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                                Log.e(TAG, "ERROR (" + response.code() + "): " + errorBody);
                            } catch (Exception ignored) {}
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Message>> call, Throwable t) {
                        Log.e(TAG, "NETWORK ERROR while sending message", t);
                    }
                });
    }

    private void updateLocalStatus(String msgId, String status) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase.getInstance(appContext).messageDao().updateMessageStatusString(msgId, status);
        });
    }

    public void markMessageAsRead(String messageId) {
        if (!isValidUuid(messageId)) return;
        updateMessageStatus(messageId, "READ", "neq.READ");
    }

    public void markMessageAsDelivered(String messageId) {
        if (!isValidUuid(messageId)) return;
        updateMessageStatus(messageId, "DELIVERED", "eq.SENT");
    }

    private void updateMessageStatus(String messageId, String status, String filter) {
        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        Map<String, Object> updates = new HashMap<>();
        updates.put("read_status", status);

        api.updateMessageStatus("eq." + messageId, filter, updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Message " + messageId + " status updated to " + status + " (Filter: " + filter + ")");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to update message status", t);
            }
        });
    }

    public void markAllMessagesAsRead(String partnerId) {
        String currentId = getCurrentUserId();
        if (!isValidUuid(currentId) || !isValidUuid(partnerId)) return;

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        Map<String, Object> updates = new HashMap<>();
        updates.put("read_status", "READ");

        api.markMessagesAsRead("eq." + partnerId, "eq." + currentId, "neq.READ", updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) Log.d(TAG, "All messages from " + partnerId + " marked as read");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to mark all messages as read", t);
            }
        });
    }

    public void deleteConversation(String partnerId, Runnable onComplete) {
        String currentId = getCurrentUserId();
        if (!isValidUuid(currentId) || !isValidUuid(partnerId)) return;

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        String orFilter = "(and(sender_id.eq." + currentId + ",receiver_id.eq." + partnerId + ")," +
                         "and(sender_id.eq." + partnerId + ",receiver_id.eq." + currentId + "))";

        api.deleteMessages(orFilter).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Conversation deleted from Supabase: " + partnerId);
                    if (onComplete != null) onComplete.run();
                } else {
                    Log.e(TAG, "Failed to delete conversation from Supabase: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error while deleting conversation", t);
            }
        });
    }

    public void syncPendingMessages() {
        String currentId = getCurrentUserId();
        if (!isValidUuid(currentId)) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<MessageModel> pending = AppDatabase.getInstance(appContext).messageDao().getPendingMessages();
            for (MessageModel model : pending) {
                Message cloudMsg = new Message();
                cloudMsg.setMessageId(model.getCloudId());
                cloudMsg.setSenderId(currentId);
                cloudMsg.setReceiverId(model.getChatPartnerId());
                cloudMsg.setMessageText(SecurityUtils.encrypt(model.getMessage()));
                cloudMsg.setTimestamp(model.getTimestamp());
                cloudMsg.setMessageType(model.getType().name());
                cloudMsg.setReadStatus("SENT");
                
                sendMessage(cloudMsg);
            }
        });
    }

    public void sendNotification(String receiverId, String message) {
        Log.d(TAG, "sendNotification [Legacy]: To: " + receiverId + ", Msg: " + message);
    }

    public void createGroup(String name, String description, List<String> participantNames) {
        Log.d(TAG, "createGroup: " + name + " (" + description + ") with participants: " + participantNames);
    }

    public void fetchUserById(String userId, OnUsersFetchedListener listener) {
        if (userId == null) {
            listener.onUsersFetched(new ArrayList<>());
            return;
        }

        if (userCache.containsKey(userId)) {
            List<User> list = new ArrayList<>();
            list.add(userCache.get(userId));
            listener.onUsersFetched(list);
            return;
        }

        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        String filter = "id.eq." + userId;
        api.getUserProfile(filter).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    userCache.put(userId, user);
                    listener.onUsersFetched(response.body());
                } else {
                    listener.onUsersFetched(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                listener.onUsersFetched(new ArrayList<>());
            }
        });
    }

    public Call<List<User>> fetchAllUsers(OnUsersFetchedListener listener) {
        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        Call<List<User>> call = api.getAllUsers("*", "full_name.asc.nullslast");
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (User u : response.body()) if (u.getUserId() != null) userCache.put(u.getUserId(), u);
                    listener.onUsersFetched(response.body());
                } else {
                    listener.onUsersFetched(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (!call.isCanceled()) listener.onUsersFetched(null);
            }
        });
        return call;
    }

    public Call<List<User>> searchUsersByName(String query, OnUsersFetchedListener listener) {
        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        String orFilter = "(full_name.ilike.*" + query + "*,username.ilike.*" + query + "*,email.ilike.*" + query + "*)";
        Call<List<User>> call = api.searchUsers(orFilter, "*", "full_name.asc.nullslast", 50);
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (User u : response.body()) if (u.getUserId() != null) userCache.put(u.getUserId(), u);
                    listener.onUsersFetched(response.body());
                } else {
                    listener.onUsersFetched(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (!call.isCanceled()) listener.onUsersFetched(null);
            }
        });
        return call;
    }

    public void listenForPosts() {
        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        api.getPosts("*", "created_at.desc").enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Post post : response.body()) SharedDataManager.getInstance().addPost(post);
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) { }
        });
    }

    public void uploadPost(Post post) {
        ApiService api = SupabaseClient.getClient().create(ApiService.class);
        api.createPost(post, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Post uploaded successfully");
                } else {
                    Log.e(TAG, "Post upload failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Post upload network error", t);
            }
        });
    }

    public void uploadLargeFile(Uri fileUri, OnImageUploadListener listener) {
        String mimeType = appContext.getContentResolver().getType(fileUri);
        if (mimeType == null) {
            String path = fileUri.getPath();
            if (path != null && path.contains(".")) {
                String extension = path.substring(path.lastIndexOf(".") + 1);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
        }
        if (mimeType == null) mimeType = "application/octet-stream";

        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null) {
             String path = fileUri.getPath();
             if (path != null && path.contains(".")) {
                 extension = path.substring(path.lastIndexOf(".") + 1);
             } else {
                 extension = "file";
             }
        }

        String bucket = "chat-files";
        if (mimeType.startsWith("image")) bucket = "chat-images";
        else if (mimeType.startsWith("video")) bucket = "chat-videos";
        else if (mimeType.startsWith("audio")) bucket = "chat-audio";

        Log.d(TAG, "Uploading file: " + fileUri + " to bucket: " + bucket + " (Mime: " + mimeType + ")");
        uploadGenericFile(fileUri, bucket, UUID.randomUUID().toString() + "." + extension, mimeType, listener);
    }

    public void uploadGenericFile(Uri fileUri, String bucket, String fileName, String mimeType, OnImageUploadListener listener) {
        if (fileUri == null) {
            listener.onFailure(new Exception("File URI is null"));
            return;
        }

        try {
            InputStream inputStream = appContext.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                listener.onFailure(new Exception("Could not open input stream for URI: " + fileUri));
                return;
            }

            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] fileBytes = byteBuffer.toByteArray();
            inputStream.close();

            String path = fileName;
            ApiService api = SupabaseClient.getClient().create(ApiService.class);
            RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), fileBytes);

            api.uploadFile(bucket, path, requestBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        // Use the public endpoint for generated URLs
                        String imageUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + path;
                        Log.d(TAG, "File uploaded successfully: " + imageUrl);
                        listener.onSuccess(imageUrl);
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e(TAG, "Upload failed (" + response.code() + "): " + errorBody);
                            listener.onFailure(new Exception("Upload failed: " + response.code() + " - " + errorBody));
                        } catch (Exception e) {
                            listener.onFailure(new Exception("Upload failed: " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Network error during upload", t);
                    listener.onFailure(new Exception(t));
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception during upload setup", e);
            listener.onFailure(e);
        }
    }
}
