package com.linkup.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.linkup.app.models.MessageModel;
import com.linkup.app.models.IntruderAlert;
import com.linkup.app.models.Call;

@Database(entities = {MessageModel.class, IntruderAlert.class, Call.class}, version = 11) // Incremented to 11
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract MessageDao messageDao();
    public abstract IntruderAlertDao intruderAlertDao();
    public abstract CallDao callDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "linkup_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
