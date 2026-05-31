package com.linkup.app.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient for legacy or internal services.
 * Note: Main cloud backend is now handled by SupabaseClient.
 */
public class ApiClient {
    private static final String BASE_URL = "https://your-fallback-api.com/"; // Updated from local XAMPP
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
