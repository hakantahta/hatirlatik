package com.tht.hatirlatik.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.receivers.AlarmReceiver;
import com.tht.hatirlatik.receivers.DailyWidgetUpdateReceiver;

import java.util.Calendar;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";
    private final Context context;
    private final AlarmManager alarmManager;
    private static final int ALARM_REQUEST_CODE = 100; // Sabit request code
    private static final int DAILY_WIDGET_UPDATE_REQUEST_CODE = 999; // Widget güncellemesi için sabit request code
    public static final String ACTION_DAILY_WIDGET_UPDATE = "com.tht.hatirlatik.ACTION_DAILY_WIDGET_UPDATE";

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

    /**
     * Günlük widget güncellemesi için alarm kurar.
     * Bu alarm her gün gece yarısı tetiklenir.
     */
    public void scheduleDailyWidgetUpdate() {
        Log.d(TAG, "scheduleDailyWidgetUpdate: Günlük widget güncelleme alarmı kuruluyor");
        
        // Gece yarısı için Calendar nesnesi oluştur
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // Eğer şu anki zaman gece yarısını geçtiyse, bir sonraki güne ayarla
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        long triggerTime = calendar.getTimeInMillis();
        
        Intent intent = new Intent(context, DailyWidgetUpdateReceiver.class);
        intent.setAction(ACTION_DAILY_WIDGET_UPDATE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_WIDGET_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        try {
            // Android sürümüne göre uygun alarm tipini seç
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                !alarmManager.canScheduleExactAlarms()) {
                // Android 12 ve üzeri için exact alarm izni yoksa
                alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6 ve üzeri için
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
            } else {
                // Android 6 öncesi için
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
            }
            
            Log.d(TAG, "scheduleDailyWidgetUpdate: Günlük widget güncelleme alarmı kuruldu. Tetiklenme zamanı: " 
                    + calendar.getTime().toString());
        } catch (Exception e) {
            Log.e(TAG, "scheduleDailyWidgetUpdate: Alarm kurulurken hata oluştu", e);
        }
    }
    
    /**
     * Günlük widget güncelleme alarmını iptal eder.
     */
    public void cancelDailyWidgetUpdate() {
        Intent intent = new Intent(context, DailyWidgetUpdateReceiver.class);
        intent.setAction(ACTION_DAILY_WIDGET_UPDATE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_WIDGET_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "cancelDailyWidgetUpdate: Günlük widget güncelleme alarmı iptal edildi");
    }
} 