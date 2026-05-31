package com.linkup.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.linkup.app.R;
import com.linkup.app.database.AppDatabase;
import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private IntroAdapter adapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        if (!prefs.getBoolean("initial_void_reset_done", false)) {
            resetApp();
            prefs.edit().putBoolean("initial_void_reset_done", true).apply();
        }

        if (prefs.getBoolean("intro_shown", false)) {
            startLoginActivity();
            return;
        }

        setContentView(R.layout.activity_intro);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        View llBottom = findViewById(R.id.llBottom);

        if (llBottom != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
            llBottom.startAnimation(slideUp);
        }

        List<IntroPage> pages = new ArrayList<>();
        pages.add(new IntroPage(getString(R.string.intro_title_1), getString(R.string.intro_desc_1), R.drawable.app_logo_png));
        pages.add(new IntroPage(getString(R.string.intro_title_2), getString(R.string.intro_desc_2), R.drawable.app_logo_png));
        pages.add(new IntroPage(getString(R.string.intro_title_3), getString(R.string.intro_desc_3), R.drawable.app_logo_png));

        adapter = new IntroAdapter(pages);
        viewPager.setAdapter(adapter);
        
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1.0f - absPos);
            float scale = 0.85f + (1.0f - 0.85f) * (1.0f - absPos);
            page.setScaleX(scale);
            page.setScaleY(scale);
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText(R.string.get_started);
                } else {
                    btnNext.setText(R.string.next);
                }
                Animation pulse = AnimationUtils.loadAnimation(IntroActivity.this, android.R.anim.fade_in);
                btnNext.startAnimation(pulse);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                markIntroShown();
                startLoginActivity();
            }
        });
    }

    private void resetApp() {
        getSharedPreferences("LinkUpUsage", Context.MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().clear().apply();
        
        AsyncTask.execute(() -> {
            AppDatabase.getInstance(this).clearAllTables();
        });
    }

    private void markIntroShown() {
        prefs.edit().putBoolean("intro_shown", true).apply();
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private static class IntroPage {
        String title, description;
        int imageRes;

        IntroPage(String title, String description, int imageRes) {
            this.title = title;
            this.description = description;
            this.imageRes = imageRes;
        }
    }

    private static class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.ViewHolder> {
        private List<IntroPage> pages;

        IntroAdapter(List<IntroPage> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            IntroPage page = pages.get(position);
            holder.title.setText(page.title);
            holder.desc.setText(page.description);
            holder.image.setImageResource(page.imageRes);
            Animation anim = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.fade_in_up);
            holder.image.startAnimation(anim);
            holder.title.startAnimation(anim);
            holder.desc.startAnimation(anim);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, desc;

            ViewHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.ivIntroImage);
                title = itemView.findViewById(R.id.tvIntroTitle);
                desc = itemView.findViewById(R.id.tvIntroDesc);
            }
        }
    }
}
