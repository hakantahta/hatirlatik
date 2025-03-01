package com.tht.hatirlatik.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.MainActivity;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.receivers.NotificationActionReceiver;

public class NotificationHelper {
    private static final String CHANNEL_ID = "task_notifications";
    private static final String CHANNEL_NAME = "Görev Bildirimleri";
    private static final String CHANNEL_DESCRIPTION = "Görev hatırlatmaları için bildirimler";
    
    // Her görev için benzersiz bildirim ID'si kullanacağız
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
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showTaskNotification(Task task) {
        showTaskNotification(task.getId(), task.getTitle(), task.getDescription());
    }

    public void showTaskNotification(long taskId, String title, String description) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Görevi Tamamla butonu için intent
        Intent completeIntent = new Intent(context, NotificationActionReceiver.class);
        completeIntent.setAction(NotificationActionReceiver.ACTION_COMPLETE_TASK);
        completeIntent.putExtra("taskId", taskId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) taskId,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Sonra Hatırlat butonu için intent
        Intent remindLaterIntent = new Intent(context, NotificationActionReceiver.class);
        remindLaterIntent.setAction(NotificationActionReceiver.ACTION_REMIND_LATER);
        remindLaterIntent.putExtra("taskId", taskId);
        remindLaterIntent.putExtra("taskTitle", title);
        remindLaterIntent.putExtra("taskDescription", description);
        PendingIntent remindLaterPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (taskId + 1000), // Farklı bir request code kullanıyoruz
                remindLaterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500})
                .addAction(R.drawable.ic_check, "Görevi Tamamla", completePendingIntent)
                .addAction(R.drawable.ic_alarm, "Sonra Hatırlat", remindLaterPendingIntent);

        try {
            // Her görev için benzersiz bir bildirim ID'si kullanıyoruz
            notificationManager.notify((int) taskId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
            // Bildirim izni yoksa burada işlenebilir
        }
    }

    public void cancelNotification(long taskId) {
        notificationManager.cancel((int) taskId);
    }
} 