package com.linkup.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.linkup.app.R;

public class SupportActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@linkup.app"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - LinkUp");
            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (Exception e) {
                Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show();
            }
        });
            
        findViewById(R.id.btnLiveChat).setOnClickListener(v -> {
            Intent intent = new Intent(this, AIChatActivity.class);
            intent.putExtra("user_name", "Live Support");
            startActivity(intent);
            Toast.makeText(this, "Connecting to live support...", Toast.LENGTH_SHORT).show();
        });
    }
}
