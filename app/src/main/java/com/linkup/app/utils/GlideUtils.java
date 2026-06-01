package com.linkup.app.utils;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.linkup.app.R;
import com.linkup.app.network.SupabaseConfig;

public class GlideUtils {

    public static void loadImage(Context context, String url, ImageView imageView) {
        if (url == null || url.isEmpty()) return;

        if (url.contains("supabase.co")) {
            GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.API_KEY)
                    .build());
            
            Glide.with(context)
                    .load(glideUrl)
                    .placeholder(R.drawable.app_logo)
                    .centerCrop()
                    .into(imageView);
        } else {
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.app_logo)
                    .centerCrop()
                    .into(imageView);
        }
    }
}
