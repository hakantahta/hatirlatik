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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.databinding.FragmentTaskDetailBinding;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.viewmodel.TaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskDetailFragment extends Fragment {
    private FragmentTaskDetailBinding binding;
    private TaskViewModel viewModel;
    private Task currentTask;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("tr"));

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        // Görev ID'sini al
        TaskDetailFragmentArgs args = TaskDetailFragmentArgs.fromBundle(getArguments());
        long taskId = args.getTaskId();
        
        // Görevi yükle
        viewModel.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                currentTask = task;
                updateUI(task);
            }
        });

        // FAB click listener
        binding.fabEdit.setOnClickListener(v -> navigateToEdit());

        // CheckBox listener
        binding.checkboxTask.setOnClickListener(v -> onCheckBoxClicked());
    }

    private void updateUI(Task task) {
        binding.textTaskTitle.setText(task.getTitle());
        binding.textTaskDescription.setText(task.getDescription());
        binding.textTaskDatetime.setText(dateFormat.format(task.getDateTime()));
        binding.textReminderTime.setText(String.format(getString(R.string.reminder_minutes_format), 
                task.getReminderMinutes()));
        binding.textNotificationType.setText(task.getNotificationType().name());
        binding.checkboxTask.setChecked(task.isCompleted());
        
        // Tamamlanmış görevlerin görünümünü güncelle
        float alpha = task.isCompleted() ? 0.5f : 1.0f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            binding.textTaskTitle.setAlpha(alpha);
            binding.textTaskDescription.setAlpha(alpha);
            binding.textTaskDatetime.setAlpha(alpha);
            binding.textReminderTime.setAlpha(alpha);
            binding.textNotificationType.setAlpha(alpha);
        }
    }

    private void onCheckBoxClicked() {
        if (currentTask != null) {
            viewModel.updateTaskCompletionStatus(currentTask.getId(), binding.checkboxTask.isChecked());
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
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> {
                    if (currentTask != null) {
                        viewModel.deleteTask(currentTask);
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 