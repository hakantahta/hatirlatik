package com.tht.hatirlatik.domain.usecase;

import com.tht.hatirlatik.domain.model.Task;
import com.tht.hatirlatik.domain.repository.TaskRepository;
import java.util.Calendar;
import java.util.Date;

/**
 * Görev zamanlama işlemlerini yöneten UseCase sınıfı
 */
public class ScheduleTaskUseCase {
    private final TaskRepository taskRepository;

    public ScheduleTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(Task task) {
        // Validasyon kontrolleri
        if (task == null) {
            throw new IllegalArgumentException("Görev boş olamaz");
        }

        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Görev tarihi boş olamaz");
        }

        // Hatırlatıcı zamanını hesapla
        if (task.getReminderMinutesBefore() > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(task.getDueDate());
            calendar.add(Calendar.MINUTE, -task.getReminderMinutesBefore());
            task.setReminderTime(calendar.getTime());
        }

        // Şu anki zamandan önce bir hatırlatma zamanı ayarlanmışsa hata ver
        if (task.getReminderTime() != null && task.getReminderTime().before(new Date())) {
            throw new IllegalArgumentException("Hatırlatma zamanı geçmiş bir zaman olamaz");
        }

        // Görevi güncelle
        if (task.getId() > 0) {
            taskRepository.updateTask(task);
        } else {
            taskRepository.insertTask(task);
        }
    }

    public void cancelSchedule(long taskId) {
        // Validasyon kontrolü
        if (taskId <= 0) {
            throw new IllegalArgumentException("Geçersiz görev ID'si");
        }

        Task task = taskRepository.getTaskById(taskId);
        if (task != null) {
            task.setReminderTime(null);
            task.setReminderMinutesBefore(0);
            taskRepository.updateTask(task);
        }
    }
} 