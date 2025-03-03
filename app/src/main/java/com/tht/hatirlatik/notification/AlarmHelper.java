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
        long taskTime = task.getDateTime().getTime();
        long alarmTime = taskTime - (task.getReminderMinutes() * 60 * 1000L);

        // Eğer alarm zamanı geçmişse, alarm kurma
        if (alarmTime <= System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("taskId", task.getId());
        intent.putExtra("taskTitle", task.getTitle());
        intent.putExtra("taskDescription", task.getDescription());

        // Her görev için benzersiz bir request code kullan
        int requestCode = (int) task.getId();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Android sürümüne göre uygun alarm tipini seç
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
            !alarmManager.canScheduleExactAlarms()) {
            // Android 12 ve üzeri için exact alarm izni yoksa
            alarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(alarmTime, pendingIntent),
                pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6 ve üzeri için
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            );
        } else {
            // Android 6 öncesi için
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            );
        }
    }

    public void cancelAlarm(Task task) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        
        // Çalan alarmı durdur
        AlarmReceiver.stopAlarmSound();
    }
} 