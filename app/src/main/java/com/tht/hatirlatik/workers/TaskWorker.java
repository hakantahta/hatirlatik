package com.tht.hatirlatik.workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.notification.AlarmHelper;
import com.tht.hatirlatik.notification.NotificationHelper;
import com.tht.hatirlatik.receivers.AlarmReceiver;

public class TaskWorker extends Worker {
    private static final String TAG = "TaskWorker";
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
                // Bildirim tipine göre işlem yap
                switch (notificationType) {
                    case NOTIFICATION:
                        // Sadece bildirim göster
                        Task notificationTask = new Task(taskTitle, taskDescription, null, 0, notificationType);
                        notificationTask.setId(taskId);
                        new NotificationHelper(context).showTaskNotification(notificationTask);
                        Log.d(TAG, "doWork: Bildirim gösterildi - taskId: " + taskId);
                        break;
                        
                    case ALARM:
                        // Alarm çal - NotificationHelper üzerinden bildirim gösterme
                        Log.d(TAG, "doWork: Alarm çalınıyor - taskId: " + taskId);
                        AlarmHelper alarmHelper = new AlarmHelper(context);
                        
                        // Doğrudan AlarmReceiver'ı tetikle
                        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                        alarmIntent.putExtra("taskId", taskId);
                        alarmIntent.putExtra("taskTitle", taskTitle);
                        alarmIntent.putExtra("taskDescription", taskDescription);
                        context.sendBroadcast(alarmIntent);
                        break;
                        
                    case NOTIFICATION_AND_ALARM:
                        // Hem bildirim göster hem de alarm çal
                        Task combinedTask = new Task(taskTitle, taskDescription, null, 0, notificationType);
                        combinedTask.setId(taskId);
                        
                        // Bildirim göster
                        new NotificationHelper(context).showTaskNotification(combinedTask);
                        
                        // Alarmı çal
                        Log.d(TAG, "doWork: Bildirim ve alarm - taskId: " + taskId);
                        Intent combinedIntent = new Intent(context, AlarmReceiver.class);
                        combinedIntent.putExtra("taskId", taskId);
                        combinedIntent.putExtra("taskTitle", taskTitle);
                        combinedIntent.putExtra("taskDescription", taskDescription);
                        context.sendBroadcast(combinedIntent);
                        break;
                }
                return Result.success();
            }
            return Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "doWork: Hata oluştu", e);
            e.printStackTrace();
            return Result.failure();
        }
    }
} 