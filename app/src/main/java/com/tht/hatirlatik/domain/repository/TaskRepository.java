package com.tht.hatirlatik.domain.repository;

import com.tht.hatirlatik.domain.model.Task;
import java.util.Date;
import java.util.List;

/**
 * Görev veritabanı işlemlerini tanımlayan repository interface'i
 */
public interface TaskRepository {
    // Temel CRUD operasyonları
    long insertTask(Task task);
    void updateTask(Task task);
    void deleteTask(long taskId);
    Task getTaskById(long taskId);
    List<Task> getAllTasks();

    // Özel sorgular
    List<Task> getTasksByDate(Date date);
    List<Task> getTasksByDateRange(Date startDate, Date endDate);
    List<Task> getCompletedTasks();
    List<Task> getIncompleteTasks();
    List<Task> getUpcomingTasks(int limit);
    List<Task> getTasksWithReminders();
    
    // İstatistiksel sorgular
    int getCompletedTaskCount();
    int getIncompleteTaskCount();
    int getTotalTaskCount();
} 