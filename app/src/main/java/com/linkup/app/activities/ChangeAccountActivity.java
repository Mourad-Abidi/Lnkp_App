package com.linkup.app.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.linkup.app.R;

public class ChangeAccountActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_account);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupCurrentAccount();
        setupAccountActions();
    }

    private void setupCurrentAccount() {
        TextView tvUser = findViewById(R.id.tvCurrentUsername);
        TextView tvEmail = findViewById(R.id.tvCurrentEmail);
        
        // In a real app, you'd get this from your Auth system
        if (tvUser != null) tvUser.setText("Current User");
        if (tvEmail != null) tvEmail.setText("user@example.com");
    }

    private void setupAccountActions() {
        findViewById(R.id.btnAddAccount).setOnClickListener(v -> {
            Toast.makeText(this, "Opening Add Account flow...", Toast.LENGTH_SHORT).show();
        });
        
        // Handle RecyclerView or other list items if they were dynamically populated
    }
}
