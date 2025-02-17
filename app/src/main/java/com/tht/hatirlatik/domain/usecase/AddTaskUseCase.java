package com.tht.hatirlatik.domain.usecase;

import com.tht.hatirlatik.domain.model.Task;
import com.tht.hatirlatik.domain.repository.TaskRepository;

/**
 * Yeni görev ekleme işlemini yöneten UseCase sınıfı
 */
public class AddTaskUseCase {
    private final TaskRepository taskRepository;

    public AddTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public long execute(Task task) {
        // Validasyon kontrolleri
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Görev başlığı boş olamaz");
        }

        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Görev tarihi boş olamaz");
        }

        // Görevi veritabanına ekle
        return taskRepository.insertTask(task);
    }
} 