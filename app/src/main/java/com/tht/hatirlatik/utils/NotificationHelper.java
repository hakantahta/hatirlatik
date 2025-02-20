package com.tht.hatirlatik.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.tht.hatirlatik.MainActivity;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.preferences.PreferencesManager;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "task_notifications";
    private static final String CHANNEL_NAME = "Görev Bildirimleri";
    private static final String CHANNEL_DESCRIPTION = "Görev hatırlatmaları için bildirimler";

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final PreferencesManager preferencesManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        this.preferencesManager = new PreferencesManager(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            
            // Ses ayarlarını yapılandır
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);
            
            // Titreşim ayarlarını yapılandır
            if (preferencesManager.isNotificationVibrationEnabled()) {
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            }

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public void showTaskNotification(Task task) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Bildirim izni verilmemiş");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) task.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.getTitle())
                .setContentText(task.getDescription())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Ses ayarlarını kontrol et
        if (preferencesManager.isNotificationSoundEnabled()) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);
        }

        // Titreşim ayarlarını kontrol et
        if (preferencesManager.isNotificationVibrationEnabled()) {
            builder.setVibrate(new long[]{0, 500, 250, 500});
        }

        try {
            int notificationId = (int) (task.getId() % Integer.MAX_VALUE);
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Bildirim gösterilirken hata oluştu: " + e.getMessage());
        }
    }

    public void showTaskNotification(String title, String description) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Bildirim izni verilmemiş");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Ses ayarlarını kontrol et
        if (preferencesManager.isNotificationSoundEnabled()) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);
        }

        // Titreşim ayarlarını kontrol et
        if (preferencesManager.isNotificationVibrationEnabled()) {
            builder.setVibrate(new long[]{0, 500, 250, 500});
        }

        try {
            notificationManager.notify(0, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Bildirim gösterilirken hata oluştu: " + e.getMessage());
        }
    }

    public void cancelNotification(long taskId) {
        try {
            int notificationId = (int) (taskId % Integer.MAX_VALUE);
            notificationManager.cancel(notificationId);
        } catch (SecurityException e) {
            Log.e(TAG, "Bildirim iptal edilirken hata oluştu: " + e.getMessage());
        }
    }

    public void cancelAllNotifications() {
        try {
            notificationManager.cancelAll();
        } catch (SecurityException e) {
            Log.e(TAG, "Tüm bildirimler iptal edilirken hata oluştu: " + e.getMessage());
        }
    }
} 