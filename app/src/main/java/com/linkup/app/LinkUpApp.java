package com.linkup.app;

import android.app.Application;
import com.linkup.app.database.FirebaseDatabaseManager;
import com.linkup.app.network.PresenceManager;

public class LinkUpApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabaseManager.init(this);
        PresenceManager.getInstance().init();
    }
}
