package com.tahtalı.hatirlatik.presentation.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.tahtalı.hatirlatik.R;
import com.tahtalı.hatirlatik.presentation.home.TaskAdapter;

public class CalendarFragment extends Fragment {
    private CalendarViewModel viewModel;
    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews(view);
        setupRecyclerView();
        setupObservers();
        setupCalendarPicker();
    }

    private void setupViews(View view) {
        taskRecyclerView = view.findViewById(R.id.recycler_view_calendar_tasks);
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter();
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskRecyclerView.setAdapter(taskAdapter);
    }

    private void setupObservers() {
        viewModel.getTasksForSelectedDate().observe(getViewLifecycleOwner(), tasks -> {
            taskAdapter.submitList(tasks);
        });
    }

    private void setupCalendarPicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Tarih Seçin")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            viewModel.setSelectedDate(selection);
        });
    }
} 