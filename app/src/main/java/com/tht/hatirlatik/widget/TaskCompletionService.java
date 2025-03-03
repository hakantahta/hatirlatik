package com.tht.hatirlatik.widget;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.database.TaskDao;
import com.tht.hatirlatik.model.Task;

/**
 * Widget'tan görev tamamlama işlemini gerçekleştiren service sınıfı.
 */
public class TaskCompletionService extends IntentService {
    private static final String TAG = "TaskCompletionService";

    public TaskCompletionService() {
        super("TaskCompletionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            long taskId = intent.getLongExtra(TaskWidgetProvider.EXTRA_TASK_ID, -1);
            if (taskId != -1) {
                try {
                    // Görev durumunu değiştir
                    TaskDao taskDao = AppDatabase.getInstance(getApplicationContext()).taskDao();
                    
                    // Görevin mevcut durumunu al
                    Task task = taskDao.getTaskByIdSync(taskId);
                    if (task != null) {
                        // Durumu tersine çevir
                        boolean newStatus = !task.isCompleted();
                        taskDao.updateTaskCompletionStatus(taskId, newStatus);
                        
                        // Widget'ı güncelle
                        TaskWidgetProvider.updateAllWidgets(getApplicationContext());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Görev durumu güncellenirken hata oluştu", e);
                }
            }
        }
    }
} 