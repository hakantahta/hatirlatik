package com.tht.hatirlatik.ui.task;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.databinding.FragmentTaskListBinding;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.TaskFilter;
import com.tht.hatirlatik.ui.adapter.TaskAdapter;
import com.tht.hatirlatik.viewmodel.TaskViewModel;

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
    }

    private void observeViewModel() {
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            Log.d(TAG, "Tasks updated: " + (tasks != null ? tasks.size() : 0) + " items");
            if (tasks != null && !tasks.isEmpty()) {
                binding.emptyState.setVisibility(View.GONE);
                binding.recyclerViewTasks.setVisibility(View.VISIBLE);
                adapter.submitList(tasks);
            } else {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewTasks.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state: " + isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Log.e(TAG, "Error: " + errorMessage);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        try {
            inflater.inflate(R.menu.menu_main, menu);
            Log.d(TAG, "Menu inflated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error inflating menu: " + e.getMessage());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            int itemId = item.getItemId();
            Log.d(TAG, "Menu item selected: " + itemId);
            
            if (itemId == R.id.action_filter) {
                Log.d(TAG, "Filter button clicked");
                showFilterMenu(requireActivity().findViewById(android.R.id.content));
                return true;
            } else if (itemId == R.id.action_settings) {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_taskList_to_settings);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling menu item: " + e.getMessage());
            Toast.makeText(requireContext(), "Menü işlenirken bir hata oluştu", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterMenu(View anchor) {
        try {
            Log.d(TAG, "Showing filter menu");
            
            // Toolbar'ı bul ve onun üzerinde popup'ı göster
            View toolbarView = requireActivity().findViewById(R.id.toolbar);
            if (toolbarView == null) {
                Log.d(TAG, "Toolbar not found, using anchor view");
                toolbarView = anchor;
            } else {
                Log.d(TAG, "Toolbar found");
            }
            
            PopupMenu popup = new PopupMenu(requireContext(), toolbarView);
            popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());

            // Mevcut filtreyi işaretle
            Menu menu = popup.getMenu();
            TaskFilter currentFilter = viewModel.getCurrentFilter();
            Log.d(TAG, "Current filter: " + currentFilter);

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
                Log.d(TAG, "Filter selected: " + itemId);

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
            Log.d(TAG, "Filter menu shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing filter menu: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Filtre menüsü açılırken bir hata oluştu", Toast.LENGTH_SHORT).show();
        }
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
        viewModel.updateTaskCompletionStatus(task.getId(), isChecked);
    }

    @Override
    public void onTaskDeleteClicked(Task task) {
        viewModel.deleteTask(task);
        Toast.makeText(requireContext(), R.string.task_deleted, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 