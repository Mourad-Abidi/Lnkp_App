package com.linkup.app.core;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.linkup.app.R;
import com.linkup.app.activities.ChatActivity;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "messages_channel";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Messages";
            String description = "Notifications for new messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification Channel created: " + CHANNEL_ID);
            }
        }
    }

    public static void showMessageNotification(Context context, String senderId, String senderName, String messageText) {
        Log.d(TAG, "Attempting to show notification from: " + senderName + " (" + senderId + ")");

        // 1. Check if permission is granted (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Aborting notification: POST_NOTIFICATIONS permission not granted.");
                return;
            }
        }

        // 2. Prevent notification if the chat with this user is currently open
        if (isChatOpenWithUser(senderId)) {
            Log.d(TAG, "Aborting notification: Chat is currently open with " + senderId);
            return;
        }

        try {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("user_id", senderId);
            intent.putExtra("user_name", senderName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 
                    senderId.hashCode(), 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher) // Small icon is mandatory
                    .setContentTitle(senderName)
                    .setContentText(messageText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(senderId.hashCode(), builder.build());
            Log.d(TAG, "Notification posted successfully for: " + senderName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error posting notification", e);
        }
    }

    private static boolean isChatOpenWithUser(String userId) {
        String activeId = SharedDataManager.getInstance().getActiveChatUserId();
        return userId != null && userId.equals(activeId);
    }
}
