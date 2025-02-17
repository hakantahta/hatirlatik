package com.tahtalı.hatirlatik.presentation.task;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.tahtalı.hatirlatik.data.repository.TaskRepository;
import com.tahtalı.hatirlatik.domain.model.Task;
import com.tahtalı.hatirlatik.worker.ReminderWorker;
import java.util.concurrent.TimeUnit;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository taskRepository;
    private final WorkManager workManager;

    public TaskViewModel(Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        workManager = WorkManager.getInstance(application);
    }

    public void addTask(Task task) {
        taskRepository.insertTask(task);
        scheduleTaskReminder(task);
    }

    private void scheduleTaskReminder(Task task) {
        long currentTime = System.currentTimeMillis();
        long taskTime = task.getDateTime();
        long delayInMillis = taskTime - currentTime;

        if (delayInMillis <= 0) {
            return; // Geçmiş tarihli görevler için hatırlatıcı ayarlanmaz
        }

        Data inputData = new Data.Builder()
                .putLong("task_id", task.getId())
                .putString("task_title", task.getTitle())
                .putString("task_description", task.getDescription())
                .build();

        OneTimeWorkRequest reminderWork = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        workManager.enqueue(reminderWork);
    }
} 