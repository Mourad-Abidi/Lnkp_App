package com.linkup.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.linkup.app.models.Call;
import java.util.List;

@Dao
public interface CallDao {
    @Insert
    void insertCall(Call call);

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    List<Call> getAllCalls();

    @Query("DELETE FROM calls")
    void deleteAll();
}
