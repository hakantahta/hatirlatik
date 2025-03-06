package com.tht.hatirlatik.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.notification.AlarmHelper;
import com.tht.hatirlatik.notification.NotificationHelper;
import com.tht.hatirlatik.notification.TaskNotificationManager;
import com.tht.hatirlatik.repository.TaskRepository;
import com.tht.hatirlatik.workers.TaskWorker;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_COMPLETE_TASK = "com.tht.hatirlatik.ACTION_COMPLETE_TASK";
    public static final String ACTION_REMIND_LATER = "com.tht.hatirlatik.ACTION_REMIND_LATER";
    
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra("taskId", -1);
        
        if (taskId == -1) {
            return;
        }
        
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        
        // Rutin görevler için benzersiz ID kontrolü
        String uniqueTaskId = intent.getStringExtra("uniqueTaskId");
        boolean isRoutineTask = uniqueTaskId != null && !uniqueTaskId.isEmpty();
        
        TaskRepository taskRepository = new TaskRepository((Application) context.getApplicationContext());
        NotificationHelper notificationHelper = new NotificationHelper(context);
        AlarmHelper alarmHelper = new AlarmHelper(context);
        
        switch (action) {
            case ACTION_COMPLETE_TASK:
                if (isRoutineTask) {
                    // Rutin görev için özel işleme
                    handleCompleteRoutineTask(context, taskRepository, notificationHelper, alarmHelper, taskId, uniqueTaskId);
                } else {
                    // Normal görev için standart işleme
                    handleCompleteTask(context, taskRepository, notificationHelper, alarmHelper, taskId);
                }
                break;
                
            case ACTION_REMIND_LATER:
                if (isRoutineTask) {
                    // Rutin görev için özel işleme
                    handleRemindLaterRoutineTask(context, taskRepository, alarmHelper, taskId, 
                            intent.getStringExtra("taskTitle"), 
                            intent.getStringExtra("taskDescription"),
                            uniqueTaskId);
                } else {
                    // Normal görev için standart işleme
                    handleRemindLater(context, taskRepository, alarmHelper, taskId, 
                            intent.getStringExtra("taskTitle"), 
                            intent.getStringExtra("taskDescription"));
                }
                break;
        }
    }
    
    private void handleCompleteTask(Context context, TaskRepository taskRepository, 
                                   NotificationHelper notificationHelper, AlarmHelper alarmHelper, long taskId) {
        executor.execute(() -> {
            // Görevi veritabanından al
            LiveData<Task> taskLiveData = taskRepository.getTaskById(taskId);
            
            // LiveData'yı gözlemle
            Observer<Task> observer = new Observer<Task>() {
                @Override
                public void onChanged(Task task) {
                    if (task != null) {
                        // Alarmı ve titreşimi hemen durdur
                        AlarmReceiver.stopAlarmSound();
                        AlarmReceiver.stopVibration();
                        
                        // Alarmı iptal et
                        alarmHelper.cancelAlarm(task);
                        
                        // Görevi tamamlandı olarak işaretle
                        task.setCompleted(true);
                        taskRepository.updateTask(task, new TaskRepository.OnTaskOperationCallback() {
                            @Override
                            public void onSuccess(long taskId) {
                                // Bildirimi iptal et
                                notificationHelper.cancelNotification(taskId);
                                
                                // Kullanıcıya bildirim göster
                                handler.post(() -> {
                                    Toast.makeText(context, "Görev tamamlandı olarak işaretlendi", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                handler.post(() -> {
                                    Toast.makeText(context, "Görev güncellenirken hata oluştu", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                    
                    // Gözlemciyi kaldır
                    taskLiveData.removeObserver(this);
                }
            };
            
            // Gözlemciyi ekle
            handler.post(() -> {
                taskLiveData.observeForever(observer);
            });
        });
    }
    
    /**
     * Rutin görevler için tamamlama işlemi
     * Bu metod, sadece belirli bir günün görevini tamamlar, diğer günleri etkilemez
     */
    private void handleCompleteRoutineTask(Context context, TaskRepository taskRepository, 
                                         NotificationHelper notificationHelper, AlarmHelper alarmHelper, 
                                         long taskId, String uniqueTaskId) {
        executor.execute(() -> {
            // Görevi veritabanından al
            LiveData<Task> taskLiveData = taskRepository.getTaskById(taskId);
            
            // LiveData'yı gözlemle
            Observer<Task> observer = new Observer<Task>() {
                @Override
                public void onChanged(Task task) {
                    if (task != null) {
                        // Alarmı ve titreşimi hemen durdur
                        AlarmReceiver.stopAlarmSound();
                        AlarmReceiver.stopVibration();
                        
                        // Alarmı iptal et
                        alarmHelper.cancelAlarm(task);
                        
                        // Sadece bu bildirim için tamamlandı olarak işaretle
                        // Ana görevi güncelleme, sadece bu günün bildirimini kapat
                        
                        // Benzersiz ID ile bildirimi iptal et
                        notificationHelper.cancelNotificationByUniqueId(uniqueTaskId);
                        
                        // Kullanıcıya bildirim göster
                        handler.post(() -> {
                            Toast.makeText(context, "Bugünkü görev tamamlandı olarak işaretlendi", Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    // Gözlemciyi kaldır
                    taskLiveData.removeObserver(this);
                }
            };
            
            // Gözlemciyi ekle
            handler.post(() -> {
                taskLiveData.observeForever(observer);
            });
        });
    }
    
    private void handleRemindLater(Context context, TaskRepository taskRepository, 
                                  AlarmHelper alarmHelper, long taskId, 
                                  String taskTitle, String taskDescription) {
        executor.execute(() -> {
            // Görevi veritabanından al
            LiveData<Task> taskLiveData = taskRepository.getTaskById(taskId);
            
            // LiveData'yı gözlemle
            Observer<Task> observer = new Observer<Task>() {
                @Override
                public void onChanged(Task task) {
                    if (task != null) {
                        // Mevcut bildirimi kapat
                        new NotificationHelper(context).cancelNotification(taskId);
                        
                        // Mevcut alarmı durdur
                        alarmHelper.cancelAlarm(task);
                        
                        // 10 dakika sonrası için yeni bir zaman ayarla
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, 10);
                        Date newDateTime = calendar.getTime();
                        
                        // Görevi güncelle
                        task.setDateTime(newDateTime);
                        task.setReminderMinutes(0); // Anında bildirim
                        
                        taskRepository.updateTask(task, new TaskRepository.OnTaskOperationCallback() {
                            @Override
                            public void onSuccess(long taskId) {
                                // Yeni hatırlatıcıyı planla
                                TaskNotificationManager notificationManager = new TaskNotificationManager(context);
                                notificationManager.scheduleTaskReminder(task);
                                
                                // Widget'ı güncelle - iki farklı yöntemle
                                updateWidgets(context);
                                
                                handler.post(() -> {
                                    Toast.makeText(context, "Görev 10 dakika sonra tekrar hatırlatılacak", 
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                handler.post(() -> {
                                    Toast.makeText(context, "Görev güncellenirken hata: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    } else {
                        // Eğer görev veritabanında bulunamazsa, bildirim bilgilerini kullanarak yeni bir hatırlatma oluştur
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, 10);
                        
                        Task newTask = new Task();
                        newTask.setId(taskId);
                        newTask.setTitle(taskTitle);
                        newTask.setDescription(taskDescription);
                        newTask.setDateTime(calendar.getTime());
                        newTask.setReminderMinutes(0);
                        newTask.setNotificationType(NotificationType.NOTIFICATION);
                        
                        taskRepository.insertTask(newTask, new TaskRepository.OnTaskOperationCallback() {
                            @Override
                            public void onSuccess(long newTaskId) {
                                newTask.setId(newTaskId);
                                // Yeni hatırlatıcıyı planla
                                TaskNotificationManager notificationManager = new TaskNotificationManager(context);
                                notificationManager.scheduleTaskReminder(newTask);
                                
                                // Widget'ı güncelle - iki farklı yöntemle
                                updateWidgets(context);
                                
                                handler.post(() -> {
                                    Toast.makeText(context, "Görev 10 dakika sonra tekrar hatırlatılacak", 
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                handler.post(() -> {
                                    Toast.makeText(context, "Yeni görev oluşturulurken hata: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                    
                    // Observer'ı kaldır
                    taskLiveData.removeObserver(this);
                }
            };
            
            // Main thread'de observer'ı ekle
            handler.post(() -> taskLiveData.observeForever(observer));
        });
    }
    
    /**
     * Rutin görevler için sonra hatırlatma işlemi
     */
    private void handleRemindLaterRoutineTask(Context context, TaskRepository taskRepository, 
                                            AlarmHelper alarmHelper, long taskId, 
                                            String taskTitle, String taskDescription,
                                            String uniqueTaskId) {
        executor.execute(() -> {
            // Alarmı ve titreşimi hemen durdur
            AlarmReceiver.stopAlarmSound();
            AlarmReceiver.stopVibration();
            
            // Bildirimi iptal et
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.cancelNotificationByUniqueId(uniqueTaskId);
            
            // 30 dakika sonra tekrar hatırlat
            int delayMinutes = 30;
            
            // WorkManager için input data oluştur
            Data inputData = new Data.Builder()
                    .putLong("taskId", taskId)
                    .putString("taskTitle", taskTitle)
                    .putString("taskDescription", taskDescription)
                    .putString("notificationType", "NOTIFICATION")
                    .putString("uniqueTaskId", uniqueTaskId) // Benzersiz ID'yi ekle
                    .build();

            // WorkRequest oluştur - benzersiz bir tag kullan
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                    .setInputData(inputData)
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .addTag("task_remind_later_" + uniqueTaskId) // Benzersiz tag kullan
                    .build();

            // Work'ü planla
            WorkManager.getInstance(context).enqueue(workRequest);
            
            // Kullanıcıya bildirim göster
            handler.post(() -> {
                Toast.makeText(context, delayMinutes + " dakika sonra tekrar hatırlatılacak", Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    // Widget'ı güncelleme yardımcı metodu
    private void updateWidgets(Context context) {
        try {
            // 1. Yöntem: Widget'ı doğrudan güncelle
            com.tht.hatirlatik.widget.TaskWidgetProvider.refreshWidget(context);
            
            // 2. Yöntem: Uygulama sınıfından güncelleme yap
            if (context.getApplicationContext() instanceof com.tht.hatirlatik.HatirlatikApplication) {
                com.tht.hatirlatik.HatirlatikApplication app = 
                    (com.tht.hatirlatik.HatirlatikApplication) context.getApplicationContext();
                app.updateWidgets();
            }
            
            // 3. Yöntem: Doğrudan tüm widget'ları güncelle
            com.tht.hatirlatik.widget.TaskWidgetProvider.updateAllWidgets(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 