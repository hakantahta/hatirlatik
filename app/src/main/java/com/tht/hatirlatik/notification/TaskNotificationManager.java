package com.tht.hatirlatik.notification;

import android.content.Context;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.workers.TaskWorker;
import java.util.concurrent.TimeUnit;

public class TaskNotificationManager {
    private final Context context;
    private final NotificationHelper notificationHelper;
    private final AlarmHelper alarmHelper;

    public TaskNotificationManager(Context context) {
        this.context = context;
        this.notificationHelper = new NotificationHelper(context);
        this.alarmHelper = new AlarmHelper(context);
    }

    public void scheduleTaskReminder(Task task) {
        // Hatırlatma süresini hesapla (milisaniye cinsinden)
        long taskTime = task.getDateTime().getTime();
        long reminderTime = taskTime - (task.getReminderMinutes() * 60 * 1000L);
        long delayMillis = reminderTime - System.currentTimeMillis();

        // Eğer zaman geçmişse, hemen bildirim göster
        if (delayMillis <= 0) {
            delayMillis = 0;
        }

        // WorkManager için input data oluştur
        Data inputData = new Data.Builder()
                .putLong("taskId", task.getId())
                .putString("taskTitle", task.getTitle())
                .putString("taskDescription", task.getDescription())
                .putString("notificationType", task.getNotificationType().name())
                .build();

        // WorkRequest oluştur - her görev için benzersiz bir tag kullan
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("task_" + task.getId())
                .build();

        // Work'ü planla
        WorkManager.getInstance(context).enqueue(workRequest);

        // Eğer alarm tipi seçilmişse, AlarmManager ile de planla
        if (task.getNotificationType() == NotificationType.ALARM || 
            task.getNotificationType() == NotificationType.NOTIFICATION_AND_ALARM) {
            alarmHelper.scheduleAlarm(task);
        }
    }

    public void cancelTaskReminder(Task task) {
        // WorkManager'daki işi iptal et
        WorkManager.getInstance(context)
                .cancelAllWorkByTag("task_" + task.getId());

        // Alarmı iptal et
        alarmHelper.cancelAlarm(task);

        // Bildirimi iptal et
        notificationHelper.cancelNotification(task.getId());
    }

    public void updateTaskReminder(Task task) {
        // Önce mevcut hatırlatıcıları iptal et
        cancelTaskReminder(task);
        
        // Yeni hatırlatıcıyı planla
        scheduleTaskReminder(task);
    }
    
    public void scheduleRemindLater(Task task, int delayMinutes) {
        // Mevcut bildirimi iptal et
        notificationHelper.cancelNotification(task.getId());
        
        // Yeni hatırlatma zamanını ayarla
        long newTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000);
        
        // WorkManager için input data oluştur
        Data inputData = new Data.Builder()
                .putLong("taskId", task.getId())
                .putString("taskTitle", task.getTitle())
                .putString("taskDescription", task.getDescription())
                .putString("notificationType", task.getNotificationType().name())
                .build();

        // WorkRequest oluştur - benzersiz bir tag kullan
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("task_remind_later_" + task.getId())
                .build();

        // Work'ü planla
        WorkManager.getInstance(context).enqueue(workRequest);
    }
} 