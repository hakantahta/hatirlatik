package com.tahtalı.hatirlatik.presentation.task;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.tahtalı.hatirlatik.R;
import com.tahtalı.hatirlatik.domain.model.Task;
import java.util.Calendar;

public class AddTaskDialog extends DialogFragment {
    private TaskViewModel viewModel;
    private TextInputEditText titleEditText;
    private TextInputEditText descriptionEditText;
    private Button dateButton;
    private Button timeButton;
    private Button saveButton;
    private Button cancelButton;
    private Calendar selectedDateTime;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        selectedDateTime = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews(view);
        setupListeners();
    }

    private void setupViews(View view) {
        titleEditText = view.findViewById(R.id.edit_text_task_title);
        descriptionEditText = view.findViewById(R.id.edit_text_task_description);
        dateButton = view.findViewById(R.id.button_task_date);
        timeButton = view.findViewById(R.id.button_task_time);
        saveButton = view.findViewById(R.id.button_save);
        cancelButton = view.findViewById(R.id.button_cancel);
    }

    private void setupListeners() {
        dateButton.setOnClickListener(v -> showDatePicker());
        timeButton.setOnClickListener(v -> showTimePicker());
        saveButton.setOnClickListener(v -> saveTask());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.task_date))
                .setSelection(selectedDateTime.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateTime.setTimeInMillis(selection);
            updateDateButtonText();
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedDateTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(selectedDateTime.get(Calendar.MINUTE))
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            selectedDateTime.set(Calendar.MINUTE, timePicker.getMinute());
            updateTimeButtonText();
        });

        timePicker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    private void updateDateButtonText() {
        dateButton.setText(android.text.format.DateFormat.getDateFormat(requireContext())
                .format(selectedDateTime.getTime()));
    }

    private void updateTimeButtonText() {
        timeButton.setText(android.text.format.DateFormat.getTimeFormat(requireContext())
                .format(selectedDateTime.getTime()));
    }

    private void saveTask() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Başlık boş olamaz");
            return;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDateTime(selectedDateTime.getTimeInMillis());

        viewModel.addTask(task);
        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.add_task);
        return dialog;
    }
} 