package com.tht.hatirlatik.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.TaskFilter;
import com.tht.hatirlatik.ui.adapter.TaskAdapter;
import com.tht.hatirlatik.viewmodel.TaskViewModel;

public class TaskListFragment extends Fragment implements TaskAdapter.TaskItemListener {
    
    private TaskViewModel viewModel;
    private TaskAdapter adapter;
    private View emptyStateView;
    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private FloatingActionButton fabAddTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // View'ları başlat
        initViews(view);
        
        // ViewModel'i başlat
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        
        // Adapter'ı başlat
        adapter = new TaskAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Observer'ları ayarla
        setupObservers();

        // FAB click listener'ı ayarla
        fabAddTask = requireActivity().findViewById(R.id.fab_add_task);
        fabAddTask.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_taskList_to_taskForm);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fabAddTask != null) {
            fabAddTask.show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fabAddTask != null) {
            fabAddTask.hide();
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        emptyStateView = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupObservers() {
        // Filtrelenmiş görevleri gözlemle
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            adapter.submitList(tasks);
            updateEmptyState(tasks == null || tasks.isEmpty());
        });

        // Yükleme durumunu gözlemle
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Hata mesajlarını gözlemle
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_completed) {
            showDeleteCompletedDialog();
            return true;
        } else if (item.getItemId() == R.id.action_filter) {
            showFilterMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteCompletedDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> {
                    viewModel.deleteCompletedTasks();
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private void showFilterMenu() {
        View menuItemView = requireActivity().findViewById(R.id.action_filter);
        PopupMenu popup = new PopupMenu(requireContext(), menuItemView);
        popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());

        // Mevcut filtreyi işaretle
        TaskFilter currentFilter = viewModel.getCurrentFilter();
        if (currentFilter != null) {
            MenuItem menuItem = null;
            switch (currentFilter) {
                case ALL:
                    menuItem = popup.getMenu().findItem(R.id.filter_all);
                    break;
                case ACTIVE:
                    menuItem = popup.getMenu().findItem(R.id.filter_active);
                    break;
                case COMPLETED:
                    menuItem = popup.getMenu().findItem(R.id.filter_completed);
                    break;
            }
            if (menuItem != null) {
                menuItem.setChecked(true);
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            TaskFilter newFilter = null;

            if (itemId == R.id.filter_all) {
                newFilter = TaskFilter.ALL;
            } else if (itemId == R.id.filter_active) {
                newFilter = TaskFilter.ACTIVE;
            } else if (itemId == R.id.filter_completed) {
                newFilter = TaskFilter.COMPLETED;
            }

            if (newFilter != null) {
                item.setChecked(true);
                viewModel.setFilter(newFilter);
                showFilterToast(newFilter);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showFilterToast(TaskFilter filter) {
        String message;
        switch (filter) {
            case ALL:
                message = getString(R.string.filter_all);
                break;
            case ACTIVE:
                message = getString(R.string.filter_active);
                break;
            case COMPLETED:
                message = getString(R.string.filter_completed);
                break;
            default:
                return;
        }
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void updateEmptyState(boolean isEmpty) {
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    // TaskAdapter.TaskItemListener implementasyonları
    @Override
    public void onTaskClicked(Task task) {
        // Task detay sayfasına yönlendir
        TaskListFragmentDirections.ActionTaskListToTaskDetail action =
                TaskListFragmentDirections.actionTaskListToTaskDetail(task.getId());
        Navigation.findNavController(requireView()).navigate(action);
    }

    @Override
    public void onTaskCheckedChanged(Task task, boolean isChecked) {
        viewModel.updateTaskCompletionStatus(task.getId(), isChecked);
    }

    @Override
    public void onTaskDeleteClicked(Task task) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> {
                    viewModel.deleteTask(task);
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }
} 