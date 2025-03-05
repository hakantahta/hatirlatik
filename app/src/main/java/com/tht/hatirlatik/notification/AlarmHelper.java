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
        long taskTime = task.getDateTime() != null ? task.getDateTime().getTime() : System.currentTimeMillis();
        long alarmTime = taskTime - (task.getReminderMinutes() * 60 * 1000L);
        
        Log.d(TAG, "scheduleAlarm: Görev zamanı: " + new java.util.Date(taskTime).toString());
        Log.d(TAG, "scheduleAlarm: Alarm zamanı: " + new java.util.Date(alarmTime).toString());
        Log.d(TAG, "scheduleAlarm: Şu anki zaman: " + new java.util.Date(System.currentTimeMillis()).toString());
        Log.d(TAG, "scheduleAlarm: Hatırlatma süresi: " + task.getReminderMinutes() + " dakika");

        // Eğer alarm zamanı geçmişse veya çok yakınsa (5 saniyeden az), hemen alarmı tetikle
        if (alarmTime <= System.currentTimeMillis() + 5000) {
            Log.d(TAG, "scheduleAlarm: Alarm zamanı geçmiş veya çok yakın, hemen tetikleniyor");
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("taskId", task.getId());
            intent.putExtra("taskTitle", task.getTitle());
            intent.putExtra("taskDescription", task.getDescription());
            context.sendBroadcast(intent);
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                !alarmManager.canScheduleExactAlarms()) {
                // Android 12 ve üzeri için exact alarm izni yoksa
                Log.d(TAG, "scheduleAlarm: Android 12+ için setAlarmClock kullanılıyor");
                alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(alarmTime, pendingIntent),
                    pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6 ve üzeri için
                Log.d(TAG, "scheduleAlarm: Android 6+ için setExactAndAllowWhileIdle kullanılıyor");
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                );
            } else {
                // Android 6 öncesi için
                Log.d(TAG, "scheduleAlarm: Android 6 öncesi için setExact kullanılıyor");
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                );
            }
            Log.d(TAG, "scheduleAlarm: Alarm başarıyla kuruldu. Görev ID: " + task.getId());
        } catch (Exception e) {
            Log.e(TAG, "scheduleAlarm: Alarm kurulurken hata oluştu", e);
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
        
        // Çalan alarmı ve titreşimi durdur
        AlarmReceiver.stopAlarmSound();
        AlarmReceiver.stopVibration();
        
        Log.d(TAG, "cancelAlarm: Alarm iptal edildi. Görev ID: " + task.getId());
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