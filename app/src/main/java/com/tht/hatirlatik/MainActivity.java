package com.tht.hatirlatik;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.databinding.ActivityMainBinding;
import com.tht.hatirlatik.utils.InternetHelper;
import com.tht.hatirlatik.ui.fragment.TaskListFragment;
import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.notification.TaskNotificationManager;
import com.tht.hatirlatik.repository.TaskRepository;
import com.tht.hatirlatik.widget.TaskWidgetProvider;

import android.content.SharedPreferences;
import android.os.Handler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Ana aktivite sınıfı.
 * Navigation component ile fragment geçişlerini yönetir.
 */
public class MainActivity extends AppCompatActivity implements InternetHelper.InternetConnectionListener {

    private static final String TAG = "MainActivity";
    public static final String EXTRA_OPEN_TASK_DETAIL = "com.tht.hatirlatik.EXTRA_OPEN_TASK_DETAIL";
    public static final String EXTRA_TASK_ID = "com.tht.hatirlatik.EXTRA_TASK_ID";
    private ActivityMainBinding binding;
    private NavController navController;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private InternetHelper internetHelper;
    private androidx.appcompat.app.AlertDialog noInternetDialog;
    private ExtendedFloatingActionButton fabAddTask;
    private ExtendedFloatingActionButton fabCalendar;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // İnternet yardımcısını başlat
            internetHelper = new InternetHelper(this);
            internetHelper.setConnectionListener(this);

            MaterialToolbar toolbar = binding.toolbar;
            setSupportActionBar(toolbar);

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.taskListFragment
                ).build();

                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

                // Görev ekleme butonu
                fabAddTask = binding.fabAddTask;
                fabAddTask.setOnClickListener(view -> {
                    try {
                        if (!internetHelper.isInternetAvailable()) {
                            showNoInternetDialog();
                            return;
                        }
                        navController.navigate(R.id.action_taskList_to_taskForm);
                    } catch (Exception e) {
                        Log.e(TAG, "FAB onClick: " + e.getMessage(), e);
                        Toast.makeText(this, "Görev ekleme ekranı açılırken bir hata oluştu", Toast.LENGTH_SHORT).show();
                    }
                });
                
                // Takvim butonu
                fabCalendar = binding.fabCalendar;
                fabCalendar.setOnClickListener(view -> {
                    try {
                        if (!internetHelper.isInternetAvailable()) {
                            showNoInternetDialog();
                            return;
                        }
                        navController.navigate(R.id.action_taskList_to_customCalendar);
                    } catch (Exception e) {
                        Log.e(TAG, "Calendar FAB onClick: " + e.getMessage(), e);
                        Toast.makeText(this, "Takvim ekranı açılırken bir hata oluştu", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Navigation host fragment bulunamadı");
                Toast.makeText(this, "Uygulama başlatılırken bir hata oluştu", Toast.LENGTH_LONG).show();
                finish();
            }

            // İzin isteği için launcher'ı başlat
            setupPermissionLauncher();
            
            // İnternet bağlantısını kontrol et
            if (!internetHelper.isInternetAvailable()) {
                showNoInternetDialog();
            }

            // İzinleri kontrol et
            checkPermissions();

            // Widget'tan gelen intent'i işle
            handleIntent(getIntent());

        } catch (Exception e) {
            Log.e(TAG, "onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Uygulama başlatılırken bir hata oluştu", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            // Widget'tan görev detayını açma isteği
            if (intent.getBooleanExtra(EXTRA_OPEN_TASK_DETAIL, false)) {
                long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
                if (taskId != -1) {
                    // Görev detay sayfasına git
                    Bundle args = new Bundle();
                    args.putLong("taskId", taskId);
                    navController.navigate(R.id.action_taskList_to_taskDetail, args);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (internetHelper != null) {
            internetHelper.removeConnectionListener();
        }
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();
        }
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                if (noInternetDialog != null && noInternetDialog.isShowing()) {
                    noInternetDialog.dismiss();
                }
                Snackbar.make(binding.getRoot(), R.string.internet_connected, Snackbar.LENGTH_SHORT).show();
            } else {
                showNoInternetDialog();
            }
        });
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            return;
        }

        noInternetDialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.no_internet_title)
            .setMessage(R.string.no_internet_message)
            .setCancelable(false)
            .setPositiveButton(R.string.no_internet_retry, (dialog, which) -> {
                if (internetHelper.isInternetAvailable()) {
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(), R.string.internet_connected, Snackbar.LENGTH_SHORT).show();
                } else {
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(), R.string.internet_not_connected, Snackbar.LENGTH_SHORT).show();
                    showNoInternetDialog();
                }
            })
            .setNeutralButton(R.string.no_internet_settings, (dialog, which) -> {
                internetHelper.openInternetSettings();
                dialog.dismiss();
                showNoInternetDialog();
            })
            .setNegativeButton(R.string.no_internet_exit, (dialog, which) -> {
                finish();
            })
            .create();

        noInternetDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!internetHelper.isInternetAvailable()) {
            showNoInternetDialog();
        }
        
        // Widget'ı güncelle
        HatirlatikApplication.updateWidgets();
        
        // Eğer ana ekrandaysak FAB'ları göster, değilse gizle
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestinationId = navController.getCurrentDestination().getId();
            if (currentDestinationId == R.id.taskListFragment) {
                fabAddTask.show();
                fabCalendar.show();
            } else {
                fabAddTask.hide();
                fabCalendar.hide();
            }
        }
    }

    // NavController'daki değişiklikleri dinlemek için
    @Override
    protected void onStart() {
        super.onStart();
        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                if (destinationId == R.id.taskListFragment) {
                    fabAddTask.show();
                    fabCalendar.show();
                } else {
                    fabAddTask.hide();
                    fabCalendar.hide();
                }

                // Toolbar menüsünü güncelle
                invalidateOptionsMenu();
            });
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestinationId = navController.getCurrentDestination().getId();
            
            // Takvim butonunu sadece görev listesi ekranında göster
            MenuItem calendarItem = menu.findItem(R.id.action_custom_calendar);
            if (calendarItem != null) {
                calendarItem.setVisible(currentDestinationId == R.id.taskListFragment);
            }
            
            // Filtreleme butonunu sadece görev listesi ekranında göster
            MenuItem filterItem = menu.findItem(R.id.action_filter);
            if (filterItem != null) {
                filterItem.setVisible(currentDestinationId == R.id.taskListFragment);
            }
        }
        
        return true;
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (!isGranted) {
                    showPermissionDeniedDialog();
                }
            }
        );
    }

    private void checkPermissions() {
        // İzinleri sırayla kontrol et
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.permission_notification_title)
                    .setMessage(R.string.permission_notification_message)
                    .setPositiveButton(R.string.button_grant_permission, (dialog, which) -> {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        // Bildirim izni istendikten sonra alarm iznini kontrol et
                        new Handler().postDelayed(this::checkAlarmPermission, 1000);
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                        showPermissionDeniedDialog();
                        // İzin reddedilse bile diğer izinleri kontrol et
                        new Handler().postDelayed(this::checkAlarmPermission, 1000);
                    })
                    .setCancelable(false)
                    .show();
            } else {
                // Bildirim izni zaten varsa alarm iznini kontrol et
                checkAlarmPermission();
            }
        } else {
            // Android 13'ten küçük sürümlerde direkt alarm iznini kontrol et
            checkAlarmPermission();
        }
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_alarm_title)
                .setMessage(R.string.permission_alarm_message)
                .setPositiveButton(R.string.button_open_settings, (dialog, which) -> {
                    openAlarmSettings();
                    // Alarm ayarları açıldıktan sonra internet iznini kontrol et
                    new Handler().postDelayed(this::checkInternetPermission, 1000);
                })
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    // İzin reddedilse bile internet iznini kontrol et
                    new Handler().postDelayed(this::checkInternetPermission, 1000);
                })
                .show();
        } else {
            // Alarm izni zaten varsa internet iznini kontrol et
            checkInternetPermission();
        }
    }

    private void checkInternetPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_internet_title)
                .setMessage(R.string.permission_internet_message)
                .setPositiveButton(R.string.button_grant_permission, (dialog, which) -> {
                    // İnternet izni için ayarlar sayfasına yönlendir
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
        }
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_notification_denied_title)
            .setMessage(R.string.permission_notification_denied_message)
            .setPositiveButton(R.string.button_open_settings, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton(R.string.button_cancel, null)
            .show();
    }

    private boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return getSystemService(android.app.AlarmManager.class).canScheduleExactAlarms();
        }
        return true;
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
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
            int itemId = item.getItemId();
            if (itemId == R.id.action_settings) {
                navController.navigate(R.id.action_taskList_to_settings);
                return true;
            } else if (itemId == R.id.action_filter) {
                if (navController.getCurrentDestination().getId() == R.id.taskListFragment) {
                    TaskListFragment fragment = (TaskListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment)
                            .getChildFragmentManager()
                            .getFragments().get(0);
                    fragment.showFilterMenu();
                }
                return true;
            } else if (itemId == R.id.action_custom_calendar) {
                navController.navigate(R.id.action_taskList_to_customCalendar);
                return true;
            } else if (itemId == R.id.action_test_notifications) {
                testNotificationSystem();
                return true;
            }
            return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, "onOptionsItemSelected: " + e.getMessage(), e);
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && (navController.navigateUp() || super.onSupportNavigateUp());
    }
    
    /**
     * Bildirim sistemini test etmek için kullanılan metod.
     * Bu metod, çoklu görev bildirimlerini test etmek için kullanılır.
     */
    private void testNotificationSystem() {
        executor.execute(() -> {
            // Test için 5 görev oluştur
            List<Task> tasks = new ArrayList<>();
            
            for (int i = 1; i <= 5; i++) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, 5); // 5 saniye sonra bildirim göster
                
                Task task = new Task();
                task.setTitle("Test Görevi " + i);
                task.setDescription("Bu bir test görevidir. #" + i);
                task.setDateTime(calendar.getTime());
                task.setReminderMinutes(0);
                task.setNotificationType(NotificationType.NOTIFICATION);
                
                tasks.add(task);
            }
            
            // Görevleri veritabanına kaydet ve bildirim planla
            TaskRepository taskRepository = new TaskRepository(getApplication());
            TaskNotificationManager notificationManager = new TaskNotificationManager(this);
            
            for (Task task : tasks) {
                taskRepository.insertTask(task, new TaskRepository.OnTaskOperationCallback() {
                    @Override
                    public void onSuccess(long taskId) {
                        task.setId(taskId);
                        notificationManager.scheduleTaskReminder(task);
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        handler.post(() -> {
                            Toast.makeText(MainActivity.this, "Görev oluşturulurken hata: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            // UI thread'de bilgi mesajı göster
            handler.post(() -> {
                Toast.makeText(this, "5 test görevi oluşturuldu. 5 saniye içinde bildirimler gelecek.", 
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    /**
     * Görev eklendiğinde, güncellendiğinde veya silindiğinde widget'ı günceller
     */
    public void updateWidgets() {
        Intent intent = new Intent(this, TaskWidgetProvider.class);
        intent.setAction(TaskWidgetProvider.ACTION_DATA_UPDATED);
        sendBroadcast(intent);
    }
}