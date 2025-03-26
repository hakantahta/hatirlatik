package com.tht.hatirlatik.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.database.TaskDao;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.notification.TaskNotificationManager;
import com.tht.hatirlatik.widget.TaskWidgetProvider;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Bu receiver, cihaz yeniden başlatıldığında tetiklenir ve
 * zamanlanmış görevleri ve widget'ları yeniden düzenler.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Cihaz yeniden başlatıldı");
        
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "onReceive: ACTION_BOOT_COMPLETED alındı, görevleri yeniden planlıyoruz");
            
            // Widget'ları güncelle
            TaskWidgetProvider.refreshWidget(context);
            TaskWidgetProvider.updateAllWidgets(context);
            
            // Aktif görevleri arkaplan thread'inde yükle ve yeniden planla
            executor.execute(() -> {
                try {
                    rescheduleActiveTasks(context);
                } catch (Exception e) {
                    Log.e(TAG, "onReceive: Görevleri yeniden planlarken hata oluştu", e);
                }
            });
        }
    }
    
    /**
     * Aktif ve gelecekteki görevleri yeniden planlar
     */
    private void rescheduleActiveTasks(Context context) {
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            if (database != null) {
                TaskDao taskDao = database.taskDao();
                
                // Şu andan sonraki görevleri al
                Calendar cal = Calendar.getInstance();
                Date now = cal.getTime();
                
                // Sadece gelecekteki ve tamamlanmamış görevleri al
                List<Task> futureTasks = taskDao.getTasksBetweenDatesSync(now, new Date(now.getTime() + (365 * 24 * 60 * 60 * 1000L))); // 1 yıl sonrası
                
                if (futureTasks != null && !futureTasks.isEmpty()) {
                    TaskNotificationManager notificationManager = new TaskNotificationManager(context);
                    
                    for (Task task : futureTasks) {
                        // Sadece tamamlanmamış görevleri planla
                        if (!task.isCompleted()) {
                            Log.d(TAG, "rescheduleActiveTasks: Görev yeniden planlanıyor: " + task.getTitle() + " (ID: " + task.getId() + ")");
                            notificationManager.scheduleTaskReminder(task);
                        }
                    }
                    
                    Log.d(TAG, "rescheduleActiveTasks: " + futureTasks.size() + " görev yeniden planlandı");
                } else {
                    Log.d(TAG, "rescheduleActiveTasks: Planlanacak görev bulunamadı");
                }
            } else {
                Log.e(TAG, "rescheduleActiveTasks: Veritabanı null");
            }
        } catch (Exception e) {
            Log.e(TAG, "rescheduleActiveTasks: Hata oluştu", e);
        }
    }
} 