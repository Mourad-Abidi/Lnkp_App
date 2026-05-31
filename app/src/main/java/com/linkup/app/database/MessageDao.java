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

    @Query("SELECT * FROM messages WHERE chatPartnerId = :partnerId ORDER BY timestamp ASC")
    List<MessageModel> getMessagesForChat(String partnerId);

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE cloudId = :cloudId)")
    boolean exists(String cloudId);
}
