package com.linkup.app.network;

import com.linkup.app.models.Message;
import com.linkup.app.models.Post;
import com.linkup.app.models.User;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Path;

public interface ApiService {

    // --- AUTHENTICATION ---
    
    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body AuthRequest request);

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> login(@Body AuthRequest request);

    @POST("auth/v1/signup")
    Call<AuthResponse> registerUser(@Body AuthRequest request);

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> loginUser(@Body AuthRequest request);

    // --- DATABASE (Using 'profiles' table for public user data) ---

    @POST("rest/v1/profiles")
    Call<Void> saveUserToDatabase(@Body User user, @Header("Prefer") String prefer);

    @GET("rest/v1/profiles")
    Call<List<User>> getUserProfile(@Query("id") String userId);

    @GET("rest/v1/profiles")
    Call<List<User>> getAllUsers(@Query("select") String select, @Query("order") String order);

    // Advanced search using Supabase 'or' filter
    @GET("rest/v1/profiles")
    Call<List<User>> searchUsers(@Query("or") String orFilter, @Query("select") String select, @Query("order") String order, @Query("limit") int limit);
    
    @GET("rest/v1/messages")
    Call<List<Message>> getMessages(@Query("chat_id") String chatId, @Query("order") String order);

    @GET("rest/v1/messages")
    Call<List<Message>> getMessagesByFilter(@Query("or") String orFilter, @Query("select") String select, @Query("order") String order);

    @GET("rest/v1/messages")
    Call<List<Message>> getMessagesWithMap(@QueryMap Map<String, String> filters);

    @POST("rest/v1/messages")
    Call<List<Message>> sendMessage(
            @Body Object message,
            @Header("Prefer") String prefer
    );

    @PATCH("rest/v1/messages")
    Call<Void> updateMessageStatus(
            @Query("id") String messageId,
            @Body Map<String, Object> updates
    );

    @PATCH("rest/v1/messages")
    Call<Void> markMessagesAsRead(
            @Query("sender_id") String senderId,
            @Query("receiver_id") String receiverId,
            @Query("read_status") String filterReadStatus,
            @Body Map<String, Object> updates
    );

    // --- SOCIAL FEED ---
    
    @GET("rest/v1/posts")
    Call<List<Post>> getPosts(@Query("select") String select, @Query("order") String order);

    @POST("rest/v1/posts")
    Call<Void> createPost(@Body Post post, @Header("Prefer") String prefer);

    // --- STORAGE ---
    @POST("storage/v1/object/{bucket}/{path}")
    Call<ResponseBody> uploadFile(
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String path,
            @Body RequestBody file
    );
}
