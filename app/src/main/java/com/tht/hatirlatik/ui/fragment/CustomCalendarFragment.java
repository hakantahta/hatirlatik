package com.tht.hatirlatik.ui.fragment;

import android.os.Bundle;
import android.util.Log;
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
import com.tht.hatirlatik.model.RepeatType;
import com.tht.hatirlatik.model.RoutineSettings;
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
                // Tüm görevleri al (rutin görevler dahil)
                List<Task> allTasks = new ArrayList<>(tasks);
                
                // Rutin görevleri de ekle
                for (Task task : tasks) {
                    if (task.getRepeatType() != null && task.getRepeatType() != RepeatType.NONE) {
                        // Rutin görevleri ekle
                        if (!allTasks.contains(task)) {
                            allTasks.add(task);
                        }
                    }
                }
                
                // Görevleri işle ve takvimde işaretle
                updateCalendarWithTasks(allTasks);
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
                // Tüm görevleri al (rutin görevler dahil)
                List<Task> allTasks = new ArrayList<>(tasks);
                
                // Rutin görevleri de ekle
                for (Task task : tasks) {
                    if (task.getRepeatType() != null && task.getRepeatType() != RepeatType.NONE) {
                        // Rutin görevleri ekle
                        if (!allTasks.contains(task)) {
                            allTasks.add(task);
                        }
                    }
                }
                
                updateCalendarWithTasks(allTasks);
            }
        });
        
        // Son işlenen görev ID'sini ve durumunu izle
        viewModel.getLastCheckedTaskId().observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null && taskId > 0) {
                // Takvimi yenile
                calendarView.notifyCalendarChanged();
                // Seçili tarihe göre görevleri filtrele
                filterTasksForSelectedDate();
            }
        });
    }

    private void updateCalendarWithTasks(List<Task> tasks) {
        // Görev listelerini temizle
        datesWithTasks.clear();
        datesWithCompletedTasks.clear();
        
        Log.d("CustomCalendarFragment", "updateCalendarWithTasks başladı: " + tasks.size() + " görev var");
        
        // Görevleri tarihe göre grupla
        Map<LocalDate, List<Task>> tasksByDate = new HashMap<>();
        
        // Önce tüm görevleri tarihlere göre grupla
        for (Task task : tasks) {
            // Task tarihini LocalDate'e dönüştür
            LocalDate taskDate = task.getDateTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            Log.d("CustomCalendarFragment", "Görev inceleniyor: " + task.getTitle() + ", tarih: " + taskDate + ", rutin tipi: " + task.getRepeatType());
            
            // Rutin görevleri kontrol et ve ilgili tarihleri ekle
            if (task.getRepeatType() != null && task.getRepeatType() != RepeatType.NONE) {
                // Rutin görev için tüm ilgili tarihleri ekle
                addRoutineTaskDates(task, tasksByDate);
            } else {
                // Normal görev için sadece kendi tarihini ekle
                if (!tasksByDate.containsKey(taskDate)) {
                    tasksByDate.put(taskDate, new ArrayList<>());
                }
                tasksByDate.get(taskDate).add(task);
            }
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
                Log.d("CustomCalendarFragment", "Tamamlanmış görevler için tarih eklendi: " + date);
            } else {
                // Tamamlanmamış görevler varsa yeşil nokta
                datesWithTasks.add(date);
                Log.d("CustomCalendarFragment", "Tamamlanmamış görevler için tarih eklendi: " + date);
            }
        }
        
        // Takvimi güncelle
        calendarView.notifyCalendarChanged();
        
        // Seçili tarihe göre görevleri filtrele
        filterTasksForSelectedDate();
    }

    /**
     * Rutin görevler için ilgili tarihleri ekler
     * @param task Rutin görev
     * @param tasksByDate Tarih-görev haritası
     */
    private void addRoutineTaskDates(Task task, Map<LocalDate, List<Task>> tasksByDate) {
        // Görevin başlangıç tarihini al
        LocalDate taskDate = task.getDateTime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        
        // Rutin ayarlarını senkron olarak al
        RoutineSettings routineSettings = viewModel.getRoutineSettingsByTaskIdSync(task.getId());
        if (routineSettings == null) {
            Log.e("CustomCalendarFragment", "Rutin ayarları bulunamadı: taskId=" + task.getId());
            return;
        }
        
        Log.d("CustomCalendarFragment", "Rutin görev işleniyor: " + task.getTitle() + ", rutin tipi: " + routineSettings.getRepeatType());
        
        // Tekrarlama tipine göre tarihleri ekle
        switch (routineSettings.getRepeatType()) {
            case DAILY:
                // Her gün için 30 gün boyunca ekle
                addDailyTaskDates(task, taskDate, tasksByDate, 30);
                break;
                
            case WEEKLY:
                // Haftanın belirli günleri için 8 hafta boyunca ekle
                addWeeklyTaskDates(task, taskDate, routineSettings.getWeekDays(), tasksByDate, 8);
                break;
                
            case MONTHLY:
                // Ayın belirli günleri için 6 ay boyunca ekle
                addMonthlyTaskDates(task, taskDate, routineSettings.getMonthDays(), tasksByDate, 6);
                break;
                
            case WEEKDAYS:
                // Hafta içi günler için 4 hafta boyunca ekle
                Log.d("CustomCalendarFragment", "Hafta içi görev için tarihleri ekliyorum: " + task.getTitle());
                addWeekdaysTaskDates(task, taskDate, tasksByDate, 4);
                break;
                
            case WEEKENDS:
                // Hafta sonu günler için 4 hafta boyunca ekle
                addWeekendsTaskDates(task, taskDate, tasksByDate, 4);
                break;
                
            case CUSTOM:
                // Özel tekrarlama ayarları için ekle
                addCustomTaskDates(task, taskDate, routineSettings, tasksByDate);
                break;
        }
    }
    
    /**
     * Günlük görevler için tarihleri ekler
     */
    private void addDailyTaskDates(Task task, LocalDate startDate, Map<LocalDate, List<Task>> tasksByDate, int days) {
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            addTaskToDate(task, date, tasksByDate);
        }
    }
    
    /**
     * Haftalık görevler için tarihleri ekler
     */
    private void addWeeklyTaskDates(Task task, LocalDate startDate, String weekDaysStr, 
                                   Map<LocalDate, List<Task>> tasksByDate, int weeks) {
        // Varsayılan olarak görevin oluşturulduğu günü kullan
        int[] weekDays = {startDate.getDayOfWeek().getValue()};
        
        // Eğer belirli günler belirtilmişse, onları kullan
        if (weekDaysStr != null && !weekDaysStr.isEmpty()) {
            String[] days = weekDaysStr.split(",");
            weekDays = new int[days.length];
            
            for (int i = 0; i < days.length; i++) {
                try {
                    weekDays[i] = Integer.parseInt(days[i].trim());
                } catch (NumberFormatException e) {
                    weekDays[i] = startDate.getDayOfWeek().getValue();
                }
            }
        }
        
        // Her hafta için belirtilen günleri ekle
        for (int week = 0; week < weeks; week++) {
            for (int day : weekDays) {
                // Haftanın gününü bul (1=Pazartesi, 7=Pazar)
                LocalDate date = startDate.with(DayOfWeek.of(day)).plusWeeks(week);
                
                // Eğer başlangıç tarihinden önceyse, bir sonraki haftaya geç
                if (date.isBefore(startDate)) {
                    date = date.plusWeeks(1);
                }
                
                addTaskToDate(task, date, tasksByDate);
            }
        }
    }
    
    /**
     * Aylık görevler için tarihleri ekler
     */
    private void addMonthlyTaskDates(Task task, LocalDate startDate, String monthDaysStr, 
                                    Map<LocalDate, List<Task>> tasksByDate, int months) {
        // Varsayılan olarak görevin oluşturulduğu günü kullan
        int[] monthDays = {startDate.getDayOfMonth()};
        
        // Eğer belirli günler belirtilmişse, onları kullan
        if (monthDaysStr != null && !monthDaysStr.isEmpty()) {
            String[] days = monthDaysStr.split(",");
            monthDays = new int[days.length];
            
            for (int i = 0; i < days.length; i++) {
                try {
                    monthDays[i] = Integer.parseInt(days[i].trim());
                } catch (NumberFormatException e) {
                    monthDays[i] = startDate.getDayOfMonth();
                }
            }
        }
        
        // Her ay için belirtilen günleri ekle
        for (int month = 0; month < months; month++) {
            for (int day : monthDays) {
                try {
                    // Ayın gününü bul
                    LocalDate date = startDate.withDayOfMonth(day).plusMonths(month);
                    
                    // Eğer başlangıç tarihinden önceyse, bir sonraki aya geç
                    if (date.isBefore(startDate)) {
                        date = date.plusMonths(1);
                    }
                    
                    addTaskToDate(task, date, tasksByDate);
                } catch (Exception e) {
                    // Geçersiz tarih (örn. 31 Şubat) durumunda atla
                    continue;
                }
            }
        }
    }
    
    /**
     * Hafta içi görevler için tarihleri ekler
     */
    private void addWeekdaysTaskDates(Task task, LocalDate startDate, 
                                     Map<LocalDate, List<Task>> tasksByDate, int weeks) {
        // Hafta içi günler (1=Pazartesi, 5=Cuma)
        int[] weekDays = {1, 2, 3, 4, 5};
        
        // Bugünün tarihini al
        LocalDate today = LocalDate.now();
        
        Log.d("CustomCalendarFragment", "addWeekdaysTaskDates başladı: task=" + task.getTitle() + ", startDate=" + startDate);
        
        // Başlangıç tarihini hafta başına ayarla (Pazartesi)
        LocalDate weekStart = startDate;
        if (startDate.getDayOfWeek().getValue() > 5) {
            // Eğer hafta sonu ise, bir sonraki haftanın Pazartesi'sine ayarla
            weekStart = startDate.plusDays(8 - startDate.getDayOfWeek().getValue());
        } else {
            // Eğer hafta içi ise, bu haftanın Pazartesi'sine ayarla
            weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);
        }
        
        Log.d("CustomCalendarFragment", "Hafta başlangıcı: " + weekStart);
        
        // Her hafta için hafta içi günleri ekle
        for (int week = 0; week < weeks; week++) {
            for (int day : weekDays) {
                // Haftanın gününü bul
                LocalDate date = weekStart.plusDays(day - 1).plusWeeks(week);
                
                Log.d("CustomCalendarFragment", "Kontrol edilen tarih: " + date + ", bugün: " + today);
                
                // Eğer tarih bugünden önceyse ve aynı hafta değilse, atla
                if (date.isBefore(today) && !date.equals(today) && 
                    !(date.getYear() == today.getYear() && date.get(WeekFields.of(Locale.getDefault()).weekOfYear()) == today.get(WeekFields.of(Locale.getDefault()).weekOfYear()))) {
                    Log.d("CustomCalendarFragment", "Tarih atlandı: " + date);
                    continue;
                }
                
                // Görevi tarihe ekle
                addTaskToDate(task, date, tasksByDate);
                
                // Log ekle
                Log.d("CustomCalendarFragment", "Hafta içi görev eklendi: " + task.getTitle() + ", tarih: " + date);
            }
        }
    }
    
    /**
     * Hafta sonu görevler için tarihleri ekler
     */
    private void addWeekendsTaskDates(Task task, LocalDate startDate, 
                                     Map<LocalDate, List<Task>> tasksByDate, int weeks) {
        // Hafta sonu günler (6=Cumartesi, 7=Pazar)
        int[] weekDays = {6, 7};
        
        // Her hafta için hafta sonu günleri ekle
        for (int week = 0; week < weeks; week++) {
            for (int day : weekDays) {
                // Haftanın gününü bul
                LocalDate date = startDate.with(DayOfWeek.of(day)).plusWeeks(week);
                
                // Eğer başlangıç tarihinden önceyse, bir sonraki haftaya geç
                if (date.isBefore(startDate)) {
                    date = date.plusWeeks(1);
                }
                
                addTaskToDate(task, date, tasksByDate);
            }
        }
    }
    
    /**
     * Özel görevler için tarihleri ekler
     */
    private void addCustomTaskDates(Task task, LocalDate startDate, RoutineSettings routineSettings,
                                   Map<LocalDate, List<Task>> tasksByDate) {
        // Hem haftalık hem de aylık ayarları kontrol et
        if (routineSettings.getWeekDays() != null && !routineSettings.getWeekDays().isEmpty()) {
            addWeeklyTaskDates(task, startDate, routineSettings.getWeekDays(), tasksByDate, 4);
        }
        
        if (routineSettings.getMonthDays() != null && !routineSettings.getMonthDays().isEmpty()) {
            addMonthlyTaskDates(task, startDate, routineSettings.getMonthDays(), tasksByDate, 3);
        }
        
        // Eğer her ikisi de yoksa, günlük olarak ekle
        if ((routineSettings.getWeekDays() == null || routineSettings.getWeekDays().isEmpty()) && 
            (routineSettings.getMonthDays() == null || routineSettings.getMonthDays().isEmpty())) {
            addDailyTaskDates(task, startDate, tasksByDate, 14);
        }
    }
    
    /**
     * Belirli bir tarihe görev ekler
     */
    private void addTaskToDate(Task task, LocalDate date, Map<LocalDate, List<Task>> tasksByDate) {
        if (!tasksByDate.containsKey(date)) {
            tasksByDate.put(date, new ArrayList<>());
        }
        
        // Aynı görevi tekrar eklememek için kontrol et
        boolean taskExists = false;
        for (Task existingTask : tasksByDate.get(date)) {
            if (existingTask.getId() == task.getId()) {
                taskExists = true;
                break;
            }
        }
        
        if (!taskExists) {
            tasksByDate.get(date).add(task);
            Log.d("CustomCalendarFragment", "Görev tarihe eklendi: " + task.getTitle() + ", tarih: " + date);
        } else {
            Log.d("CustomCalendarFragment", "Görev zaten var, eklenmedi: " + task.getTitle() + ", tarih: " + date);
        }
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

        Log.d("CustomCalendarFragment", "filterTasksForSelectedDate başladı: seçili tarih=" + selectedDate + ", toplam görev sayısı=" + allTasks.size());

        // Seçilen tarih için başlangıç ve bitiş zamanlarını ayarla
        LocalDate startDate = selectedDate;
        LocalDate endDate = selectedDate.plusDays(1);
        
        Date startDateTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateTime = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Seçilen tarih için görevleri filtrele
        List<Task> tasksForDate = new ArrayList<>();
        
        // Normal görevleri filtrele
        List<Task> normalTasksForDate = allTasks.stream()
                .filter(task -> {
                    Date taskDate = task.getDateTime();
                    return taskDate != null &&
                            !taskDate.before(startDateTime) &&
                            taskDate.before(endDateTime) &&
                            (task.getRepeatType() == null || task.getRepeatType() == RepeatType.NONE);
                })
                .collect(Collectors.toList());
        
        Log.d("CustomCalendarFragment", "Normal görev sayısı: " + normalTasksForDate.size());
        
        // Normal görevleri listeye ekle
        tasksForDate.addAll(normalTasksForDate);
        
        // Rutin görevleri filtrele ve ekle
        List<Task> routineTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getRepeatType() != null && task.getRepeatType() != RepeatType.NONE) {
                Log.d("CustomCalendarFragment", "Rutin görev kontrol ediliyor: " + task.getTitle() + ", rutin tipi: " + task.getRepeatType());
                
                // Rutin görev için kontrol et
                boolean isForSelectedDate = isTaskForSelectedDate(task, selectedDate);
                Log.d("CustomCalendarFragment", "Görev seçili tarih için geçerli mi: " + isForSelectedDate);
                
                if (isForSelectedDate) {
                    // Görevin kopyasını oluştur ve tarihini seçilen tarihe ayarla
                    Task taskCopy = new Task();
                    taskCopy.setId(task.getId());
                    taskCopy.setTitle(task.getTitle());
                    taskCopy.setDescription(task.getDescription());
                    taskCopy.setCompleted(task.isCompleted());
                    taskCopy.setNotificationType(task.getNotificationType());
                    taskCopy.setRepeatType(task.getRepeatType());
                    taskCopy.setReminderMinutes(task.getReminderMinutes());
                    
                    // Tarihi seçilen tarihe ayarla
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(task.getDateTime());
                    
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth(),
                                        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0);
                    
                    taskCopy.setDateTime(selectedCalendar.getTime());
                    
                    routineTasks.add(taskCopy);
                    Log.d("CustomCalendarFragment", "Rutin görev eklendi: " + taskCopy.getTitle() + ", tarih: " + selectedCalendar.getTime());
                }
            }
        }
        
        Log.d("CustomCalendarFragment", "Rutin görev sayısı: " + routineTasks.size());
        
        // Rutin görevleri listeye ekle
        tasksForDate.addAll(routineTasks);
        
        // Görevleri tarihe göre sırala
        tasksForDate.sort((t1, t2) -> t1.getDateTime().compareTo(t2.getDateTime()));

        Log.d("CustomCalendarFragment", "Toplam görev sayısı: " + tasksForDate.size());

        if (tasksForDate.isEmpty()) {
            showEmptyState();
            Log.d("CustomCalendarFragment", "Boş durum gösteriliyor");
        } else {
            hideEmptyState();
            // Yeni bir liste oluştur ve adapter'a gönder
            adapter.submitList(null);
            adapter.submitList(new ArrayList<>(tasksForDate));
            Log.d("CustomCalendarFragment", "Görevler adapter'a gönderildi");
        }
    }

    /**
     * Bir görevin belirli bir tarih için geçerli olup olmadığını kontrol eder
     */
    private boolean isTaskForSelectedDate(Task task, LocalDate date) {
        try {
            // Görevin tarihini LocalDate'e dönüştür
            LocalDate taskDate = task.getDateTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            Log.d("CustomCalendarFragment", "isTaskForSelectedDate: task=" + task.getTitle() + ", taskDate=" + taskDate + ", selectedDate=" + date + ", repeatType=" + task.getRepeatType());
            
            // Eğer görev rutin değilse, sadece kendi tarihinde göster
            if (task.getRepeatType() == null || task.getRepeatType() == RepeatType.NONE) {
                return taskDate.equals(date);
            }
            
            // Rutin görev için rutin ayarlarını al
            RoutineSettings routineSettings = viewModel.getRoutineSettingsByTaskIdSync(task.getId());
            if (routineSettings == null) {
                Log.e("CustomCalendarFragment", "Rutin ayarları bulunamadı: taskId=" + task.getId());
                return taskDate.equals(date);
            }
            
            Log.d("CustomCalendarFragment", "Rutin tipi: " + routineSettings.getRepeatType());
            
            // Bugünün tarihini al
            LocalDate today = LocalDate.now();
            
            // Tekrarlama tipine göre kontrol et
            boolean result = false;
            switch (routineSettings.getRepeatType()) {
                case DAILY:
                    // Her gün için geçerli
                    result = true;
                    break;
                    
                case WEEKLY:
                    // Haftanın belirli günleri için geçerli
                    result = isWeeklyTaskForDate(routineSettings.getWeekDays(), date, taskDate);
                    break;
                    
                case MONTHLY:
                    // Ayın belirli günleri için geçerli
                    result = isMonthlyTaskForDate(routineSettings.getMonthDays(), date);
                    break;
                    
                case WEEKDAYS:
                    // Hafta içi günler için geçerli (Pazartesi-Cuma)
                    DayOfWeek dayOfWeek = date.getDayOfWeek();
                    boolean isWeekday = dayOfWeek.getValue() >= DayOfWeek.MONDAY.getValue() && 
                                       dayOfWeek.getValue() <= DayOfWeek.FRIDAY.getValue();
                    
                    // Eğer hafta içi bir gün değilse, gösterme
                    if (!isWeekday) {
                        Log.d("CustomCalendarFragment", "Hafta içi bir gün değil, gösterilmiyor");
                        result = false;
                        break;
                    }
                    
                    // Hafta içi görevleri her zaman göster
                    result = true;
                    break;
                    
                case WEEKENDS:
                    // Hafta sonu günler için geçerli (Cumartesi-Pazar)
                    DayOfWeek dayOfWeek2 = date.getDayOfWeek();
                    boolean isWeekend = dayOfWeek2 == DayOfWeek.SATURDAY || dayOfWeek2 == DayOfWeek.SUNDAY;
                    
                    // Eğer hafta sonu bir gün değilse, gösterme
                    if (!isWeekend) {
                        result = false;
                        break;
                    }
                    
                    // Hafta sonu görevleri her zaman göster
                    result = true;
                    break;
                    
                case CUSTOM:
                    // Özel tekrarlama ayarları için geçerli
                    result = isCustomTaskForDate(routineSettings, date, taskDate);
                    break;
                    
                default:
                    result = taskDate.equals(date);
                    break;
            }
            
            Log.d("CustomCalendarFragment", "isTaskForSelectedDate sonucu: " + result);
            return result;
        } catch (Exception e) {
            Log.e("CustomCalendarFragment", "isTaskForSelectedDate: Hata oluştu", e);
            return false;
        }
    }
    
    /**
     * Haftalık görevin belirli bir tarihte gösterilmesi gerekip gerekmediğini kontrol eder
     */
    private boolean isWeeklyTaskForDate(String weekDaysStr, LocalDate date, LocalDate taskStartDate) {
        // Eğer haftanın günleri belirtilmemişse, görevin başlangıç gününü kullan
        if (weekDaysStr == null || weekDaysStr.isEmpty()) {
            return date.getDayOfWeek().getValue() == taskStartDate.getDayOfWeek().getValue();
        }
        
        // Haftanın günlerini kontrol et
        String[] days = weekDaysStr.split(",");
        int currentDayOfWeek = date.getDayOfWeek().getValue();
        
        for (String day : days) {
            try {
                int weekDay = Integer.parseInt(day.trim());
                if (weekDay == currentDayOfWeek) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Geçersiz gün formatı, atla
            }
        }
        
        return false;
    }
    
    /**
     * Aylık görevin belirli bir tarihte gösterilmesi gerekip gerekmediğini kontrol eder
     */
    private boolean isMonthlyTaskForDate(String monthDaysStr, LocalDate date) {
        // Eğer ayın günleri belirtilmemişse, gösterme
        if (monthDaysStr == null || monthDaysStr.isEmpty()) {
            return false;
        }
        
        // Ayın günlerini kontrol et
        String[] days = monthDaysStr.split(",");
        int currentDayOfMonth = date.getDayOfMonth();
        
        for (String day : days) {
            try {
                int monthDay = Integer.parseInt(day.trim());
                if (monthDay == currentDayOfMonth) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Geçersiz gün formatı, atla
            }
        }
        
        return false;
    }
    
    /**
     * Özel görevin belirli bir tarihte gösterilmesi gerekip gerekmediğini kontrol eder
     */
    private boolean isCustomTaskForDate(RoutineSettings routineSettings, LocalDate date, LocalDate taskStartDate) {
        // Hem haftalık hem de aylık ayarları kontrol et
        if (routineSettings.getWeekDays() != null && !routineSettings.getWeekDays().isEmpty()) {
            if (isWeeklyTaskForDate(routineSettings.getWeekDays(), date, taskStartDate)) {
                return true;
            }
        }
        
        if (routineSettings.getMonthDays() != null && !routineSettings.getMonthDays().isEmpty()) {
            if (isMonthlyTaskForDate(routineSettings.getMonthDays(), date)) {
                return true;
            }
        }
        
        // Eğer her ikisi de yoksa, günlük olarak kontrol et
        if ((routineSettings.getWeekDays() == null || routineSettings.getWeekDays().isEmpty()) && 
            (routineSettings.getMonthDays() == null || routineSettings.getMonthDays().isEmpty())) {
            return true; // Her gün göster
        }
        
        return false;
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
        // Görünen ay ve komşu aylar için tarih aralığını belirle
        LocalDate startDate = visibleMonth.minusMonths(1).atDay(1);
        LocalDate endDate = visibleMonth.plusMonths(1).atEndOfMonth();
        
        // Java Date nesnelerine dönüştür
        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        
        // Sadece bu tarih aralığındaki görevleri yükle
        viewModel.getTasksBetweenDates(start, end).observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                // Tüm görevleri al (rutin görevler dahil)
                List<Task> allTasks = new ArrayList<>(tasks);
                
                // Rutin görevleri de ekle
                List<Task> routineTasks = viewModel.getAllTasks().getValue();
                if (routineTasks != null) {
                    for (Task task : routineTasks) {
                        if (task.getRepeatType() != null && task.getRepeatType() != RepeatType.NONE) {
                            // Rutin görevleri ekle
                            if (!allTasks.contains(task)) {
                                allTasks.add(task);
                            }
                        }
                    }
                }
                
                // Takvimi güncelle
                updateCalendarWithTasks(allTasks);
                
                // Eğer seçili tarih bu aralıktaysa, o tarihe ait görevleri filtrele
                if (!selectedDate.isBefore(startDate) && !selectedDate.isAfter(endDate)) {
                    filterTasksForSelectedDate();
                }
            }
        });
    }
} 