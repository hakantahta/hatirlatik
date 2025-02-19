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
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateField();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
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

        Task task = new Task(title, description, dateTime, reminderMinutes, notificationType);
        viewModel.insertTask(task);
        
        // Ana listeye geri dön
        Navigation.findNavController(requireView()).navigateUp();
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

        // Geçmiş tarih kontrolü
        if (calendar.getTime().before(new Date())) {
            showSnackbar(getString(R.string.error_past_date));
            isValid = false;
        }

        return isValid;
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
} 