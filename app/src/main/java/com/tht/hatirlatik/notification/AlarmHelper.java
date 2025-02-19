package com.tht.hatirlatik.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.receivers.AlarmReceiver;

public class AlarmHelper {
    private final Context context;
    private final AlarmManager alarmManager;
    private static final int ALARM_REQUEST_CODE = 100; // Sabit request code

    public AlarmHelper(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleAlarm(Task task) {
        // Alarm zamanını hesapla (görev zamanından hatırlatma süresini çıkar)
        long alarmTime = task.getDateTime().getTime() - 
                        (task.getReminderMinutes() * 60 * 1000);

        // Eğer alarm zamanı geçmişse, alarm kurma
        if (alarmTime <= System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("taskId", task.getId());
        intent.putExtra("taskTitle", task.getTitle());
        intent.putExtra("taskDescription", task.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Android sürümüne göre uygun alarm tipini seç
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
            );
        }
    }

    public void cancelAlarm(Task task) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("taskId", task.getId()); // Task ID'sini intent'e ekle

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
} 