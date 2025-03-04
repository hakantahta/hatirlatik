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
import com.tht.hatirlatik.model.NotificationType;
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
        binding.textNotificationType.setText(getNotificationTypeText(task.getNotificationType()));
        binding.checkboxTask.setChecked(task.isCompleted());
        
        // Görev özetini oluştur ve göster
        String summary = generateTaskSummary(task);
        binding.textTaskSummary.setText(summary);
        
        // Tamamlanmış görevlerin görünümünü güncelle
        float alpha = task.isCompleted() ? 0.5f : 1.0f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            binding.textTaskTitle.setAlpha(alpha);
            binding.textTaskDescription.setAlpha(alpha);
            binding.textTaskDatetime.setAlpha(alpha);
            binding.textReminderTime.setAlpha(alpha);
            binding.textNotificationType.setAlpha(alpha);
            binding.textTaskSummary.setAlpha(alpha);
        }
    }

    private String generateTaskSummary(Task task) {
        StringBuilder summary = new StringBuilder();
        
        // Görevin durumu
        String status = task.isCompleted() ? "Tamamlandı" : "Aktif";
        summary.append("Durum: ").append(status).append("\n\n");
        
        // Kalan süre veya geçen süre
        long currentTime = System.currentTimeMillis();
        long taskTime = task.getDateTime().getTime();
        long timeDiff = taskTime - currentTime;
        
        if (timeDiff > 0) {
            // Gelecekteki görev
            long days = timeDiff / (24 * 60 * 60 * 1000);
            long hours = (timeDiff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
            long minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000);
            
            summary.append("Kalan süre: ");
            if (days > 0) {
                summary.append(days).append(" gün ");
            }
            if (hours > 0 || days > 0) {
                summary.append(hours).append(" saat ");
            }
            summary.append(minutes).append(" dakika");
        } else {
            // Geçmiş görev
            long diffInMinutes = Math.abs(timeDiff) / (60 * 1000);
            if (diffInMinutes < 60) {
                summary.append("Geçen süre: ").append(diffInMinutes).append(" dakika önce");
            } else if (diffInMinutes < 24 * 60) {
                long hours = diffInMinutes / 60;
                long minutes = diffInMinutes % 60;
                summary.append("Geçen süre: ").append(hours).append(" saat");
                if (minutes > 0) {
                    summary.append(" ").append(minutes).append(" dakika");
                }
                summary.append(" önce");
            } else {
                long days = diffInMinutes / (24 * 60);
                long hours = (diffInMinutes % (24 * 60)) / 60;
                summary.append("Geçen süre: ").append(days).append(" gün");
                if (hours > 0) {
                    summary.append(" ").append(hours).append(" saat");
                }
                summary.append(" önce");
            }
        }
        
        summary.append("\n\n");
        
        // Hatırlatıcı bilgisi
        if (task.getReminderMinutes() > 0) {
            summary.append("Hatırlatıcı: ").append(task.getReminderMinutes())
                   .append(" dakika önce\n");
            summary.append("Bildirim tipi: ").append(getNotificationTypeText(task.getNotificationType()));
        } else {
            summary.append("Hatırlatıcı ayarlanmamış");
        }
        
        return summary.toString();
    }

    private String getNotificationTypeText(NotificationType type) {
        switch (type) {
            case NOTIFICATION:
                return "Normal Bildirim";
            case ALARM:
                return "Alarm";
            case NOTIFICATION_AND_ALARM:
                return "Bildirim ve Alarm";
            default:
                return type.name();
        }
    }

    private void onCheckBoxClicked() {
        if (currentTask != null) {
            viewModel.updateTaskCompletionStatus(currentTask.getId(), binding.checkboxTask.isChecked());
            // Widget'ı güncelle - birden fazla yöntemle
            updateWidgets();
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
                        // Widget'ı güncelle - birden fazla yöntemle
                        updateWidgets();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 