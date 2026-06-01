package com.linkup.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.linkup.app.models.IntruderAlert;
import java.util.List;

@Dao
public interface IntruderAlertDao {
    @Insert
    void insert(IntruderAlert alert);

    @Query("SELECT * FROM intruder_alerts ORDER BY timestamp DESC")
    List<IntruderAlert> getAllAlerts();

    @Query("DELETE FROM intruder_alerts")
    void deleteAll();
}
