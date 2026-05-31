package com.linkup.app.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.linkup.app.R;
import com.linkup.app.adapters.IntruderAlertAdapter;
import com.linkup.app.database.AppDatabase;
import com.linkup.app.models.IntruderAlert;
import java.util.List;

public class SecurityHistoryActivity extends BaseActivity {

    private RecyclerView rvSecurityHistory;
    private TextView tvEmptyHistory;
    private IntruderAlertAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSecurityHistory = findViewById(R.id.rvSecurityHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        
        db = AppDatabase.getInstance(this);
        if (rvSecurityHistory != null) {
            rvSecurityHistory.setLayoutManager(new LinearLayoutManager(this));
        }

        loadAlerts();
    }

    private void loadAlerts() {
        if (db == null) return;
        AsyncTask.execute(() -> {
            List<IntruderAlert> alerts = db.intruderAlertDao().getAllAlerts();
            runOnUiThread(() -> {
                if (alerts.isEmpty()) {
                    if (tvEmptyHistory != null) tvEmptyHistory.setVisibility(View.VISIBLE);
                    if (rvSecurityHistory != null) rvSecurityHistory.setVisibility(View.GONE);
                } else {
                    if (tvEmptyHistory != null) tvEmptyHistory.setVisibility(View.GONE);
                    if (rvSecurityHistory != null) {
                        rvSecurityHistory.setVisibility(View.VISIBLE);
                        adapter = new IntruderAlertAdapter(alerts);
                        rvSecurityHistory.setAdapter(adapter);
                    }
                }
            });
        });
    }
}
