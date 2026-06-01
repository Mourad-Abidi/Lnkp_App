package com.linkup.app.network;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.linkup.app.models.Message;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class SupabaseRealtimeManager {
    private static final String TAG = "SupabaseRealtime";
    private static SupabaseRealtimeManager instance;

    private WebSocket webSocket;
    private final Gson gson = new Gson();
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private String currentUserId;

    public interface MessageListener {
        void onNewMessage(Message message);
    }

    public static synchronized SupabaseRealtimeManager getInstance() {
        if (instance == null) instance = new SupabaseRealtimeManager();
        return instance;
    }

    public void startListening(String userId, MessageListener listener) {
        this.currentUserId = userId;
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
        
        if (webSocket == null) {
            connect();
        }
    }

    public void stopListening(MessageListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void connect() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();

        String wsUrl = SupabaseConfig.URL.replace("https://", "wss://")
                + "/realtime/v1/websocket?apikey=" + SupabaseConfig.API_KEY + "&vsn=1.0.0";

        Log.d(TAG, "Connecting to: " + wsUrl);

        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket Opened Successfully");
                joinChannel();
                startHeartbeat();
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                // Section 3: Raw Event Debugging
                Log.d(TAG, "RAW_MESSAGE: " + text);
                handleMessage(text);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.w(TAG, "WebSocket Closed: " + reason + " (Code: " + code + ")");
                SupabaseRealtimeManager.this.webSocket = null;
                reconnect();
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket Failure: " + t.getMessage());
                SupabaseRealtimeManager.this.webSocket = null;
                reconnect();
            }
        });
    }

    private void reconnect() {
        Log.d(TAG, "Attempting to reconnect in 5s...");
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::connect, 5000);
    }

    private void joinChannel() {
        try {
            // Subscription for INSERT and UPDATE events
            JSONObject joinMsg = new JSONObject();
            joinMsg.put("topic", "realtime:public:messages");
            joinMsg.put("event", "phx_join");
            
            JSONArray configs = new JSONArray();
            
            // Listen for INSERTs
            configs.put(new JSONObject()
                    .put("event", "INSERT")
                    .put("schema", "public")
                    .put("table", "messages"));
            
            // Listen for UPDATEs (for read status)
            configs.put(new JSONObject()
                    .put("event", "UPDATE")
                    .put("schema", "public")
                    .put("table", "messages"));
            
            joinMsg.put("payload", new JSONObject().put("config", new JSONObject().put("postgres_changes", configs)));
            joinMsg.put("ref", "1");
            
            Log.d(TAG, "Sending phx_join for messages table (INSERT & UPDATE)");
            if (webSocket != null) {
                webSocket.send(joinMsg.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error joining channel", e);
        }
    }

    private void handleMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String event = json.optString("event");
            
            if ("postgres_changes".equals(event)) {
                JSONObject payload = json.getJSONObject("payload");
                JSONObject data = payload.getJSONObject("data");
                JSONObject record = data.getJSONObject("record");

                Message message = gson.fromJson(record.toString(), Message.class);
                
                String senderId = message.getSenderId();
                String receiverId = message.getReceiverId();

                // Trigger listeners if user is either receiver (new message) or sender (status update)
                boolean isIncoming = receiverId != null && receiverId.equals(currentUserId);
                boolean isOutgoingUpdate = senderId != null && senderId.equals(currentUserId);

                if (isIncoming || isOutgoingUpdate) {
                    Log.d(TAG, "Valid event for current user. Incoming=" + isIncoming + ", OutgoingUpdate=" + isOutgoingUpdate);
                    for (MessageListener listener : listeners) {
                        listener.onNewMessage(message);
                    }
                } else {
                    Log.d(TAG, "Message filtered out. User=" + currentUserId + ", Sender=" + senderId + ", Receiver=" + receiverId);
                }
            } else if ("phx_reply".equals(event)) {
                JSONObject payload = json.optJSONObject("payload");
                if (payload != null && "ok".equals(payload.optString("status"))) {
                    Log.d(TAG, "Subscription ACTIVE for topic: " + json.optString("topic"));
                } else {
                    Log.e(TAG, "Subscription REJECTED: " + text);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing realtime msg: " + e.getMessage());
        }
    }

    private void startHeartbeat() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (webSocket != null) {
                webSocket.send("{\"topic\":\"phoenix\",\"event\":\"heartbeat\",\"payload\":{},\"ref\":\"hb\"}");
                startHeartbeat();
            }
        }, 30000);
    }
}
