package com.linkup.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.linkup.app.R;
import com.linkup.app.models.GhostMessage;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class GhostChatActivity extends BaseActivity {

    private TextInputEditText etReceiver, etMessage;
    private TextView tvSelectedTime;
    private MaterialButton btnSetTime, btnSendGhost, btnGoToInbox;
    private Calendar scheduledCalendar = Calendar.getInstance();
    private boolean isTimeSet = false;

    private static final String PREFS_NAME = "GhostChatPrefs";
    private static final String LAST_SENT_KEY = "lastSentTimestamp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ghost_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etReceiver = findViewById(R.id.etReceiver);
        etMessage = findViewById(R.id.etMessage);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnSetTime = findViewById(R.id.btnSetTime);
        btnSendGhost = findViewById(R.id.btnSendGhost);
        btnGoToInbox = findViewById(R.id.btnGoToInbox);

        if (btnSetTime != null) btnSetTime.setOnClickListener(v -> showDateTimePicker());
        if (btnSendGhost != null) btnSendGhost.setOnClickListener(v -> attemptSendGhostMessage());
        if (btnGoToInbox != null) btnGoToInbox.setOnClickListener(v -> startActivity(new Intent(this, GhostInboxActivity.class)));
    }

    private void showDateTimePicker() {
        Calendar current = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            scheduledCalendar.set(Calendar.YEAR, year);
            scheduledCalendar.set(Calendar.MONTH, month);
            scheduledCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                scheduledCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                scheduledCalendar.set(Calendar.MINUTE, minute);
                scheduledCalendar.set(Calendar.SECOND, 0);

                if (scheduledCalendar.before(Calendar.getInstance())) {
                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show();
                } else {
                    isTimeSet = true;
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    if (tvSelectedTime != null) tvSelectedTime.setText("Scheduled Open Time: " + sdf.format(scheduledCalendar.getTime()));
                }
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show();

        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void attemptSendGhostMessage() {
        if (!canSendToday()) {
            Toast.makeText(this, "You can only send one Ghost Message per day!", Toast.LENGTH_LONG).show();
            return;
        }

        String receiver = etReceiver != null ? etReceiver.getText().toString().trim() : "";
        String content = etMessage != null ? etMessage.getText().toString().trim() : "";

        if (receiver.isEmpty() || content.isEmpty() || !isTimeSet) {
            Toast.makeText(this, "Please fill all fields and set opening time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Ghost Message
        GhostMessage ghost = new GhostMessage();
        ghost.setId(UUID.randomUUID().toString());
        ghost.setReceiverName(receiver);
        ghost.setContent(content);
        ghost.setContentType("TEXT");
        ghost.setScheduledOpenTime(scheduledCalendar.getTimeInMillis());
        ghost.setSentTimestamp(System.currentTimeMillis());
        ghost.setRead(false);
        ghost.setSenderId("Anonymous"); // No identity

        // In a real app, send to Firebase/Server here. 
        // For this demo, we'll save it locally in a "Mock Server" list (Shared Preferences)
        saveGhostMessageLocally(ghost);

        updateLastSentTimestamp();
        
        Toast.makeText(this, "Ghost message sent! It has disappeared from your history.", Toast.LENGTH_LONG).show();
        finish(); // It disappears from the sender's UI immediately
    }

    private boolean canSendToday() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastSent = prefs.getLong(LAST_SENT_KEY, 0);
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTimeInMillis(lastSent);
        
        Calendar now = Calendar.getInstance();
        
        if (lastSent == 0) return true;
        
        // Check if it's the same day
        return !(lastCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                 lastCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR));
    }

    private void updateLastSentTimestamp() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(LAST_SENT_KEY, System.currentTimeMillis()).apply();
    }

    private void saveGhostMessageLocally(GhostMessage ghost) {
        SharedPreferences prefs = getSharedPreferences("GhostInbox", MODE_PRIVATE);
        String currentList = prefs.getString("messages", "[]");
        // Simplified: appending to a JSON array string
        // In production, use Room or a real backend
        String newList = currentList.substring(0, currentList.length() - 1);
        if (newList.length() > 1) newList += ",";
        newList += ghost.toJson() + "]";
        prefs.edit().putString("messages", newList).apply();
    }
}
