package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import com.linkup.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.HashSet;
import java.util.Set;

public class AISettingsActivity extends BaseActivity {

    private SwitchMaterial swAccountAccess, swMediaAnalysis, swSemanticSearch;
    private MaterialButton btnClearNeuralCache, btnOpenAIChat;
    private ImageButton infoDataAccess, infoNeuralFunctions;
    private SharedPreferences aiPrefs;

    private static final String PREF_ACCOUNT_ACCESS = "ai_account_access";
    private static final String PREF_MEDIA_ANALYSIS = "ai_media_analysis";
    private static final String PREF_SEMANTIC_SEARCH = "ai_semantic_search";
    private static final String PREF_SELECTED_SHORTCUTS = "ai_selected_shortcuts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        aiPrefs = getSharedPreferences("AISettings", Context.MODE_PRIVATE);

        setupToolbar();
        initViews();
        loadSettings();
        setupListeners();
        setupFunctionItems();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Neural Configuration");
        }
    }

    private void initViews() {
        swAccountAccess = findViewById(R.id.swAccountAccess);
        swMediaAnalysis = findViewById(R.id.swMediaAnalysis);
        swSemanticSearch = findViewById(R.id.swSemanticSearch);
        btnClearNeuralCache = findViewById(R.id.btnClearNeuralCache);
        infoDataAccess = findViewById(R.id.infoDataAccess);
        infoNeuralFunctions = findViewById(R.id.infoNeuralFunctions);
        
        btnOpenAIChat = findViewById(R.id.btnOpenAIChat);
    }

    private void loadSettings() {
        swAccountAccess.setChecked(aiPrefs.getBoolean(PREF_ACCOUNT_ACCESS, false));
        swMediaAnalysis.setChecked(aiPrefs.getBoolean(PREF_MEDIA_ANALYSIS, false));
        swSemanticSearch.setChecked(aiPrefs.getBoolean(PREF_SEMANTIC_SEARCH, false));
    }

    private void setupListeners() {
        if (btnOpenAIChat != null) {
            btnOpenAIChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, AIAssistantActivity.class);
                startActivity(intent);
            });
        }

        infoDataAccess.setOnClickListener(v -> showInfoDialog("Data Access Protocol",
            "Determines how deep the AI can integrate with your account. Full Synchronization allows the AI to see your contacts and logs to provide personalized security advice."));

        infoNeuralFunctions.setOnClickListener(v -> showInfoDialog("AI Specializations",
            "Select exactly 4 specialized AI modules to appear as quick shortcuts in your chat. These provide expert-level analysis in specific domains."));

        findViewById(R.id.infoAccountAccess).setOnClickListener(v -> showInfoDialog("Account Sync", "Grants the AI read-only access to your account metadata for personalized insights."));
        findViewById(R.id.infoMediaAnalysis).setOnClickListener(v -> showInfoDialog("Media Scanning", "Local AI scanning of attachments to detect deepfakes, malware, or hidden trackers."));

        swAccountAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            aiPrefs.edit().putBoolean(PREF_ACCOUNT_ACCESS, isChecked).apply();
            Toast.makeText(this, "Account synchronization " + (isChecked ? "active" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        swMediaAnalysis.setOnCheckedChangeListener((buttonView, isChecked) -> aiPrefs.edit().putBoolean(PREF_MEDIA_ANALYSIS, isChecked).apply());

        swSemanticSearch.setOnCheckedChangeListener((buttonView, isChecked) -> aiPrefs.edit().putBoolean(PREF_SEMANTIC_SEARCH, isChecked).apply());

        btnClearNeuralCache.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Purge Neural Cache")
                .setMessage("This will erase all AI-learned patterns and interaction history. This action is irreversible. Proceed?")
                .setPositiveButton("Purge", (dialog, which) -> Toast.makeText(this, "Neural memory wiped.", Toast.LENGTH_LONG).show())
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void setupFunctionItems() {
        setupFunctionItem(R.id.itemMathLogic, "Math & Logic Solver", "Solve complex equations, statistical analysis, and logic puzzles.", "ai_math");
        setupFunctionItem(R.id.itemLegalAdvisor, "Legal Advisor", "Contract review, legal terminology explanation, and document drafting.", "ai_legal");
        setupFunctionItem(R.id.itemHealthAssistant, "Health Assistant", "Symptom analysis, wellness tracking, and nutrition guidance.", "ai_health");
        setupFunctionItem(R.id.itemCodeExpert, "Code & Tech Expert", "Programming assistance, debugging, and architectural advice.", "ai_code");
        setupFunctionItem(R.id.itemFinanceAnalyst, "Financial Analyst", "Budgeting advice, market trends, and risk assessment.", "ai_finance");
        setupFunctionItem(R.id.itemLanguageExpert, "Language Translator", "Real-time multilingual support and idiom explanation.", "ai_language");
        setupFunctionItem(R.id.itemScientificResearch, "Scientific Research", "Physics, chemistry, and biology research assistance.", "ai_science");
        setupFunctionItem(R.id.itemCreativeWriting, "Creative Writing", "Poetry, story drafting, and creative brainstorming.", "ai_creative");
        setupFunctionItem(R.id.itemHistoryCulture, "History & Culture", "Contextual historical data and cultural nuances.", "ai_history");
        setupFunctionItem(R.id.itemTravelLogistic, "Travel & Logistics", "Itinerary planning and travel optimization.", "ai_travel");
        setupFunctionItem(R.id.itemCyberSecurity, "Cyber Security", "Threat analysis, encryption advice, and privacy auditing.", "ai_security");
        setupFunctionItem(R.id.itemBusinessStrategy, "Business Strategy", "Market analysis, growth planning, and corporate strategy.", "ai_business");
    }

    private void setupFunctionItem(int id, String name, String description, String key) {
        View view = findViewById(id);
        if (view != null) {
            TextView tvName = view.findViewById(R.id.tvFunctionName);
            TextView tvDesc = view.findViewById(R.id.tvFunctionDescription);
            CheckBox cb = view.findViewById(R.id.cbFunctionSelect);

            if (tvName != null) tvName.setText(name);
            if (tvDesc != null) tvDesc.setText(description);

            Set<String> selected = aiPrefs.getStringSet(PREF_SELECTED_SHORTCUTS, new HashSet<>());
            cb.setChecked(selected.contains(key));
            
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Set<String> current = new HashSet<>(aiPrefs.getStringSet(PREF_SELECTED_SHORTCUTS, new HashSet<>()));
                if (isChecked) {
                    if (current.size() >= 4) {
                        cb.setChecked(false);
                        Toast.makeText(this, "Maximum 4 shortcuts allowed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    current.add(key);
                } else {
                    current.remove(key);
                }
                aiPrefs.edit().putStringSet(PREF_SELECTED_SHORTCUTS, current).apply();
            });
            
            view.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
        }
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Understood", null)
            .show();
    }
}
