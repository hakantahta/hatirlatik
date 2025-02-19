package com.tht.hatirlatik.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.notification.TaskNotificationManager;
import com.tht.hatirlatik.repository.TaskRepository;
import java.util.Date;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final TaskNotificationManager notificationManager;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public TaskViewModel(Application application) {
        super(application);
        repository = new TaskRepository(application);
        notificationManager = new TaskNotificationManager(application);
    }

    // Görev ekleme
    public void insertTask(Task task) {
        isLoading.setValue(true);
        repository.insertTask(task, new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
                task.setId(taskId);
                // Görevi ekledikten sonra hatırlatıcıyı planla
                notificationManager.scheduleTaskReminder(task);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Görev eklenirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Görev güncelleme
    public void updateTask(Task task) {
        isLoading.setValue(true);
        repository.updateTask(task, new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
                // Görevi güncelledikten sonra hatırlatıcıyı güncelle
                notificationManager.updateTaskReminder(task);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Görev güncellenirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Görev silme
    public void deleteTask(Task task) {
        isLoading.setValue(true);
        repository.deleteTask(task, new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
                // Görevi sildikten sonra hatırlatıcıyı iptal et
                notificationManager.cancelTaskReminder(task);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Görev silinirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Tamamlanmış görevleri silme
    public void deleteCompletedTasks() {
        isLoading.setValue(true);
        repository.deleteCompletedTasks(new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Tamamlanmış görevler silinirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Görev tamamlanma durumunu güncelleme
    public void updateTaskCompletionStatus(long taskId, boolean isCompleted) {
        isLoading.setValue(true);
        repository.updateTaskCompletionStatus(taskId, isCompleted, new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
                // Eğer görev tamamlandıysa hatırlatıcıyı iptal et
                if (isCompleted) {
                    repository.getTaskById(taskId).observeForever(task -> {
                        if (task != null) {
                            notificationManager.cancelTaskReminder(task);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Görev durumu güncellenirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Tüm görevleri getir
    public LiveData<List<Task>> getAllTasks() {
        return repository.getAllTasks();
    }

    // Aktif görevleri getir
    public LiveData<List<Task>> getActiveTasks() {
        return repository.getActiveTasks();
    }

    // Belirli tarih aralığındaki görevleri getir
    public LiveData<List<Task>> getTasksBetweenDates(Date startDate, Date endDate) {
        return repository.getTasksBetweenDates(startDate, endDate);
    }

    // Belirli bir görevi getir
    public LiveData<Task> getTaskById(long taskId) {
        return repository.getTaskById(taskId);
    }

    // Gecikmiş görevleri getir
    public void getOverdueTasks(Date date) {
        isLoading.setValue(true);
        repository.getOverdueTasks(date, tasks -> {
            isLoading.postValue(false);
            // Burada gecikmiş görevler için bildirim veya UI güncellemesi yapılabilir
        });
    }

    // Hata mesajını getir
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Yükleme durumunu getir
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
} 