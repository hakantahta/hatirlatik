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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.databinding.FragmentTaskListBinding;
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
    private SwipeRefreshLayout swipeRefreshLayout;
    private View rootView;
    private FragmentTaskListBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskListBinding.inflate(inflater, container, false);
        return binding.getRoot();
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
        
        // SwipeRefreshLayout'u ayarla
        setupSwipeRefresh();
        
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
        recyclerView = binding.recyclerViewTasks;
        emptyStateView = binding.emptyState;
        progressBar = binding.progressBar;
        swipeRefreshLayout = binding.swipeRefresh;
    }

    private void setupSwipeRefresh() {
        // SwipeRefreshLayout'un renklerini ayarla
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.primary_dark,
            R.color.accent
        );
        
        // Yenileme listener'ını ayarla
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Görev listesini yenile
            viewModel.refreshTasks();
        });
    }

    private void setupObservers() {
        // Filtrelenmiş görev listesini gözlemle
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                adapter.submitList(null); // Önce listeyi temizle
                adapter.submitList(tasks); // Yeni listeyi set et
                updateEmptyState(tasks.isEmpty());
            }
        });

        // Yükleme durumunu gözlemle
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // Yükleme bittiğinde yenileme animasyonunu durdur
            if (!isLoading) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Hata mesajlarını gözlemle
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
                // Hata durumunda yenileme animasyonunu durdur
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewTasks.setAdapter(adapter);
        binding.recyclerViewTasks.setHasFixedSize(true);
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
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
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