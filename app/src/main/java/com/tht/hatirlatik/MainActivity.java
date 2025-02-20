package com.tht.hatirlatik;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tht.hatirlatik.databinding.ActivityMainBinding;

/**
 * Ana aktivite sınıfı.
 * Navigation component ile fragment geçişlerini yönetir.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Toolbar'ı ayarla
            setSupportActionBar(binding.toolbar);

            // Navigation controller'ı ayarla
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                
                // Top level destinasyonları ayarla
                AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.taskListFragment
                ).build();

                // Toolbar ile navigation'ı bağla
                NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);

                // Navigation değişikliklerini dinle
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    // Toolbar başlığını güncelle
                    if (destination.getId() == R.id.taskDetailFragment) {
                        binding.toolbar.setTitle(R.string.task_details);
                    } else if (destination.getId() == R.id.taskListFragment) {
                        binding.toolbar.setTitle(R.string.task_list);
                    }
                });

                // FAB click listener'ı ayarla
                binding.fabAddTask.setOnClickListener(view -> {
                    try {
                        navController.navigate(R.id.action_taskList_to_taskForm);
                    } catch (Exception e) {
                        Log.e(TAG, "FAB onClick: " + e.getMessage(), e);
                        Toast.makeText(this, "Görev ekleme ekranı açılırken bir hata oluştu", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Navigation host fragment bulunamadı");
                Toast.makeText(this, "Uygulama başlatılırken bir hata oluştu", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Uygulama başlatılırken bir hata oluştu", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "onCreateOptionsMenu: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.action_settings && navController != null) {
                navController.navigate(R.id.action_taskList_to_settings);
                return true;
            }
            return super.onOptionsItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, "onOptionsItemSelected: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && (navController.navigateUp() || super.onSupportNavigateUp());
    }
}