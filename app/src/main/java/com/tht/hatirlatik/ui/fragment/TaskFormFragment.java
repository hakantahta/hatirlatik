package com.tht.hatirlatik.ui.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.model.RepeatType;
import com.tht.hatirlatik.model.RoutineSettings;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TaskFormFragment extends Fragment {

    private TaskViewModel viewModel;
    private TextInputLayout layoutTaskTitle;
    private TextInputEditText editTaskTitle;
    private TextInputEditText editTaskDescription;
    private TextInputEditText editTaskDate;
    private TextInputEditText editTaskTime;
    private AutoCompleteTextView dropdownReminderMinutes;
    private RadioGroup radioGroupNotificationType;
    private MaterialButton buttonSave;
    
    private final Calendar calendar = Calendar.getInstance();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("tr"));
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("tr"));
    
    private static final int[] REMINDER_MINUTES = {5, 10, 15, 30, 60, 120, 180, 360, 720, 1440};

    // Düzenlenen görevin ID'si
    private long editingTaskId = -1L;
    private Task editingTask = null;

    // Rutin ayarları bileşenleri
    private SwitchMaterial switchIsRoutine;
    private ConstraintLayout layoutRoutineOptions;
    private RadioGroup radioGroupRepeatType;
    private ConstraintLayout layoutWeeklySettings;
    private ChipGroup chipGroupWeekDays;
    private ConstraintLayout layoutMonthlySettings;
    private TextInputEditText editTextMonthDays;
    private TextInputLayout layoutMonthDays;
    private ConstraintLayout layoutDailySettings;
    private Slider sliderTimesPerDay;
    private TextView textViewTimesPerDay;
    private Slider sliderIntervalHours;
    private TextView textViewIntervalHours;
    private ConstraintLayout layoutIntervalHours;
    private ConstraintLayout layoutCustomSettings;
    private TextInputEditText editTextStartTime;
    private TextInputEditText editTextEndTime;
    private TextInputEditText editTextStartDate;
    private TextInputEditText editTextEndDate;
    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();
    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ViewModel'i başlat
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        
        // View'ları başlat
        initViews(view);
        
        // Hatırlatma süresi seçeneklerini ayarla
        setupReminderMinutesDropdown();
        
        // Tarih ve saat seçicileri için click listener'ları ayarla
        setupDateTimePickers();
        
        // Rutin ayarlarını ayarla
        setupRoutineSettings();
        
        // Kaydet butonu için click listener ayarla
        buttonSave.setOnClickListener(v -> saveTask());
        
        // Observer'ları ayarla
        setupObservers();
        
        // Argümanları kontrol et
        checkArguments();
    }

    private void initViews(View view) {
        layoutTaskTitle = view.findViewById(R.id.layout_task_title);
        editTaskTitle = view.findViewById(R.id.edit_task_title);
        editTaskDescription = view.findViewById(R.id.edit_task_description);
        editTaskDate = view.findViewById(R.id.edit_task_date);
        editTaskTime = view.findViewById(R.id.edit_task_time);
        dropdownReminderMinutes = view.findViewById(R.id.dropdown_reminder_minutes);
        radioGroupNotificationType = view.findViewById(R.id.radio_group_notification_type);
        buttonSave = view.findViewById(R.id.button_save);
        
        // Rutin ayarları bileşenleri
        switchIsRoutine = view.findViewById(R.id.switch_is_routine);
        layoutRoutineOptions = view.findViewById(R.id.layout_routine_options);
        radioGroupRepeatType = view.findViewById(R.id.radio_group_repeat_type);
        
        // Haftalık ayarlar
        layoutWeeklySettings = view.findViewById(R.id.layout_weekly_settings);
        chipGroupWeekDays = view.findViewById(R.id.chip_group_week_days);
        
        // Aylık ayarlar
        layoutMonthlySettings = view.findViewById(R.id.layout_monthly_settings);
        editTextMonthDays = view.findViewById(R.id.edit_text_month_days);
        layoutMonthDays = view.findViewById(R.id.layout_month_days);
        
        // Günlük ayarlar
        layoutDailySettings = view.findViewById(R.id.layout_daily_settings);
        sliderTimesPerDay = view.findViewById(R.id.slider_times_per_day);
        textViewTimesPerDay = view.findViewById(R.id.text_view_times_per_day);
        sliderIntervalHours = view.findViewById(R.id.slider_interval_hours);
        textViewIntervalHours = view.findViewById(R.id.text_view_interval_hours);
        layoutIntervalHours = view.findViewById(R.id.layout_interval_hours);
        
        // Özel ayarlar
        layoutCustomSettings = view.findViewById(R.id.layout_custom_settings);
        
        // Başlangıç ve bitiş saatleri
        editTextStartTime = view.findViewById(R.id.edit_text_start_time);
        editTextEndTime = view.findViewById(R.id.edit_text_end_time);
        
        // Başlangıç ve bitiş tarihleri
        editTextStartDate = view.findViewById(R.id.edit_text_start_date);
        editTextEndDate = view.findViewById(R.id.edit_text_end_date);
    }

    private void setupReminderMinutesDropdown() {
        String[] items = new String[REMINDER_MINUTES.length];
        for (int i = 0; i < REMINDER_MINUTES.length; i++) {
            items[i] = String.format(getString(R.string.reminder_minutes_format), REMINDER_MINUTES[i]);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, items);
        dropdownReminderMinutes.setAdapter(adapter);
        dropdownReminderMinutes.setText(items[0], false);
    }

    private void setupDateTimePickers() {
        editTaskDate.setOnClickListener(v -> showDatePicker());
        editTaskTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, month);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            // Seçilen tarih bugünden önceyse, bugünün tarihini kullan
            if (selectedCalendar.getTime().before(Calendar.getInstance().getTime())) {
                showSnackbar(getString(R.string.error_past_date));
                // Bugünün tarihini ayarla
                Calendar today = Calendar.getInstance();
                calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
            } else {
                // Geçerli bir tarih seçildi
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            }
            updateDateField();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        
        // Minimum tarih olarak bugünü ayarla
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            Calendar selectedTime = Calendar.getInstance();
            selectedTime.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
            selectedTime.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
            selectedTime.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedTime.set(Calendar.MINUTE, minute);
            
            // Eğer seçilen tarih bugünse ve seçilen saat şu andan önceyse
            Calendar now = Calendar.getInstance();
            if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) &&
                selectedTime.getTime().before(now.getTime())) {
                
                showSnackbar(getString(R.string.error_past_time));
                // Şu anki saatten 1 saat sonrasını ayarla
                now.add(Calendar.HOUR_OF_DAY, 1);
                calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, 0);
            } else {
                // Geçerli bir saat seçildi
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
            }
            updateTimeField();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void updateDateField() {
        editTaskDate.setText(dateFormat.format(calendar.getTime()));
    }

    private void updateTimeField() {
        editTaskTime.setText(timeFormat.format(calendar.getTime()));
    }

    private void setupObservers() {
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showSnackbar(message);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonSave.setEnabled(!isLoading);
        });
    }

    private void saveTask() {
        if (!validateForm()) {
            return;
        }
        
        android.util.Log.d("TaskFormFragment", "saveTask: Görev kaydediliyor");
        
        // Görev nesnesini oluştur
        Task task = new Task();
        
        // Eğer düzenleme modundaysak, mevcut görevin ID'sini kullan
        if (editingTask != null) {
            task.setId(editingTask.getId());
            task.setCreatedAt(editingTask.getCreatedAt());
            task.setCompleted(editingTask.isCompleted());
            android.util.Log.d("TaskFormFragment", "saveTask: Mevcut görev düzenleniyor, id=" + editingTask.getId());
        }
        
        // Görev bilgilerini ayarla
        task.setTitle(editTaskTitle.getText().toString().trim());
        task.setDescription(editTaskDescription.getText().toString().trim());
        task.setDateTime(calendar.getTime());
        task.setUpdatedAt(new Date());
        
        // Hatırlatma süresini ayarla
        String reminderMinutesStr = dropdownReminderMinutes.getText().toString();
        int reminderMinutes = 15; // Varsayılan değer
        try {
            reminderMinutes = Integer.parseInt(reminderMinutesStr);
        } catch (NumberFormatException e) {
            // Varsayılan değeri kullan
        }
        task.setReminderMinutes(reminderMinutes);
        
        // Bildirim tipini ayarla
        int selectedNotificationTypeId = radioGroupNotificationType.getCheckedRadioButtonId();
        if (selectedNotificationTypeId == R.id.radio_notification) {
            task.setNotificationType(NotificationType.NOTIFICATION);
        } else if (selectedNotificationTypeId == R.id.radio_notification_alarm) {
            task.setNotificationType(NotificationType.NOTIFICATION_AND_ALARM);
        } else {
            task.setNotificationType(NotificationType.NOTIFICATION);
        }
        
        // Rutin ayarlarını al
        RoutineSettings routineSettings = getRoutineSettings();
        
        // Tekrarlama tipini görev nesnesine de ekle (takvim görünümü için)
        if (routineSettings != null) {
            task.setRepeatType(routineSettings.getRepeatType());
            android.util.Log.d("TaskFormFragment", "saveTask: Rutin ayarları eklendi, tip=" + routineSettings.getRepeatType());
        } else {
            task.setRepeatType(RepeatType.NONE);
            android.util.Log.d("TaskFormFragment", "saveTask: Rutin ayarları yok, tip=NONE");
        }
        
        // Görev ve rutin ayarlarını kaydet
        if (editingTask != null) {
            // Mevcut görevi güncelle
            viewModel.updateTaskWithRoutine(task, routineSettings);
        } else {
            // Yeni görev ekle
            viewModel.insertTaskWithRoutine(task, routineSettings);
        }
        
        // Geri dön
        Navigation.findNavController(requireView()).popBackStack();
    }
    
    // Widget'ı güncelleme yardımcı metodu
    private void updateWidgets() {
        try {
            // 1. Yöntem: Widget'ı doğrudan güncelle
            com.tht.hatirlatik.widget.TaskWidgetProvider.refreshWidget(requireContext());
            
            // 2. Yöntem: Uygulama sınıfından güncelleme yap
            if (requireContext().getApplicationContext() instanceof com.tht.hatirlatik.HatirlatikApplication) {
                com.tht.hatirlatik.HatirlatikApplication app = 
                    (com.tht.hatirlatik.HatirlatikApplication) requireContext().getApplicationContext();
                app.updateWidgets();
            }
            
            // 3. Yöntem: Doğrudan tüm widget'ları güncelle
            com.tht.hatirlatik.widget.TaskWidgetProvider.updateAllWidgets(requireContext());
            
            // 4. Yöntem: ViewModel üzerinden güncelleme yap
            viewModel.updateWidgets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Başlık kontrolü
        if (editTaskTitle.getText().toString().trim().isEmpty()) {
            layoutTaskTitle.setError(getString(R.string.error_empty_title));
            isValid = false;
        } else {
            layoutTaskTitle.setError(null);
        }

        // Tarih kontrolü
        if (editTaskDate.getText().toString().isEmpty()) {
            showSnackbar(getString(R.string.error_empty_date));
            isValid = false;
        }

        // Saat kontrolü
        if (editTaskTime.getText().toString().isEmpty()) {
            showSnackbar(getString(R.string.error_empty_time));
            isValid = false;
        }

        // Geçmiş tarih ve saat kontrolü
        Calendar now = Calendar.getInstance();
        if (calendar.getTime().before(now.getTime())) {
            // Eğer seçilen tarih bugünse, sadece saat geçmişte olabilir
            if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                showSnackbar(getString(R.string.error_past_time));
            } else {
                showSnackbar(getString(R.string.error_past_date));
            }
            isValid = false;
        }

        return isValid;
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void checkArguments() {
        if (getArguments() != null) {
            // Takvimden seçilen tarih varsa, onu kullan
            long selectedDate = getArguments().getLong("selectedDate", -1L);
            if (selectedDate != -1L) {
                calendar.setTimeInMillis(selectedDate);
                updateDateField();
                
                // Saat için şu anki saati kullan
                Calendar now = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
                updateTimeField();
            }
        }
    }

    private void setupRoutineSettings() {
        // Tekrarlama tipi radio group'u için listener
        radioGroupRepeatType.setOnCheckedChangeListener((group, checkedId) -> {
            // Tüm özel ayar panellerini başlangıçta gizle
            layoutWeeklySettings.setVisibility(View.GONE);
            layoutMonthlySettings.setVisibility(View.GONE);
            layoutCustomSettings.setVisibility(View.GONE);
            layoutDailySettings.setVisibility(View.GONE);
            
            if (checkedId == R.id.radioNone) {
                // Tekrarlama yok seçilirse, tüm ayarları gizle
                return;
            }
            
            if (checkedId == R.id.radioWeekly) {
                // Haftalık seçilirse, haftalık ayarlar panelini göster
                layoutWeeklySettings.setVisibility(View.VISIBLE);
            }
            
            if (checkedId == R.id.radioMonthly) {
                // Aylık seçilirse, aylık ayarlar panelini göster
                layoutMonthlySettings.setVisibility(View.VISIBLE);
            }
            
            if (checkedId == R.id.radioCustom) {
                // Özel seçilirse, özel ayarlar panelini göster
                layoutCustomSettings.setVisibility(View.VISIBLE);
            }
            
            if (checkedId == R.id.radioDaily || checkedId == R.id.radioCustom) {
                // Günlük veya özel seçilirse, günlük ayarlar panelini göster
                layoutDailySettings.setVisibility(View.VISIBLE);
            }
        });
        
        // Günde kaç kez slider'ı değiştiğinde
        sliderTimesPerDay.addOnChangeListener((slider, value, fromUser) -> {
            int times = (int) value;
            textViewTimesPerDay.setText(times + " " + getString(R.string.routine_times_per_day));
            
            // Eğer günde birden fazla kez seçilmişse, interval ayarlarını göster
            if (times > 1) {
                layoutIntervalHours.setVisibility(View.VISIBLE);
            } else {
                layoutIntervalHours.setVisibility(View.GONE);
            }
        });
        
        // Tekrarlamalar arası süre slider'ı değiştiğinde
        sliderIntervalHours.addOnChangeListener((slider, value, fromUser) -> {
            int hours = (int) value;
            textViewIntervalHours.setText(hours + " " + getString(R.string.routine_interval_hours));
        });
        
        // Başlangıç saati seçici
        editTextStartTime.setOnClickListener(v -> showTimePickerDialog(startTimeCalendar, editTextStartTime));
        
        // Bitiş saati seçici
        editTextEndTime.setOnClickListener(v -> showTimePickerDialog(endTimeCalendar, editTextEndTime));
        
        // Başlangıç tarihi seçici
        editTextStartDate.setOnClickListener(v -> showDatePickerDialog(startDateCalendar, editTextStartDate));
        
        // Bitiş tarihi seçici
        editTextEndDate.setOnClickListener(v -> showDatePickerDialog(endDateCalendar, editTextEndDate));
        
        // Haftanın günleri için chip group listener
        chipGroupWeekDays.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // En az bir gün seçili olmalı
            if (checkedIds.isEmpty() && radioGroupRepeatType.getCheckedRadioButtonId() == R.id.radioWeekly) {
                // Eğer hiç gün seçilmemişse, bir uyarı göster
                showSnackbar(getString(R.string.error_select_at_least_one_day));
                // Varsayılan olarak pazartesi gününü seç
                ((Chip) chipGroupWeekDays.findViewById(R.id.chipMonday)).setChecked(true);
            }
        });
        
        // Ayın günleri için input filtresi
        editTextMonthDays.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Girilen değerleri kontrol et (1-31 arası, virgülle ayrılmış)
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    try {
                        String[] days = input.split(",");
                        for (String day : days) {
                            int dayNum = Integer.parseInt(day.trim());
                            if (dayNum < 1 || dayNum > 31) {
                                layoutMonthDays.setError(getString(R.string.error_invalid_month_day));
                                return;
                            }
                        }
                        layoutMonthDays.setError(null);
                    } catch (NumberFormatException e) {
                        layoutMonthDays.setError(getString(R.string.error_invalid_month_day_format));
                    }
                } else {
                    layoutMonthDays.setError(null);
                }
            }
        });
        
        // Rutin ayarları switch'i
        switchIsRoutine.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Rutin ayarları panelini göster
                layoutRoutineOptions.setVisibility(View.VISIBLE);
                // Varsayılan olarak "Günlük" seç
                radioGroupRepeatType.check(R.id.radioDaily);
            } else {
                // Rutin ayarları panelini gizle
                layoutRoutineOptions.setVisibility(View.GONE);
                // "Tekrarlama yok" seç
                radioGroupRepeatType.check(R.id.radioNone);
            }
        });
    }

    private RoutineSettings getRoutineSettings() {
        // Eğer rutin switch'i kapalıysa, rutin ayarları yok demektir
        if (!switchIsRoutine.isChecked()) {
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Rutin switch'i kapalı, null dönüyor");
            return null;
        }
        
        // Seçilen tekrarlama tipini al
        int selectedId = radioGroupRepeatType.getCheckedRadioButtonId();
        if (selectedId == R.id.radioNone) {
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi NONE, null dönüyor");
            return null; // Tekrarlama yok
        }
        
        // Rutin ayarları nesnesi oluştur
        RoutineSettings settings = new RoutineSettings();
        
        // Tekrarlama tipini ayarla
        if (selectedId == R.id.radioDaily) {
            settings.setRepeatType(RepeatType.DAILY);
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi DAILY");
        } else if (selectedId == R.id.radioWeekly) {
            settings.setRepeatType(RepeatType.WEEKLY);
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi WEEKLY");
        } else if (selectedId == R.id.radioMonthly) {
            settings.setRepeatType(RepeatType.MONTHLY);
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi MONTHLY");
        } else if (selectedId == R.id.radioWeekdays) {
            settings.setRepeatType(RepeatType.WEEKDAYS);
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi WEEKDAYS");
        } else if (selectedId == R.id.radioWeekends) {
            settings.setRepeatType(RepeatType.WEEKENDS);
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi WEEKENDS");
        } else if (selectedId == R.id.radioCustom) {
            settings.setRepeatType(RepeatType.CUSTOM);
            android.util.Log.d("TaskFormFragment", "getRoutineSettings: Tekrarlama tipi CUSTOM");
        }
        
        // Haftanın günlerini ayarla (haftalık veya özel seçilmişse)
        if (selectedId == R.id.radioWeekly || selectedId == R.id.radioCustom) {
            List<Integer> selectedDays = new ArrayList<>();
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipMonday)).isChecked()) selectedDays.add(1);
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipTuesday)).isChecked()) selectedDays.add(2);
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipWednesday)).isChecked()) selectedDays.add(3);
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipThursday)).isChecked()) selectedDays.add(4);
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipFriday)).isChecked()) selectedDays.add(5);
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipSaturday)).isChecked()) selectedDays.add(6);
            if (((Chip) chipGroupWeekDays.findViewById(R.id.chipSunday)).isChecked()) selectedDays.add(7);
            
            if (!selectedDays.isEmpty()) {
                String weekDays = selectedDays.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                settings.setWeekDays(weekDays);
            }
        }
        
        // Ayın günlerini ayarla (aylık veya özel seçilmişse)
        if (selectedId == R.id.radioMonthly || selectedId == R.id.radioCustom) {
            String monthDays = editTextMonthDays.getText().toString().trim();
            if (!monthDays.isEmpty()) {
                settings.setMonthDays(monthDays);
            }
        }
        
        // Günlük ayarları (günlük veya özel seçilmişse)
        if (selectedId == R.id.radioDaily || selectedId == R.id.radioCustom) {
            settings.setTimesPerDay((int) sliderTimesPerDay.getValue());
            settings.setIntervalHours((int) sliderIntervalHours.getValue());
            
            // Başlangıç ve bitiş saatleri
            settings.setStartTime(editTextStartTime.getText().toString());
            settings.setEndTime(editTextEndTime.getText().toString());
        }
        
        // Başlangıç ve bitiş tarihleri (özel seçilmişse)
        if (selectedId == R.id.radioCustom) {
            if (!editTextStartDate.getText().toString().isEmpty()) {
                settings.setStartDate(startDateCalendar.getTime());
            }
            
            String endDateStr = editTextEndDate.getText().toString().trim();
            if (!endDateStr.isEmpty()) {
                settings.setEndDate(endDateCalendar.getTime());
            }
        } else {
            // Diğer tekrarlama tipleri için varsayılan başlangıç tarihi bugün
            settings.setStartDate(new Date());
        }
        
        return settings;
    }

    private void populateRoutineSettings(RoutineSettings settings) {
        if (settings == null) {
            // Rutin ayarları yoksa, varsayılan değerleri kullan
            switchIsRoutine.setChecked(false);
            layoutRoutineOptions.setVisibility(View.GONE);
            radioGroupRepeatType.check(R.id.radioNone);
            return;
        }
        
        // Rutin switch'ini aç
        switchIsRoutine.setChecked(true);
        layoutRoutineOptions.setVisibility(View.VISIBLE);
        
        // Tekrarlama tipini ayarla
        switch (settings.getRepeatType()) {
            case DAILY:
                radioGroupRepeatType.check(R.id.radioDaily);
                layoutDailySettings.setVisibility(View.VISIBLE);
                break;
            case WEEKLY:
                radioGroupRepeatType.check(R.id.radioWeekly);
                layoutWeeklySettings.setVisibility(View.VISIBLE);
                break;
            case MONTHLY:
                radioGroupRepeatType.check(R.id.radioMonthly);
                layoutMonthlySettings.setVisibility(View.VISIBLE);
                break;
            case WEEKDAYS:
                radioGroupRepeatType.check(R.id.radioWeekdays);
                break;
            case WEEKENDS:
                radioGroupRepeatType.check(R.id.radioWeekends);
                break;
            case CUSTOM:
                radioGroupRepeatType.check(R.id.radioCustom);
                layoutWeeklySettings.setVisibility(View.VISIBLE);
                layoutMonthlySettings.setVisibility(View.VISIBLE);
                layoutDailySettings.setVisibility(View.VISIBLE);
                layoutCustomSettings.setVisibility(View.VISIBLE);
                break;
            default:
                radioGroupRepeatType.check(R.id.radioNone);
                break;
        }
        
        // Haftanın günlerini ayarla
        if (settings.getWeekDays() != null && !settings.getWeekDays().isEmpty()) {
            String[] days = settings.getWeekDays().split(",");
            for (String day : days) {
                try {
                    int dayNum = Integer.parseInt(day.trim());
                    switch (dayNum) {
                        case 1: // Pazartesi
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipMonday)).setChecked(true);
                            break;
                        case 2: // Salı
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipTuesday)).setChecked(true);
                            break;
                        case 3: // Çarşamba
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipWednesday)).setChecked(true);
                            break;
                        case 4: // Perşembe
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipThursday)).setChecked(true);
                            break;
                        case 5: // Cuma
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipFriday)).setChecked(true);
                            break;
                        case 6: // Cumartesi
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipSaturday)).setChecked(true);
                            break;
                        case 7: // Pazar
                            ((Chip) chipGroupWeekDays.findViewById(R.id.chipSunday)).setChecked(true);
                            break;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Ayın günlerini ayarla
        if (settings.getMonthDays() != null && !settings.getMonthDays().isEmpty()) {
            editTextMonthDays.setText(settings.getMonthDays());
        }
        
        // Günde kaç kez
        sliderTimesPerDay.setValue(settings.getTimesPerDay());
        textViewTimesPerDay.setText(settings.getTimesPerDay() + " " + getString(R.string.routine_times_per_day));
        
        // Tekrarlamalar arası süre
        sliderIntervalHours.setValue(settings.getIntervalHours());
        textViewIntervalHours.setText(settings.getIntervalHours() + " " + getString(R.string.routine_interval_hours));
        
        // Günde birden fazla kez seçilmişse, interval ayarlarını göster
        if (settings.getTimesPerDay() > 1) {
            layoutIntervalHours.setVisibility(View.VISIBLE);
        } else {
            layoutIntervalHours.setVisibility(View.GONE);
        }
        
        // Başlangıç ve bitiş saatleri
        if (settings.getStartTime() != null && !settings.getStartTime().isEmpty()) {
            editTextStartTime.setText(settings.getStartTime());
            try {
                Date startTime = timeFormat.parse(settings.getStartTime());
                if (startTime != null) {
                    startTimeCalendar.setTime(startTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (settings.getEndTime() != null && !settings.getEndTime().isEmpty()) {
            editTextEndTime.setText(settings.getEndTime());
            try {
                Date endTime = timeFormat.parse(settings.getEndTime());
                if (endTime != null) {
                    endTimeCalendar.setTime(endTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Başlangıç ve bitiş tarihleri
        if (settings.getStartDate() != null) {
            editTextStartDate.setText(dateFormat.format(settings.getStartDate()));
            startDateCalendar.setTime(settings.getStartDate());
        }
        
        if (settings.getEndDate() != null) {
            editTextEndDate.setText(dateFormat.format(settings.getEndDate()));
            endDateCalendar.setTime(settings.getEndDate());
        } else {
            editTextEndDate.setText("");
        }
    }

    private void showTimePickerDialog(Calendar calendar, TextInputEditText editText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    editText.setText(timeFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }
    
    private void showDatePickerDialog(Calendar calendar, TextInputEditText editText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    editText.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
} 