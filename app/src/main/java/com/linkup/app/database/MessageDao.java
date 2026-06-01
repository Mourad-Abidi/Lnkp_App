package com.linkup.app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.linkup.app.models.MessageModel;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageModel message);

    @Update
    void update(MessageModel message);

    @Delete
    void delete(MessageModel message);

    @Query("DELETE FROM messages WHERE chatPartnerId = :partnerId")
    void deleteMessagesForChat(String partnerId);

    @Query("SELECT COUNT(*) FROM messages WHERE chatPartnerId = :partnerId AND isSeen = 0 AND isSent = 0")
    int getUnreadCount(String partnerId);

    @Query("SELECT * FROM messages WHERE chatPartnerId = :partnerId ORDER BY timestamp ASC")
    List<MessageModel> getMessagesForChat(String partnerId);

    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<MessageModel> getPendingMessages();

    @Query("UPDATE messages SET status = :status, cloudId = :cloudId WHERE id = :localId")
    void updateMessageAfterSend(int localId, String cloudId, String status);

    @Query("UPDATE messages SET isSeen = :isSeen WHERE chatPartnerId = :partnerId")
    void markChatAsRead(String partnerId, boolean isSeen);

    @Query("UPDATE messages SET isSeen = :isSeen WHERE cloudId = :cloudId")
    void updateMessageStatus(String cloudId, boolean isSeen);

    @Query("UPDATE messages SET status = :status WHERE cloudId = :cloudId")
    void updateMessageStatusString(String cloudId, String status);

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE cloudId = :cloudId)")
    boolean exists(String cloudId);
}
