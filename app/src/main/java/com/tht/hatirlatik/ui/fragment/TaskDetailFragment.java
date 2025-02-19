package com.tht.hatirlatik.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.viewmodel.TaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskDetailFragment extends Fragment {

    private TaskViewModel viewModel;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView dateTimeTextView;
    private TextView reminderTimeTextView;
    private TextView notificationTypeTextView;
    private CheckBox checkBox;
    private FloatingActionButton fabEdit;
    private Task currentTask;
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("tr"));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ViewModel'i başlat
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        
        // View'ları başlat
        initViews(view);
        
        // Toolbar'ı ayarla
        setupToolbar(view);
        
        // Görev ID'sini al ve detayları yükle
        TaskDetailFragmentArgs args = TaskDetailFragmentArgs.fromBundle(getArguments());
        long taskId = args.getTaskId();
        loadTaskDetails(taskId);
        
        // FAB click listener
        fabEdit.setOnClickListener(v -> navigateToEdit());
    }

    private void initViews(View view) {
        titleTextView = view.findViewById(R.id.text_task_title);
        descriptionTextView = view.findViewById(R.id.text_task_description);
        dateTimeTextView = view.findViewById(R.id.text_task_datetime);
        reminderTimeTextView = view.findViewById(R.id.text_reminder_time);
        notificationTypeTextView = view.findViewById(R.id.text_notification_type);
        checkBox = view.findViewById(R.id.checkbox_task);
        fabEdit = view.findViewById(R.id.fab_edit);
        
        checkBox.setOnClickListener(v -> onCheckBoxClicked());
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        
        toolbar.setNavigationOnClickListener(v -> 
                Navigation.findNavController(requireView()).navigateUp());
    }

    private void loadTaskDetails(long taskId) {
        viewModel.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentTask = task;
                updateUI(task);
            } else {
                showSnackbar(getString(R.string.error_task_not_found));
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void updateUI(Task task) {
        titleTextView.setText(task.getTitle());
        descriptionTextView.setText(task.getDescription());
        dateTimeTextView.setText(dateFormat.format(task.getDateTime()));
        reminderTimeTextView.setText(String.format(getString(R.string.reminder_minutes_format), 
                task.getReminderMinutes()));
        notificationTypeTextView.setText(task.getNotificationType().name());
        checkBox.setChecked(task.isCompleted());
        
        // Tamamlanmış görevlerin görünümünü güncelle
        float alpha = task.isCompleted() ? 0.5f : 1.0f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            titleTextView.setAlpha(alpha);
            descriptionTextView.setAlpha(alpha);
            dateTimeTextView.setAlpha(alpha);
            reminderTimeTextView.setAlpha(alpha);
            notificationTypeTextView.setAlpha(alpha);
        }
    }

    private void onCheckBoxClicked() {
        if (currentTask != null) {
            viewModel.updateTaskCompletionStatus(currentTask.getId(), checkBox.isChecked());
        }
    }

    private void navigateToEdit() {
        if (currentTask != null) {
            TaskDetailFragmentDirections.ActionTaskDetailToTaskForm action =
                    TaskDetailFragmentDirections.actionTaskDetailToTaskForm(currentTask.getId());
            Navigation.findNavController(requireView()).navigate(action);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.task_detail_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> deleteTask())
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private void deleteTask() {
        if (currentTask != null) {
            viewModel.deleteTask(currentTask);
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
} 