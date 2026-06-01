package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import com.linkup.app.R;
import com.google.android.material.button.MaterialButton;

public class ForgotPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ImageButton backButton = findViewById(R.id.backButton);
        MaterialButton resetButton = findViewById(R.id.resetButton);

        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Handle password reset logic
                    Toast.makeText(ForgotPasswordActivity.this, "Reset link sent!", Toast.LENGTH_SHORT).show();
                    
                    // After successful reset request, navigate to MainActivity
                    Intent intent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
                    startActivity(intent);
                    finishAffinity();
                }
            });
        }
    }
}
