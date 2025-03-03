package com.tht.hatirlatik.repository;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;

import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.database.TaskDao;
import com.tht.hatirlatik.model.Task;

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

    public void insertTask(Task task, OnTaskOperationCallback callback) {
        executorService.execute(() -> {
            try {
                long taskId = taskDao.insert(task);
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
                callback.onSuccess(taskId);
            } catch (Exception e) {
                callback.onError(e);
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

    public Task getTaskByIdSync(long taskId) {
        return taskDao.getTaskByIdSync(taskId);
    }

    public void getOverdueTasks(Date date, OnTasksLoadedCallback callback) {
        executorService.execute(() -> {
            try {
                List<Task> tasks = taskDao.getOverdueTasks(date);
                callback.onTasksLoaded(tasks);
            } catch (Exception e) {
                e.printStackTrace();
                callback.onTasksLoaded(null);
            }
        });
    }

    public interface OnTaskOperationCallback {
        void onSuccess(long taskId);
        void onError(Exception e);
    }

    public interface OnTasksLoadedCallback {
        void onTasksLoaded(List<Task> tasks);
    }
} 