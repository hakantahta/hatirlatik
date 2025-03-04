package com.tht.hatirlatik.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.TaskFilter;
import com.tht.hatirlatik.notification.TaskNotificationManager;
import com.tht.hatirlatik.repository.TaskRepository;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final TaskNotificationManager notificationManager;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<TaskFilter> currentFilter = new MutableLiveData<>(TaskFilter.ALL);
    private final MediatorLiveData<List<Task>> filteredTasks = new MediatorLiveData<>();
    private final LiveData<List<Task>> allTasks;
    private final MutableLiveData<Boolean> checkboxState = new MutableLiveData<>();
    private final MutableLiveData<Long> lastCheckedTaskId = new MutableLiveData<>();
    private final MutableLiveData<List<Task>> taskListLiveData = new MutableLiveData<>();

    public TaskViewModel(Application application) {
        super(application);
        repository = new TaskRepository(application);
        notificationManager = new TaskNotificationManager(application);
        allTasks = repository.getAllTasks();

        // Görevleri ve filtreyi gözlemle
        filteredTasks.addSource(allTasks, tasks -> 
            filterTasks(tasks, currentFilter.getValue()));
        
        filteredTasks.addSource(currentFilter, filter -> 
            filterTasks(allTasks.getValue(), filter));
    }

    private void filterTasks(List<Task> tasks, TaskFilter filter) {
        if (tasks == null) {
            filteredTasks.setValue(null);
            return;
        }

        if (filter == null) {
            filter = TaskFilter.ALL;
        }

        try {
            TaskFilter finalFilter = filter;
            List<Task> filtered = tasks.stream()
                    .filter(task -> {
                        switch (finalFilter) {
                            case ALL:
                                return true;
                            case ACTIVE:
                                return !task.isCompleted();
                            case COMPLETED:
                                return task.isCompleted();
                            default:
                                return true;
                        }
                    })
                    .collect(Collectors.toList());
            
            filteredTasks.setValue(filtered);
        } catch (Exception e) {
            errorMessage.setValue("Görevler filtrelenirken bir hata oluştu: " + e.getMessage());
            filteredTasks.setValue(tasks); // Hata durumunda tüm görevleri göster
        }
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
                
                // Widget'ı güncelle - daha güçlü bir şekilde
                try {
                    // Widget'ı doğrudan güncelle
                    com.tht.hatirlatik.widget.TaskWidgetProvider.refreshWidget(getApplication());
                    
                    // Ayrıca uygulama sınıfından da güncelleme yap
                    if (getApplication() instanceof com.tht.hatirlatik.HatirlatikApplication) {
                        com.tht.hatirlatik.HatirlatikApplication app = 
                            (com.tht.hatirlatik.HatirlatikApplication) getApplication();
                        app.updateWidgets();
                    }
                    
                    // Doğrudan tüm widget'ları güncelle
                    com.tht.hatirlatik.widget.TaskWidgetProvider.updateAllWidgets(getApplication());
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                // Görevi güncelledikten sonra hatırlatıcıyı yeniden planla
                notificationManager.cancelTaskReminder(task);
                notificationManager.scheduleTaskReminder(task);
                // Widget'ı güncelle
                updateWidgets();
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
                // Widget'ı güncelle
                updateWidgets();
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Görev silinirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Tamamlanan görevleri silme
    public void deleteCompletedTasks() {
        isLoading.setValue(true);
        repository.deleteCompletedTasks(new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
                // Widget'ı güncelle
                updateWidgets();
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Tamamlanan görevler silinirken bir hata oluştu: " + e.getMessage());
            }
        });
    }

    // Görev durumunu güncelleme
    public void updateTaskCompletionStatus(long taskId, boolean isCompleted) {
        isLoading.setValue(true);
        repository.updateTaskCompletionStatus(taskId, isCompleted, new TaskRepository.OnTaskOperationCallback() {
            @Override
            public void onSuccess(long taskId) {
                isLoading.postValue(false);
                // Görev durumu değiştiğinde hatırlatıcıyı güncelle
                if (isCompleted) {
                    // Görevi ID'ye göre al ve bildirimini iptal et
                    Task task = repository.getTaskByIdSync(taskId);
                    if (task != null) {
                        notificationManager.cancelTaskReminder(task);
                    }
                } else {
                    // Görevi ID'ye göre al ve bildirimini planla
                    Task task = repository.getTaskByIdSync(taskId);
                    if (task != null) {
                        notificationManager.scheduleTaskReminder(task);
                    }
                }
                // Son işaretlenen görev ID'sini güncelle
                lastCheckedTaskId.postValue(taskId);
                checkboxState.postValue(isCompleted);
                // Widget'ı güncelle
                updateWidgets();
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

    public void setFilter(TaskFilter filter) {
        if (filter != null) {
            currentFilter.setValue(filter);
        }
    }

    public LiveData<List<Task>> getTasks() {
        return filteredTasks;
    }

    public TaskFilter getCurrentFilter() {
        return currentFilter.getValue();
    }

    public void refreshTasks() {
        isLoading.setValue(true);
        // Repository'den güncel verileri çek
        repository.getAllTasks().observeForever(tasks -> {
            if (tasks != null) {
                // Mevcut filtreyi al ve uygula
                TaskFilter currentFilterValue = currentFilter.getValue();
                filterTasks(tasks, currentFilterValue);
                taskListLiveData.setValue(filteredTasks.getValue()); // UI'yi güncelle
            }
            isLoading.setValue(false);
            // Observer'ı kaldır
            repository.getAllTasks().removeObserver(taskListObserver -> {});
        });
    }

    public LiveData<Boolean> getCheckboxState() {
        return checkboxState;
    }

    public LiveData<Long> getLastCheckedTaskId() {
        return lastCheckedTaskId;
    }

    public void setCheckboxState(long taskId, boolean state) {
        lastCheckedTaskId.setValue(taskId);
        checkboxState.setValue(state);
    }

    public LiveData<List<Task>> getTaskListLiveData() {
        return taskListLiveData;
    }
    
    public void loadTasks() {
        isLoading.setValue(true);
        // Repository'den güncel verileri çek
        final LiveData<List<Task>> tasksLiveData = repository.getAllTasks();
        final androidx.lifecycle.Observer<List<Task>> observer = new androidx.lifecycle.Observer<List<Task>>() {
            @Override
            public void onChanged(List<Task> tasks) {
                if (tasks != null) {
                    // Mevcut filtreyi al ve uygula
                    TaskFilter currentFilterValue = currentFilter.getValue();
                    filterTasks(tasks, currentFilterValue);
                    // Takvim görünümünü güncelle
                    taskListLiveData.setValue(tasks);
                }
                isLoading.setValue(false);
                // Observer'ı kaldır
                tasksLiveData.removeObserver(this);
            }
        };
        tasksLiveData.observeForever(observer);
    }

    // Widget'ı güncelleme
    public void updateWidgets() {
        try {
            // Widget'ı doğrudan güncelle
            com.tht.hatirlatik.widget.TaskWidgetProvider.refreshWidget(getApplication());
            
            // Ayrıca uygulama sınıfından da güncelleme yap
            if (getApplication() instanceof com.tht.hatirlatik.HatirlatikApplication) {
                com.tht.hatirlatik.HatirlatikApplication app = 
                    (com.tht.hatirlatik.HatirlatikApplication) getApplication();
                app.updateWidgets();
            }
            
            // Doğrudan tüm widget'ları güncelle
            com.tht.hatirlatik.widget.TaskWidgetProvider.updateAllWidgets(getApplication());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 