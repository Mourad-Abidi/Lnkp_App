package com.linkup.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import android.util.Pair;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;
import com.linkup.app.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NoteEditorActivity extends BaseActivity {

    private String label;
    private EditText etNoteTitle, etNoteContent;
    private View cardAiResult;
    private TextView tvAiExplanation, tvWordCount;
    private Chip chipPriority;
    private SharedPreferences notesPrefs;
    private final Handler autoSaveHandler = new Handler();
    private Runnable autoSaveRunnable;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleAttachment(uri, "Media")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        label = getIntent().getStringExtra("label");
        if (label == null) label = "General Note";

        notesPrefs = getSharedPreferences("PersonalNotes", Context.MODE_PRIVATE);

        initViews();
        setupToolbar();
        setupListeners();
        setupAutoSave();
        setupDragAndDrop();
        
        loadSavedNote();
    }

    private void initViews() {
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        cardAiResult = findViewById(R.id.cardAiResult);
        tvAiExplanation = findViewById(R.id.tvAiExplanation);
        tvWordCount = findViewById(R.id.tvWordCount);
        chipPriority = findViewById(R.id.chipPriority);
        TextView tvNoteLabel = findViewById(R.id.tvNoteLabel);

        if (tvNoteLabel != null) {
            tvNoteLabel.setText(label.toUpperCase());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Express & Explain");
            }
        }
    }

    private void setupListeners() {
        View btnSave = findViewById(R.id.btnSaveNote);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> finish());
        }

        findViewById(R.id.btnDiscard).setOnClickListener(v -> {
            notesPrefs.edit().remove(label + "_title").remove(label + "_content").apply();
            etNoteTitle.setText("");
            etNoteContent.setText("");
            Toast.makeText(this, "Note content cleared.", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnAiExplain).setOnClickListener(v -> runAiExplanation());
        
        findViewById(R.id.btnBold).setOnClickListener(v -> applySpan(new StyleSpan(Typeface.BOLD)));
        findViewById(R.id.btnItalic).setOnClickListener(v -> applySpan(new StyleSpan(Typeface.ITALIC)));
        findViewById(R.id.btnUnderline).setOnClickListener(v -> applySpan(new UnderlineSpan()));
        
        findViewById(R.id.btnBulletList).setOnClickListener(v -> insertTemplate("\n• "));
        
        findViewById(R.id.btnTimestamp).setOnClickListener(v -> {
            String time = new SimpleDateFormat("HH:mm, MMM dd: ", Locale.getDefault()).format(new Date());
            insertTemplate("\n[" + time + "] ");
        });

        findViewById(R.id.btnAttachImage).setOnClickListener(v -> pickMediaLauncher.launch("*/*"));
        
        findViewById(R.id.btnTranslate).setOnClickListener(v -> translateNote());
        
        findViewById(R.id.btnVoiceNote).setOnClickListener(v -> {
            Toast.makeText(this, "Voice transcription started (Simulated)", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                insertTemplate(" [Voice Memo: Record thoughts on " + label + "] ");
            }, 2000);
        });

        findViewById(R.id.btnReminder).setOnClickListener(v -> showReminderPicker());
        
        if (chipPriority != null) {
            chipPriority.setOnClickListener(v -> {
                String current = chipPriority.getText().toString();
                if (current.equals("Low")) {
                    chipPriority.setText("Medium");
                    chipPriority.setChipBackgroundColorResource(R.color.glass_white);
                } else if (current.equals("Medium")) {
                    chipPriority.setText("High");
                    chipPriority.setChipBackgroundColorResource(R.color.error_red);
                } else {
                    chipPriority.setText("Low");
                    chipPriority.setChipBackgroundColorResource(R.color.success_green);
                }
                notesPrefs.edit().putString(label + "_priority", chipPriority.getText().toString()).apply();
            });
        }
    }

    private void showReminderPicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                
                String reminderTime = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(calendar.getTime());
                Toast.makeText(this, "Secure reminder set for: " + reminderTime, Toast.LENGTH_LONG).show();
                notesPrefs.edit().putLong(label + "_reminder", calendar.getTimeInMillis()).apply();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
            
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupDragAndDrop() {
        ViewCompat.setOnReceiveContentListener(etNoteContent, new String[]{"image/*", "video/*", "application/*", "text/*"}, (view, content) -> {
            Pair<ContentInfoCompat, ContentInfoCompat> split = content.partition(item -> item.getUri() != null);
            ContentInfoCompat uriContent = split.first;
            ContentInfoCompat remaining = split.second;

            if (uriContent != null) {
                for (int i = 0; i < uriContent.getClip().getItemCount(); i++) {
                    Uri uri = uriContent.getClip().getItemAt(i).getUri();
                    handleAttachment(uri, "Dropped Source");
                }
            }
            return remaining;
        });
    }

    private void handleAttachment(Uri uri, String source) {
        if (uri != null) {
            String fileName = uri.getLastPathSegment();
            String fullAddress = uri.toString();
            String attachmentTag = String.format("\n[SOURCE: %s]\n[NAME: %s]\n[ADDRESS: %s]\n", source, fileName, fullAddress);
            insertTemplate(attachmentTag);
            Toast.makeText(this, source + " linked with full address", Toast.LENGTH_SHORT).show();
        }
    }

    private void applySpan(Object span) {
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();
        if (start != end) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(etNoteContent.getText());
            ssb.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            etNoteContent.setText(ssb);
            etNoteContent.setSelection(end);
        }
    }

    private void setupAutoSave() {
        TextWatcher autoSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (etNoteContent.hasFocus()) {
                    String text = s.toString().trim();
                    int words = text.isEmpty() ? 0 : text.split("\\s+").length;
                    tvWordCount.setText(words + " words");
                }
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveRunnable = () -> saveNoteData(false);
                autoSaveHandler.postDelayed(autoSaveRunnable, 1000);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etNoteTitle.addTextChangedListener(autoSaveWatcher);
        etNoteContent.addTextChangedListener(autoSaveWatcher);
    }

    private void insertTemplate(String template) {
        int start = etNoteContent.getSelectionStart();
        etNoteContent.getText().insert(start, template);
    }

    private void runAiExplanation() {
        String content = etNoteContent.getText().toString();
        if (content.length() < 5) {
            Toast.makeText(this, "Type more for AI to analyze", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "AI Analysis in progress...", Toast.LENGTH_SHORT).show();
        callVertexAI("Analyze this note and explain its core themes briefly: " + content, result -> {
            cardAiResult.setVisibility(View.VISIBLE);
            tvAiExplanation.setText("AI INSIGHT: " + result);
        });
    }

    private void translateNote() {
        String content = etNoteContent.getText().toString();
        if (content.isEmpty()) return;
        
        Toast.makeText(this, "Translating to Arabic...", Toast.LENGTH_SHORT).show();
        callVertexAI("Translate the following text to Arabic: " + content, result -> {
            cardAiResult.setVisibility(View.VISIBLE);
            tvAiExplanation.setText("TRANSLATION: " + result);
        });
    }

    private void callVertexAI(String prompt, AIResultCallback callback) {
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",
                "AIzaSyDlTwdktBFFQM1QKLR9_dXrHgCgZd-I2mI",
                null,
                null,
                new RequestOptions(),
                null,
                null,
                null
        );
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);
        
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);
        
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    String text = result.getText();
                    if (text != null) callback.onResult(text);
                });
            }
            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(NoteEditorActivity.this, "AI Error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }

    interface AIResultCallback {
        void onResult(String result);
    }

    private void loadSavedNote() {
        String savedTitle = notesPrefs.getString(label + "_title", "");
        String savedContent = notesPrefs.getString(label + "_content", "");
        etNoteTitle.setText(savedTitle);
        etNoteContent.setText(savedContent);
        
        String savedPriority = notesPrefs.getString(label + "_priority", "Medium");
        if (chipPriority != null) {
            chipPriority.setText(savedPriority);
            if (savedPriority.equals("High")) chipPriority.setChipBackgroundColorResource(R.color.error_red);
            else if (savedPriority.equals("Low")) chipPriority.setChipBackgroundColorResource(R.color.success_green);
        }
    }

    private void saveNoteData(boolean showToast) {
        String title = etNoteTitle.getText().toString();
        String content = etNoteContent.getText().toString();
        notesPrefs.edit().putString(label + "_title", title).putString(label + "_content", content).apply();
        if (showToast) Toast.makeText(this, "Note saved.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNoteData(false);
    }
}
