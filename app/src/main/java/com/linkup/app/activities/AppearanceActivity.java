package com.linkup.app.activities;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linkup.app.R;

public class AppearanceActivity extends BaseActivity {

    private RadioGroup rgTheme;
    private SeekBar sbFontSize;
    private TextView tvPreviewText;
    private SwitchMaterial switchAnimations;
    private SwitchMaterial switchGlassEffect;
    private SwitchMaterial switchCompactMode;
    private SwitchMaterial switchMemberCount;
    private LinearLayout llBackgroundList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Views
        rgTheme = findViewById(R.id.rgTheme);
        sbFontSize = findViewById(R.id.sbFontSize);
        tvPreviewText = findViewById(R.id.tvPreviewText);
        switchAnimations = findViewById(R.id.switchAnimations);
        switchGlassEffect = findViewById(R.id.switchGlassEffect);
        switchCompactMode = findViewById(R.id.switchCompactMode);
        switchMemberCount = findViewById(R.id.switchMemberCount);
        llBackgroundList = findViewById(R.id.llBackgroundList);

        setupThemeSelection();
        setupBackgroundList();
        setupFontSizeSelection();
        setupAppIconSelection();
        setupBubbleStyleSelection();
        setupAccentColorSelection();
        setupInterfaceOptions();
        setupEffects();
        
        // Initial Preview Update
        updateChatPreview();
    }

    private void setupThemeSelection() {
        // Default to Dark Mode if not set
        int currentTheme = settingsPrefs.getInt("app_theme", AppCompatDelegate.MODE_NIGHT_YES);
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            rgTheme.check(R.id.rbLight);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            rgTheme.check(R.id.rbDark);
        } else {
            rgTheme.check(R.id.rbSystem);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.rbLight) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.rbDark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            settingsPrefs.edit().putInt("app_theme", mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
            applyAppBackground();
        });
    }

    private void setupBackgroundList() {
        int[] resIds = {
            R.drawable.my_background_1,
            R.drawable.my_background_2,
            R.drawable.my_background_3,
            R.drawable.my_background_4,
            R.drawable.my_background_5,
            R.drawable.my_background_6,
            R.drawable.my_background_7,
            R.drawable.my_background_8,
            R.drawable.my_background_9,
            R.drawable.my_background_10
        };

        llBackgroundList.removeAllViews();
        // Use resource name for consistency with BaseActivity
        String currentBgName = settingsPrefs.getString("app_background_name", "my_background_1");

        for (int resId : resIds) {
            String resName = getResources().getResourceEntryName(resId);
            
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 200);
            params.setMargins(12, 8, 12, 8);
            card.setLayoutParams(params);
            card.setRadius(24f);
            card.setCardElevation(4f);
            card.setStrokeWidth(resName.equals(currentBgName) ? 6 : 0);
            card.setStrokeColor(Color.WHITE);
            card.setCardBackgroundColor(Color.TRANSPARENT);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageResource(resId);
            
            card.addView(iv);
            card.setOnClickListener(v -> {
                settingsPrefs.edit().putString("app_background_name", resName).apply();
                applyAppBackground();
                setupBackgroundList(); // Refresh strokes to show selection
                Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show();
            });

            llBackgroundList.addView(card);
        }
    }

    private void setupFontSizeSelection() {
        int fontSizeProgress = settingsPrefs.getInt("font_size_progress", 1);
        sbFontSize.setProgress(fontSizeProgress);

        sbFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateChatPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settingsPrefs.edit().putInt("font_size_progress", seekBar.getProgress()).apply();
            }
        });
    }

    private void setupBubbleStyleSelection() {
        View settingBubbleStyle = findViewById(R.id.settingBubbleStyle);
        if (settingBubbleStyle != null) {
            settingBubbleStyle.setOnClickListener(v -> {
                String[] styles = {"Default Rounded", "Square Sharp", "Messenger Style", "Minimalist"};
                new AlertDialog.Builder(this)
                    .setTitle("Select Bubble Style")
                    .setItems(styles, (dialog, which) -> {
                        settingsPrefs.edit().putInt("bubble_style", which).apply();
                        updateChatPreview();
                        Toast.makeText(this, "Chat style updated", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            });
        }
    }

    private void updateChatPreview() {
        if (tvPreviewText == null) return;

        // 1. Update Font Size
        int progress = sbFontSize.getProgress();
        float size = 16f;
        switch (progress) {
            case 0: size = 12f; break;
            case 1: size = 16f; break;
            case 2: size = 20f; break;
        }
        tvPreviewText.setTextSize(size);

        // 2. Update Bubble Style Appearance
        int style = settingsPrefs.getInt("bubble_style", 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(ContextCompat.getColor(this, R.color.primary));
        
        float radius;
        switch (style) {
            case 1: radius = 4f; break; // Square Sharp
            case 2: radius = 40f; break; // Messenger Style (Very round)
            case 3: // Minimalist (No background, just border)
                shape.setColor(Color.TRANSPARENT);
                shape.setStroke(2, Color.WHITE);
                radius = 12f;
                break;
            default: radius = 24f; break; // Default Rounded
        }
        shape.setCornerRadius(radius);
        tvPreviewText.setBackground(shape);
        tvPreviewText.setPadding(32, 16, 32, 16);
    }

    private void setupAppIconSelection() {
        View settingAppIcon = findViewById(R.id.settingAppIcon);
        if (settingAppIcon != null) {
            settingAppIcon.setOnClickListener(v -> {
                String[] icons = {"Default Blue", "Classic Gold", "Stealth Dark", "Neon Pink"};
                new AlertDialog.Builder(this)
                    .setTitle("Select App Icon")
                    .setItems(icons, (dialog, which) -> {
                        Toast.makeText(this, "App icon will change on next launch", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            });
        }
    }

    private void setupAccentColorSelection() {
        View settingAccentColor = findViewById(R.id.settingAccentColor);
        if (settingAccentColor != null) {
            settingAccentColor.setOnClickListener(v -> {
                String[] colors = {"LinkUp Blue", "Ruby Red", "Emerald Green", "Royal Purple", "Amber Orange"};
                new AlertDialog.Builder(this)
                    .setTitle("Select Accent Color")
                    .setItems(colors, (dialog, which) -> {
                        settingsPrefs.edit().putInt("accent_color_index", which).apply();
                        updateChatPreview(); // Refresh preview with new color if applicable
                        Toast.makeText(this, "Accent color updated", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            });
        }
    }

    private void setupInterfaceOptions() {
        if (switchCompactMode != null) {
            switchCompactMode.setChecked(settingsPrefs.getBoolean("chat_compact_mode", false));
            switchCompactMode.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("chat_compact_mode", isChecked).apply());
        }
        
        if (switchMemberCount != null) {
            switchMemberCount.setChecked(settingsPrefs.getBoolean("show_member_count", true));
            switchMemberCount.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("show_member_count", isChecked).apply());
        }
    }

    private void setupEffects() {
        if (switchAnimations != null) {
            switchAnimations.setChecked(settingsPrefs.getBoolean("enable_animations", true));
            switchAnimations.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("enable_animations", isChecked).apply());
        }

        if (switchGlassEffect != null) {
            switchGlassEffect.setChecked(settingsPrefs.getBoolean("enable_glass_effect", true));
            switchGlassEffect.setOnCheckedChangeListener((b, isChecked) -> 
                settingsPrefs.edit().putBoolean("enable_glass_effect", isChecked).apply());
        }
    }
}
