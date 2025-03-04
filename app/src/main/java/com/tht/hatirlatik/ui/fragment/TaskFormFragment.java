package com.tht.hatirlatik.ui.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.viewmodel.TaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

        String title = editTaskTitle.getText().toString().trim();
        String description = editTaskDescription.getText().toString().trim();
        Date dateTime = calendar.getTime();
        
        // Hatırlatma süresini al
        int selectedIndex = 0;
        for (int i = 0; i < REMINDER_MINUTES.length; i++) {
            if (dropdownReminderMinutes.getText().toString().contains(
                    String.valueOf(REMINDER_MINUTES[i]))) {
                selectedIndex = i;
                break;
            }
        }
        int reminderMinutes = REMINDER_MINUTES[selectedIndex];
        
        // Bildirim türünü al
        NotificationType notificationType = radioGroupNotificationType.getCheckedRadioButtonId() ==
                R.id.radio_notification_alarm ? NotificationType.ALARM : NotificationType.NOTIFICATION;

        if (editingTaskId != -1L && editingTask != null) {
            // Mevcut görevin özelliklerini güncelle
            editingTask.setTitle(title);
            editingTask.setDescription(description);
            editingTask.setDateTime(dateTime);
            editingTask.setReminderMinutes(reminderMinutes);
            editingTask.setNotificationType(notificationType);
            
            // Görevi düzenlediğimizde durumunu aktif olarak ayarla
            editingTask.setCompleted(false);
            
            // Görevi güncelle
            viewModel.updateTask(editingTask);
            
            // Kullanıcıya bilgi ver
            showSnackbar(getString(R.string.task_updated_active));
        } else {
            // Yeni görev oluştur
            Task task = new Task(title, description, dateTime, reminderMinutes, notificationType);
            viewModel.insertTask(task);
            
            // Yeni görev eklediğimizde widget'ı hemen güncelle
            updateWidgets();
            
            // Kullanıcıya bilgi ver
            showSnackbar(getString(R.string.task_added));
        }
        
        // Widget'ı güncelle - birden fazla yöntemle
        updateWidgets();
        
        // Ana listeye geri dön
        Navigation.findNavController(requireView()).navigateUp();
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
} 