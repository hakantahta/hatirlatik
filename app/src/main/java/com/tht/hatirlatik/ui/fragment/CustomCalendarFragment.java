package com.tht.hatirlatik.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.CalendarMonth;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.DayBinder;
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder;
import com.kizitonwose.calendarview.ui.ViewContainer;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.ui.adapter.TaskAdapter;
import com.tht.hatirlatik.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomCalendarFragment extends Fragment implements TaskAdapter.TaskItemListener {

    private TaskViewModel viewModel;
    private TaskAdapter adapter;
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private MaterialButton addTaskButton;
    private RecyclerView tasksRecyclerView;
    private View emptyStateView;
    private CircularProgressIndicator progressBar;
    
    private LocalDate selectedDate = LocalDate.now();
    private static final Locale locale = new Locale("tr");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    private static final DateTimeFormatter monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale);
    
    private final List<LocalDate> datesWithTasks = new ArrayList<>();
    private final List<LocalDate> datesWithCompletedTasks = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Menu'yü etkinleştir
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_customCalendarFragment_to_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_custom_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View'ları başlat
        initViews(view);

        // ViewModel'i başlat
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Adapter'ı başlat
        setupRecyclerView();

        // CalendarView'ı ayarla
        setupCalendarView();

        // Görev ekleme butonunu ayarla
        setupAddTaskButton();

        // Observer'ları ayarla
        setupObservers();

        // Başlangıçta bugünün tarihini göster
        updateSelectedDateUI();
        
        // Takvim görünümünü hemen göster, görevleri arka planda yükle
        progressBar.setVisibility(View.VISIBLE);
        
        // Başlangıçta sadece mevcut ay için görevleri yükle (lazy loading)
        loadTasksForVisibleMonths(YearMonth.now());
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        selectedDateTextView = view.findViewById(R.id.text_selected_date);
        addTaskButton = view.findViewById(R.id.button_add_task_for_date);
        tasksRecyclerView = view.findViewById(R.id.recycler_view_tasks_for_date);
        emptyStateView = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        tasksRecyclerView.setAdapter(adapter);
    }

    private void setupCalendarView() {
        // Takvim görünümünü ayarla
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(12);  // 12 ay öncesi
        YearMonth endMonth = currentMonth.plusMonths(12);     // 12 ay sonrası
        
        // Haftanın ilk gününü Pazartesi olarak ayarla
        DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();
        
        calendarView.setup(startMonth, endMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);
        
        // Animasyonlu geçiş için ayarlar
        calendarView.setMonthScrollListener(calendarMonth -> {
            // Ay değiştiğinde animasyon
            YearMonth yearMonth = calendarMonth.getYearMonth();
            calendarView.post(() -> {
                calendarView.smoothScrollToMonth(yearMonth);
            });
            
            // Görünen ay değiştiğinde sadece o ay ve komşu aylar için görevleri yükle
            loadTasksForVisibleMonths(yearMonth);
            
            return null;
        });
        
        // Gün binder'ını ayarla
        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, CalendarDay day) {
                TextView textView = container.textView;
                View indicator = container.indicator;
                textView.setText(String.valueOf(day.getDate().getDayOfMonth()));
                
                if (day.getOwner() == DayOwner.THIS_MONTH) {
                    textView.setVisibility(View.VISIBLE);
                    
                    // Her gün için göstergeyi görünür yap
                    indicator.setVisibility(View.VISIBLE);
                    
                    // Bugünün tarihini vurgula
                    if (day.getDate().equals(LocalDate.now())) {
                        textView.setBackgroundResource(R.drawable.bg_today);
                        textView.setTextColor(getResources().getColor(R.color.white, null));
                    } else if (day.getDate().equals(selectedDate)) {
                        // Seçili tarihi vurgula
                        textView.setBackgroundResource(R.drawable.bg_selected_day);
                        textView.setTextColor(getResources().getColor(R.color.white, null));
                    } else {
                        // Normal günler
                        textView.setBackgroundResource(0);
                        textView.setTextColor(getResources().getColor(R.color.black, null));
                    }
                    
                    // Görev durumuna göre gösterge rengini ayarla
                    if (datesWithCompletedTasks.contains(day.getDate())) {
                        // Tamamlanmış görevleri olan günler - kırmızı
                        indicator.setBackgroundResource(R.drawable.circle_indicator_completed);
                    } else if (datesWithTasks.contains(day.getDate())) {
                        // Tamamlanmamış görevleri olan günler - yeşil
                        indicator.setBackgroundResource(R.drawable.circle_indicator_exists);
                    } else {
                        // Görevi olmayan günler - sarı
                        indicator.setBackgroundResource(R.drawable.circle_indicator_none);
                    }
                    
                    // Tıklama olayını ayarla
                    container.view.setOnClickListener(v -> {
                        // Önceki seçili günün arka planını temizle
                        if (selectedDate != day.getDate()) {
                            LocalDate oldDate = selectedDate;
                            selectedDate = day.getDate();
                            
                            // Animasyonlu geçiş
                            container.view.animate()
                                .scaleX(0.9f)
                                .scaleY(0.9f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    container.view.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100)
                                        .start();
                                    
                                    calendarView.notifyDateChanged(oldDate);
                                    calendarView.notifyDateChanged(day.getDate());
                                    updateSelectedDateUI();
                                    filterTasksForSelectedDate();
                                })
                                .start();
                        }
                    });
                } else {
                    // Ay dışındaki günleri gizle
                    textView.setVisibility(View.INVISIBLE);
                    indicator.setVisibility(View.INVISIBLE);
                    container.view.setOnClickListener(null);
                }
            }
        });
        
        // Ay başlığı binder'ını ayarla
        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<MonthViewContainer>() {
            @Override
            public MonthViewContainer create(@NonNull View view) {
                return new MonthViewContainer(view);
            }

            @Override
            public void bind(@NonNull MonthViewContainer container, CalendarMonth month) {
                TextView textView = container.textView;
                textView.setText(monthTitleFormatter.format(month.getYearMonth()));
                
                // Haftanın günlerini ayarla
                container.legendLayout.removeAllViews();
                for (int i = 0; i < 7; i++) {
                    DayOfWeek dayOfWeek = firstDayOfWeek.plus(i);
                    TextView legend = (TextView) LayoutInflater.from(requireContext())
                            .inflate(R.layout.calendar_day_legend, container.legendLayout, false);
                    legend.setText(dayOfWeek.getDisplayName(TextStyle.SHORT, locale));
                    container.legendLayout.addView(legend);
                }
            }
        });
    }

    private void setupAddTaskButton() {
        addTaskButton.setOnClickListener(v -> {
            // Seçilen tarihi bundle ile TaskFormFragment'a gönder
            Bundle args = new Bundle();
            args.putLong("selectedDate", 
                    Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_customCalendarFragment_to_taskForm, args);
        });
    }

    private void setupObservers() {
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                // Görevleri işle ve takvimde işaretle
                updateCalendarWithTasks(tasks);
                filterTasksForSelectedDate();
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isLoading) {
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
        
        // Görev durumu değiştiğinde listeyi güncelle
        viewModel.getTaskListLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                updateCalendarWithTasks(tasks);
                filterTasksForSelectedDate();
            }
        });
        
        // Son işlenen görev ID'sini ve durumunu izle
        viewModel.getLastCheckedTaskId().observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null && taskId > 0) {
                // Takvimi yenile
                calendarView.notifyCalendarChanged();
            }
        });
    }

    private void updateCalendarWithTasks(List<Task> tasks) {
        // Görev listelerini temizle
        datesWithTasks.clear();
        datesWithCompletedTasks.clear();
        
        // Görevleri tarihe göre grupla
        Map<LocalDate, List<Task>> tasksByDate = new HashMap<>();
        
        // Önce tüm görevleri tarihlere göre grupla
        for (Task task : tasks) {
            // Task tarihini LocalDate'e dönüştür
            LocalDate taskDate = task.getDateTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            if (!tasksByDate.containsKey(taskDate)) {
                tasksByDate.put(taskDate, new ArrayList<>());
            }
            tasksByDate.get(taskDate).add(task);
        }
        
        // Her tarih için görev durumunu kontrol et
        for (Map.Entry<LocalDate, List<Task>> entry : tasksByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Task> tasksForDate = entry.getValue();
            
            if (tasksForDate.isEmpty()) {
                continue;
            }
            
            boolean allCompleted = true;
            for (Task task : tasksForDate) {
                if (!task.isCompleted()) {
                    allCompleted = false;
                    break;
                }
            }
            
            if (allCompleted && !tasksForDate.isEmpty()) {
                // Tüm görevler tamamlandıysa kırmızı nokta
                datesWithCompletedTasks.add(date);
            } else {
                // Tamamlanmamış görevler varsa yeşil nokta
                datesWithTasks.add(date);
            }
        }
        
        // Takvimi güncelle
        calendarView.notifyCalendarChanged();
    }

    private void updateSelectedDateUI() {
        String formattedDate = selectedDate.format(dateFormatter);
        selectedDateTextView.setText(getString(R.string.tasks_for_date, formattedDate));
        addTaskButton.setText(getString(R.string.add_task));
    }

    private void filterTasksForSelectedDate() {
        List<Task> allTasks = viewModel.getTasks().getValue();
        if (allTasks == null) {
            showEmptyState();
            return;
        }

        // Seçilen tarih için başlangıç ve bitiş zamanlarını ayarla
        LocalDate startDate = selectedDate;
        LocalDate endDate = selectedDate.plusDays(1);
        
        Date startDateTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateTime = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Seçilen tarih için görevleri filtrele
        List<Task> tasksForDate = allTasks.stream()
                .filter(task -> {
                    Date taskDate = task.getDateTime();
                    return taskDate != null &&
                            !taskDate.before(startDateTime) &&
                            taskDate.before(endDateTime);
                })
                .collect(Collectors.toList());

        if (tasksForDate.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            // Yeni bir liste oluştur ve adapter'a gönder
            adapter.submitList(null);
            adapter.submitList(new ArrayList<>(tasksForDate));
        }
    }

    private void showEmptyState() {
        emptyStateView.setVisibility(View.VISIBLE);
        tasksRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateView.setVisibility(View.GONE);
        tasksRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTaskClicked(Task task) {
        // Görev detaylarına git
        Bundle args = new Bundle();
        args.putLong("taskId", task.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_customCalendarFragment_to_taskDetail, args);
    }

    @Override
    public void onTaskCheckedChanged(Task task, boolean isChecked) {
        // Önce görevi güncelle
        task.setCompleted(isChecked);
        
        // UI'yi hemen güncelle (adapter'ı yenile)
        List<Task> currentList = new ArrayList<>(adapter.getCurrentList());
        adapter.submitList(null);
        adapter.submitList(currentList);
        
        // Arka planda veritabanını güncelle
        viewModel.updateTaskCompletionStatus(task.getId(), isChecked);
        
        // Görev durumu değiştiğinde Snackbar ile bildirim göster
        String message = isChecked ? 
            getString(R.string.task_marked_completed) : 
            getString(R.string.task_marked_active);
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskDeleteClicked(Task task) {
        // Görev silme işlemi
        viewModel.deleteTask(task);
    }
    
    // Gün görünümü için container sınıfı
    static class DayViewContainer extends ViewContainer {
        final TextView textView;
        final View indicator;
        final View view;

        DayViewContainer(View view) {
            super(view);
            this.view = view;
            this.textView = view.findViewById(R.id.calendarDayText);
            this.indicator = view.findViewById(R.id.taskIndicator);
        }
    }
    
    // Ay başlığı görünümü için container sınıfı
    static class MonthViewContainer extends ViewContainer {
        final TextView textView;
        final ViewGroup legendLayout;

        MonthViewContainer(View view) {
            super(view);
            this.textView = view.findViewById(R.id.headerTextView);
            this.legendLayout = view.findViewById(R.id.legendLayout);
        }
    }

    // Görünen aylar için görevleri yükle (lazy loading)
    private void loadTasksForVisibleMonths(YearMonth visibleMonth) {
        // Görünen ay ve komşu aylar için tarih aralığı oluştur
        LocalDate startDate = visibleMonth.minusMonths(1).atDay(1);
        LocalDate endDate = visibleMonth.plusMonths(1).atEndOfMonth();
        
        // Java Date nesnelerine dönüştür
        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        
        // Sadece bu tarih aralığındaki görevleri yükle
        viewModel.getTasksBetweenDates(start, end).observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                updateCalendarWithTasks(tasks);
                
                // Eğer seçili tarih bu aralıktaysa, o tarihe ait görevleri filtrele
                if (!selectedDate.isBefore(startDate) && !selectedDate.isAfter(endDate)) {
                    filterTasksForSelectedDate();
                }
            }
        });
    }
} 