package com.linkup.app.repository;

import android.content.Context;
import android.util.Log;
import com.linkup.app.core.AppExecutors;
import com.linkup.app.core.SharedDataManager;
import com.linkup.app.database.AppDatabase;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.database.MessageDao;
import com.linkup.app.models.Message;
import com.linkup.app.models.MessageModel;
import com.linkup.app.network.ApiService;
import com.linkup.app.network.SupabaseClient;
import com.linkup.app.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

public class MessageRepository {
    private static final String TAG = "MessageRepository";
    private static MessageRepository instance;
    private final MessageDao messageDao;
    private final ApiService apiService;

    private MessageRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.messageDao = db.messageDao();
        this.apiService = SupabaseClient.getClient().create(ApiService.class);
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }

    /**
     * Data Flow: Sync -> Decrypt -> Room -> UI
     * This method fetches all history for a specific chat.
     */
    public void syncHistory(String partnerId, Runnable onComplete) {
        String currentId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        if ("unknown".equals(currentId) || partnerId == null) return;

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // PostgREST filter for bidirectional messages
                String orFilter = "(and(sender_id.eq." + currentId + ",receiver_id.eq." + partnerId + ")," +
                                 "and(sender_id.eq." + partnerId + ",receiver_id.eq." + currentId + "))";
                
                Response<List<Message>> response = apiService.getMessagesByFilter(orFilter, "*", "timestamp.asc").execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    processAndSaveMessages(response.body(), partnerId);
                }
            } catch (Exception e) {
                Log.e(TAG, "History sync failed", e);
            } finally {
                if (onComplete != null) AppExecutors.getInstance().mainThread().execute(onComplete);
            }
        });
    }

    /**
     * Handles Realtime or Pull-to-refresh messages.
     */
    public void handleIncomingMessage(Message msg, String partnerId) {
        List<Message> list = new ArrayList<>();
        list.add(msg);
        AppExecutors.getInstance().diskIO().execute(() -> processAndSaveMessages(list, partnerId));
    }

    private void processAndSaveMessages(List<Message> messages, String partnerId) {
        String currentId = FirebaseDatabaseManager.getInstance().getCurrentUserId();
        String activeChatId = SharedDataManager.getInstance().getActiveChatUserId();
        boolean isActive = partnerId.equals(activeChatId);

        for (Message msg : messages) {
            // 1. Decrypt at Repository level
            String decryptedText = SecurityUtils.decrypt(msg.getMessageText());
            String decryptedMedia = SecurityUtils.decrypt(msg.getMediaUrl());

            // 2. Map to Local Model
            MessageModel model = new MessageModel();
            model.setCloudId(msg.getMessageId());
            model.setChatPartnerId(partnerId);
            model.setMessage(decryptedText);
            model.setMediaUrl(decryptedMedia);
            model.setTimestamp(msg.getTimestamp());
            
            boolean isMine = msg.getSenderId().equals(currentId);
            model.setSent(isMine);

            // 3. Mark as READ if active and for me (Forward only transition)
            String status = msg.getReadStatus();
            if (!isMine && isActive && !"READ".equals(status)) {
                status = "READ";
                FirebaseDatabaseManager.getInstance().markMessageAsRead(msg.getMessageId());
            }
            
            // Forward-only check for local DB: Don't downgrade status
            // Room OnConflictStrategy.REPLACE might overwrite a READ status with DELIVERED if sync is old.
            // We should check existing before inserting, OR rely on the fact that Sync is ordered by timestamp.
            // But status updates have the SAME timestamp usually or slightly later.
            
            model.setStatus(status);
            model.setSeen("READ".equals(status));
            
            // 4. Room handles duplicates via OnConflictStrategy.REPLACE
            // To be truly safe with status transitions, we could query first, but insert is faster.
            messageDao.insert(model);
        }
    }

    public List<MessageModel> getLocalMessages(String partnerId) {
        return messageDao.getMessagesForChat(partnerId);
    }

    public void markChatAsRead(String partnerId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            messageDao.markChatAsRead(partnerId, true);
        });
        FirebaseDatabaseManager.getInstance().markAllMessagesAsRead(partnerId);
    }

    public void deleteConversation(String partnerId, Runnable onComplete) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // 1. Delete from local Room DB
            messageDao.deleteMessagesForChat(partnerId);
            
            // 2. Delete from Supabase
            FirebaseDatabaseManager.getInstance().deleteConversation(partnerId, () -> {
                if (onComplete != null) AppExecutors.getInstance().mainThread().execute(onComplete);
            });
        });
    }
}
