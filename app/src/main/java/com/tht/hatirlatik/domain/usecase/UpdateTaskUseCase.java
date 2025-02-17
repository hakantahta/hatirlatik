package com.tht.hatirlatik.domain.usecase;

import com.tht.hatirlatik.domain.model.Task;
import com.tht.hatirlatik.domain.repository.TaskRepository;

/**
 * Görev güncelleme işlemini yöneten UseCase sınıfı
 */
public class UpdateTaskUseCase {
    private final TaskRepository taskRepository;

    public UpdateTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(Task task) {
        // Validasyon kontrolleri
        if (task.getId() <= 0) {
            throw new IllegalArgumentException("Geçersiz görev ID'si");
        }

        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Görev başlığı boş olamaz");
        }

        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Görev tarihi boş olamaz");
        }

        // Görevi güncelle
        taskRepository.updateTask(task);
    }
} 