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

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
        
        TaskRepository taskRepository = new TaskRepository((Application) context.getApplicationContext());
        NotificationHelper notificationHelper = new NotificationHelper(context);
        AlarmHelper alarmHelper = new AlarmHelper(context);
        
        switch (action) {
            case ACTION_COMPLETE_TASK:
                handleCompleteTask(context, taskRepository, notificationHelper, alarmHelper, taskId);
                break;
                
            case ACTION_REMIND_LATER:
                handleRemindLater(context, taskRepository, alarmHelper, taskId, 
                        intent.getStringExtra("taskTitle"), 
                        intent.getStringExtra("taskDescription"));
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
                        // Alarmı durdur
                        alarmHelper.cancelAlarm(task);
                        
                        // Görevi tamamlandı olarak işaretle
                        task.setCompleted(true);
                        taskRepository.updateTask(task, new TaskRepository.OnTaskOperationCallback() {
                            @Override
                            public void onSuccess(long taskId) {
                                // Bildirim güncelle ve kısa süre sonra kapat
                                notificationHelper.showTaskNotification(taskId, task.getTitle(), "Görev tamamlandı!");
                                
                                // Widget'ı güncelle - iki farklı yöntemle
                                updateWidgets(context);
                                
                                // 1 saniye sonra bildirimi kapat
                                handler.postDelayed(() -> {
                                    notificationHelper.cancelNotification(taskId);
                                    
                                    // UI thread'de Toast göster
                                    handler.post(() -> {
                                        Toast.makeText(context, "Görev tamamlandı: " + task.getTitle(), 
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }, 1000);
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                handler.post(() -> {
                                    Toast.makeText(context, "Görev güncellenirken hata: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                        
                        // Observer'ı kaldır
                        taskLiveData.removeObserver(this);
                    }
                }
            };
            
            // Main thread'de observer'ı ekle
            handler.post(() -> taskLiveData.observeForever(observer));
        });
    }
    
    private void handleRemindLater(Context context, TaskRepository taskRepository, 
                                  AlarmHelper alarmHelper, long taskId, String taskTitle, String taskDescription) {
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
                                
                                // UI thread'de Toast göster
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