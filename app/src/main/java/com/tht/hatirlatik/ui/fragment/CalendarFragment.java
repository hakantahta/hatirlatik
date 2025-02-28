package com.tht.hatirlatik.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
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
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.ui.adapter.TaskAdapter;
import com.tht.hatirlatik.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalendarFragment extends Fragment implements TaskAdapter.TaskItemListener {

    private TaskViewModel viewModel;
    private TaskAdapter adapter;
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private MaterialButton addTaskButton;
    private RecyclerView tasksRecyclerView;
    private View emptyStateView;
    private CircularProgressIndicator progressBar;
    private Calendar selectedDate = Calendar.getInstance();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("tr"));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
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

        // Takvim listener'ını ayarla
        setupCalendarView();

        // Görev ekleme butonunu ayarla
        setupAddTaskButton();

        // Observer'ları ayarla
        setupObservers();

        // Başlangıçta bugünün tarihini göster
        updateSelectedDateUI();
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendar_view);
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
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateSelectedDateUI();
            filterTasksForSelectedDate();
        });
    }

    private void setupAddTaskButton() {
        addTaskButton.setOnClickListener(v -> {
            // Seçilen tarihi bundle ile TaskFormFragment'a gönder
            Bundle args = new Bundle();
            args.putLong("selectedDate", selectedDate.getTimeInMillis());
            Navigation.findNavController(requireView()).navigate(R.id.action_calendarFragment_to_taskForm, args);
        });
    }

    private void setupObservers() {
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                filterTasksForSelectedDate();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void updateSelectedDateUI() {
        String formattedDate = dateFormat.format(selectedDate.getTime());
        selectedDateTextView.setText(getString(R.string.tasks_for_date, formattedDate));
        addTaskButton.setText(getString(R.string.add_task_for_date, formattedDate));
    }

    private void filterTasksForSelectedDate() {
        List<Task> allTasks = viewModel.getTasks().getValue();
        if (allTasks == null) {
            showEmptyState();
            return;
        }

        // Seçilen tarih için başlangıç ve bitiş zamanlarını ayarla
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        // Seçilen tarih için görevleri filtrele
        List<Task> tasksForDate = allTasks.stream()
                .filter(task -> {
                    Date taskDate = task.getDateTime();
                    return taskDate != null &&
                            taskDate.after(startOfDay.getTime()) &&
                            taskDate.before(endOfDay.getTime());
                })
                .collect(Collectors.toList());

        if (tasksForDate.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.submitList(tasksForDate);
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
        Navigation.findNavController(requireView()).navigate(R.id.action_calendarFragment_to_taskDetail, args);
    }

    @Override
    public void onTaskCheckedChanged(Task task, boolean isChecked) {
        viewModel.updateTaskCompletionStatus(task.getId(), isChecked);
    }

    @Override
    public void onTaskDeleteClicked(Task task) {
        // Görev silme işlemi
        viewModel.deleteTask(task);
    }
} 