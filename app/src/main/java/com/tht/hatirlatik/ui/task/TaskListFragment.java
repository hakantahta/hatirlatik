package com.tht.hatirlatik.ui.task;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.databinding.FragmentTaskListBinding;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.TaskFilter;
import com.tht.hatirlatik.ui.adapter.TaskAdapter;
import com.tht.hatirlatik.viewmodel.TaskViewModel;
import com.tht.hatirlatik.ui.view.TaskCheckBox;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment implements TaskAdapter.TaskItemListener {
    private static final String TAG = "TaskListFragment";

    private FragmentTaskListBinding binding;
    private TaskViewModel viewModel;
    private TaskAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewTasks.setAdapter(adapter);
        binding.recyclerViewTasks.setHasFixedSize(true);
    }

    private void observeViewModel() {
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                binding.emptyState.setVisibility(View.GONE);
                binding.recyclerViewTasks.setVisibility(View.VISIBLE);
                adapter.submitList(tasks);
            } else {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewTasks.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_filter) {
            showFilterMenu(requireActivity().findViewById(android.R.id.content));
            return true;
        } else if (itemId == R.id.action_settings) {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_taskList_to_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterMenu(View anchor) {
        View toolbarView = requireActivity().findViewById(R.id.toolbar);
        if (toolbarView == null) {
            toolbarView = anchor;
        }
        
        PopupMenu popup = new PopupMenu(requireContext(), toolbarView);
        popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());

        Menu menu = popup.getMenu();
        TaskFilter currentFilter = viewModel.getCurrentFilter();

        if (currentFilter != null) {
            switch (currentFilter) {
                case ALL:
                    menu.findItem(R.id.filter_all).setChecked(true);
                    break;
                case ACTIVE:
                    menu.findItem(R.id.filter_active).setChecked(true);
                    break;
                case COMPLETED:
                    menu.findItem(R.id.filter_completed).setChecked(true);
                    break;
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.filter_all) {
                viewModel.setFilter(TaskFilter.ALL);
                Toast.makeText(requireContext(), "Tüm görevler", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.filter_active) {
                viewModel.setFilter(TaskFilter.ACTIVE);
                Toast.makeText(requireContext(), "Aktif görevler", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.filter_completed) {
                viewModel.setFilter(TaskFilter.COMPLETED);
                Toast.makeText(requireContext(), "Tamamlanan görevler", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public void onTaskClicked(Task task) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putLong("taskId", task.getId());
        navController.navigate(R.id.action_taskList_to_taskDetail, args);
    }

    @Override
    public void onTaskCheckedChanged(Task task, boolean isChecked) {
        // Direkt olarak durumu güncelle
        viewModel.updateTaskCompletionStatus(task.getId(), isChecked);
        
        // Snackbar ile bilgilendirme yap
        String message = String.format("Görev %s olarak işaretlendi", 
            isChecked ? "tamamlandı" : "aktif");
            
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
            .setAction("Geri Al", v -> {
                viewModel.updateTaskCompletionStatus(task.getId(), !isChecked);
            })
            .show();
    }

    @Override
    public void onTaskDeleteClicked(Task task) {
        // Direkt olarak görevi sil
        viewModel.deleteTask(task);
        
        // Snackbar ile bilgilendirme yap
        Snackbar.make(binding.getRoot(), "Görev silindi", Snackbar.LENGTH_SHORT)
            .setAction("Geri Al", v -> {
                viewModel.insertTask(task);
            })
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 