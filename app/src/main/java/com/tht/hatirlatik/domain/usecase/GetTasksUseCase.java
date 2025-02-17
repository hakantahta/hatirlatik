package com.tht.hatirlatik.domain.usecase;

import com.tht.hatirlatik.domain.model.Task;
import com.tht.hatirlatik.domain.repository.TaskRepository;
import java.util.Date;
import java.util.List;

/**
 * Görev listeleme işlemlerini yöneten UseCase sınıfı
 */
public class GetTasksUseCase {
    private final TaskRepository taskRepository;

    public GetTasksUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.getAllTasks();
    }

    public List<Task> getTasksByDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Tarih boş olamaz");
        }
        return taskRepository.getTasksByDate(date);
    }

    public List<Task> getTasksByDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Başlangıç ve bitiş tarihleri boş olamaz");
        }
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Başlangıç tarihi bitiş tarihinden sonra olamaz");
        }
        return taskRepository.getTasksByDateRange(startDate, endDate);
    }

    public List<Task> getCompletedTasks() {
        return taskRepository.getCompletedTasks();
    }

    public List<Task> getIncompleteTasks() {
        return taskRepository.getIncompleteTasks();
    }

    public List<Task> getUpcomingTasks(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit 0'dan büyük olmalıdır");
        }
        return taskRepository.getUpcomingTasks(limit);
    }

    public List<Task> getTasksWithReminders() {
        return taskRepository.getTasksWithReminders();
    }
} 