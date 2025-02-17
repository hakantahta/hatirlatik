package com.tahtalı.hatirlatik.presentation.home;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.tahtalı.hatirlatik.data.repository.TaskRepository;
import com.tahtalı.hatirlatik.domain.model.Task;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private final TaskRepository taskRepository;
    private final LiveData<List<Task>> tasks;

    public HomeViewModel(Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        tasks = taskRepository.getAllTasks();
    }

    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        taskRepository.insertTask(task);
    }

    public void deleteTask(Task task) {
        taskRepository.deleteTask(task);
    }

    public void updateTask(Task task) {
        taskRepository.updateTask(task);
    }
} 