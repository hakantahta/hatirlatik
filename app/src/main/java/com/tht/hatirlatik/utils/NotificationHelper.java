package com.tht.hatirlatik.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.tht.hatirlatik.MainActivity;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;

public class NotificationHelper {
    private static final String CHANNEL_ID = "task_notifications";
    private static final String CHANNEL_NAME = "Görev Bildirimleri";
    private static final String CHANNEL_DESCRIPTION = "Görev hatırlatmaları için bildirimler";

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
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
            notificationManager.createNotificationChannel(channel);
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
                .setContentTitle(task.getTitle())
                .setContentText(task.getDescription())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            int notificationId = (int) (task.getId() % Integer.MAX_VALUE);
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // İzin hatası durumunda sessizce devam et
        }
    }

    public void cancelNotification(long taskId) {
        if (!hasNotificationPermission()) {
            return;
        }
        
        try {
            int notificationId = (int) (taskId % Integer.MAX_VALUE);
            notificationManager.cancel(notificationId);
        } catch (SecurityException e) {
            // İzin hatası durumunda sessizce devam et
        }
    }
} 