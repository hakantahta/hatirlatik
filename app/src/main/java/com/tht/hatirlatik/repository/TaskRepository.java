package com.tht.hatirlatik.repository;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.database.TaskDao;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.widget.TaskListWidget;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private final TaskDao taskDao;
    private final ExecutorService executorService;
    private final Context context;

    public TaskRepository(Application application) {
        this.context = application;
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, TaskListWidget.class));
        
        // Widget'ı güncelle
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
    }

    public void insertTask(Task task, OnTaskOperationCallback callback) {
        executorService.execute(() -> {
            try {
                long taskId = taskDao.insert(task);
                updateWidget(); // Widget'ı güncelle
                callback.onSuccess(taskId);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updateTask(Task task, OnTaskOperationCallback callback) {
        executorService.execute(() -> {
            try {
                taskDao.update(task);
                updateWidget(); // Widget'ı güncelle
                callback.onSuccess(task.getId());
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void deleteTask(Task task, OnTaskOperationCallback callback) {
        executorService.execute(() -> {
            try {
                taskDao.delete(task);
                updateWidget(); // Widget'ı güncelle
                callback.onSuccess(task.getId());
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void deleteCompletedTasks(OnTaskOperationCallback callback) {
        executorService.execute(() -> {
            try {
                taskDao.deleteCompletedTasks();
                updateWidget(); // Widget'ı güncelle
                callback.onSuccess(-1);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updateTaskCompletionStatus(long taskId, boolean isCompleted, OnTaskOperationCallback callback) {
        executorService.execute(() -> {
            try {
                taskDao.updateTaskCompletionStatus(taskId, isCompleted);
                updateWidget(); // Widget'ı güncelle
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(taskId);
                });
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError(e);
                });
            }
        });
    }

    public LiveData<List<Task>> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public LiveData<List<Task>> getActiveTasks() {
        return taskDao.getActiveTasks();
    }

    public LiveData<List<Task>> getTasksBetweenDates(Date startDate, Date endDate) {
        return taskDao.getTasksBetweenDates(startDate, endDate);
    }

    public LiveData<Task> getTaskById(long taskId) {
        return taskDao.getTaskById(taskId);
    }

    public void getOverdueTasks(Date date, TaskCallback callback) {
        executorService.execute(() -> {
            List<Task> tasks = taskDao.getOverdueTasks(date);
            callback.onTasksLoaded(tasks);
        });
    }

    public interface TaskCallback {
        void onTasksLoaded(List<Task> tasks);
    }

    public interface OnTaskOperationCallback {
        void onSuccess(long taskId);
        void onError(Exception e);
    }
} 