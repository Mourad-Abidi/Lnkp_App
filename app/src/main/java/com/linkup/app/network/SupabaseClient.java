package com.linkup.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.linkup.app.database.FirebaseDatabaseManager;

import org.json.JSONObject;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("OKHTTP_LOG", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = getValidToken();
                            
                            // Let OkHttp/Retrofit handle Content-Type from converters and RequestBody
                            Request.Builder builder = original.newBuilder()
                                    .header("apikey", SupabaseConfig.API_KEY)
                                    .header("Authorization", "Bearer " + token);

                            return chain.proceed(builder.build());
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.URL + "/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    private static String getValidToken() {
        Context context = FirebaseDatabaseManager.getContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String token = prefs.getString("supabase_token", null);
            if (token != null && !isJwtExpired(token)) {
                return token;
            }
        }
        return SupabaseConfig.API_KEY;
    }

    private static boolean isJwtExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return true;
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            long exp = json.getLong("exp");
            return (System.currentTimeMillis() / 1000) > (exp - 60);
        } catch (Exception e) {
            return true;
        }
    }

    public static void resetClient() {
        retrofit = null;
    }
}
