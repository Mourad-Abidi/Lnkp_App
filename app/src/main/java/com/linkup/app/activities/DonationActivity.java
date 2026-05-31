package com.linkup.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.linkup.app.R;

public class DonationActivity extends BaseActivity {

    private TextInputEditText etDonationAmount;
    private MaterialButton btnSubmitDonation, btnSkipDonation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        etDonationAmount = findViewById(R.id.etDonationAmount);
        btnSubmitDonation = findViewById(R.id.btnSubmitDonation);
        btnSkipDonation = findViewById(R.id.btnSkipDonation);

        btnSubmitDonation.setOnClickListener(v -> performDonation());
        btnSkipDonation.setOnClickListener(v -> proceedToMain());
    }

    private void performDonation() {
        String amount = etDonationAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amount)) {
            Toast.makeText(this, R.string.please_enter_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic for XMPP/Firebase removed as requested.
        // You can implement Supabase donation storage here if needed.
        Toast.makeText(this, "Donation logic is being updated to Supabase.", Toast.LENGTH_SHORT).show();
        proceedToMain();
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
