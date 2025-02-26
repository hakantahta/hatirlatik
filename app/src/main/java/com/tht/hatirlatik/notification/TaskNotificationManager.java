package com.tht.hatirlatik.notification;

import android.content.Context;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.tht.hatirlatik.model.Task;
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
        long delayMillis = task.getDateTime().getTime() - 
                System.currentTimeMillis() - 
                (task.getReminderMinutes() * 60 * 1000);

        // WorkManager için input data oluştur
        Data inputData = new Data.Builder()
                .putLong("taskId", task.getId())
                .putString("taskTitle", task.getTitle())
                .putString("taskDescription", task.getDescription())
                .putString("notificationType", task.getNotificationType().name())
                .build();

        // WorkRequest oluştur
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("task_" + task.getId())
                .build();

        // Work'ü planla
        WorkManager.getInstance(context).enqueue(workRequest);

        // Eğer alarm tipi seçilmişse, AlarmManager ile de planla
        switch (task.getNotificationType()) {
            case ALARM:
            case NOTIFICATION_AND_ALARM:
                alarmHelper.scheduleAlarm(task);
                break;
        }
    }

    public void cancelTaskReminder(Task task) {
        // WorkManager'daki işi iptal et
        WorkManager.getInstance(context)
                .cancelAllWorkByTag("task_" + task.getId());

        // Alarmı iptal et
        alarmHelper.cancelAlarm(task);

        // Bildirimi iptal et
        notificationHelper.cancelNotification();
    }

    public void updateTaskReminder(Task task) {
        // Önce mevcut hatırlatıcıları iptal et
        cancelTaskReminder(task);
        
        // Yeni hatırlatıcıyı planla
        scheduleTaskReminder(task);
    }
} 