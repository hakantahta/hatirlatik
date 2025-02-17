package com.tht.hatirlatik.domain.usecase;

import com.tht.hatirlatik.domain.repository.TaskRepository;

/**
 * Görev silme işlemini yöneten UseCase sınıfı
 */
public class DeleteTaskUseCase {
    private final TaskRepository taskRepository;

    public DeleteTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(long taskId) {
        // Validasyon kontrolü
        if (taskId <= 0) {
            throw new IllegalArgumentException("Geçersiz görev ID'si");
        }

        // Görevi sil
        taskRepository.deleteTask(taskId);
    }
} 