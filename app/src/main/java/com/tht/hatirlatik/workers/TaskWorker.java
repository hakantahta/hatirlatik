package com.tht.hatirlatik.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.notification.AlarmHelper;
import com.tht.hatirlatik.notification.NotificationHelper;

public class TaskWorker extends Worker {
    private final Context context;

    public TaskWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Görev bilgilerini al
            String taskTitle = getInputData().getString("taskTitle");
            String taskDescription = getInputData().getString("taskDescription");
            long taskId = getInputData().getLong("taskId", -1);
            String notificationTypeStr = getInputData().getString("notificationType");
            
            // NotificationType null ise varsayılan olarak NOTIFICATION kullan
            NotificationType notificationType = NotificationType.NOTIFICATION;
            if (notificationTypeStr != null && !notificationTypeStr.isEmpty()) {
                try {
                    notificationType = NotificationType.valueOf(notificationTypeStr);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            if (taskId != -1) {
                Task task = new Task(taskTitle, taskDescription, null, 0, notificationType);
                task.setId(taskId);

                // Bildirim tipine göre işlem yap
                switch (notificationType) {
                    case NOTIFICATION:
                        new NotificationHelper(context).showTaskNotification(task);
                        break;
                    case ALARM:
                        // Önce mevcut alarmı iptal et (eğer varsa)
                        AlarmHelper alarmHelper = new AlarmHelper(context);
                        alarmHelper.cancelAlarm(task);
                        // Yeni alarmı planla
                        alarmHelper.scheduleAlarm(task);
                        break;
                    case NOTIFICATION_AND_ALARM:
                        // Önce mevcut alarmı iptal et (eğer varsa)
                        AlarmHelper alarmHelper2 = new AlarmHelper(context);
                        alarmHelper2.cancelAlarm(task);
                        // Bildirim göster ve yeni alarmı planla
                        new NotificationHelper(context).showTaskNotification(task);
                        alarmHelper2.scheduleAlarm(task);
                        break;
                }
                return Result.success();
            }
            return Result.failure();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
} 