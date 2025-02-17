package com.tahtalı.hatirlatik.presentation.calendar;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.tahtalı.hatirlatik.data.repository.TaskRepository;
import com.tahtalı.hatirlatik.domain.model.Task;
import java.util.Calendar;
import java.util.List;

public class CalendarViewModel extends AndroidViewModel {
    private final TaskRepository taskRepository;
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<Task>> tasksForSelectedDate;

    public CalendarViewModel(Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        
        tasksForSelectedDate = Transformations.switchMap(selectedDate, date -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
            
            // Günün başlangıcı
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long startOfDay = calendar.getTimeInMillis();
            
            // Günün sonu
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            long endOfDay = calendar.getTimeInMillis();
            
            return taskRepository.getTasksBetweenDates(startOfDay, endOfDay);
        });

        // Başlangıçta bugünün tarihini ayarla
        selectedDate.setValue(System.currentTimeMillis());
    }

    public void setSelectedDate(long date) {
        selectedDate.setValue(date);
    }

    public LiveData<List<Task>> getTasksForSelectedDate() {
        return tasksForSelectedDate;
    }
} 