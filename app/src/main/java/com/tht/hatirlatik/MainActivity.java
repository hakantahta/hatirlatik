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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tht.hatirlatik.databinding.ActivityMainBinding;
import com.tht.hatirlatik.utils.InternetHelper;
import com.tht.hatirlatik.utils.WidgetHelper;

import android.content.SharedPreferences;

import android.os.Handler;
import android.appwidget.AppWidgetManager;


/**
 * Ana aktivite sınıfı.
 * Navigation component ile fragment geçişlerini yönetir.
 */
public class MainActivity extends AppCompatActivity implements InternetHelper.InternetConnectionListener {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private InternetHelper internetHelper;
    private WidgetHelper widgetHelper;
    private androidx.appcompat.app.AlertDialog noInternetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // İnternet yardımcısını başlat
            internetHelper = new InternetHelper(this);
            internetHelper.setConnectionListener(this);

            // Widget yardımcısını başlat
            widgetHelper = new WidgetHelper(this);

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

                binding.fabAddTask.setOnClickListener(view -> {
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

        } catch (Exception e) {
            Log.e(TAG, "onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Uygulama başlatılırken bir hata oluştu", Toast.LENGTH_LONG).show();
            finish();
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
                    requestPermissionLauncher.launch(Manifest.permission.INTERNET);
                    // İnternet izni istendikten sonra widget kontrolünü yap
                    new Handler().postDelayed(this::checkWidgetSupport, 1000);
                })
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    showInternetPermissionDeniedDialog();
                    // İzin reddedilse bile widget kontrolünü yap
                    new Handler().postDelayed(this::checkWidgetSupport, 1000);
                })
                .setCancelable(false)
                .show();
        } else {
            // İnternet izni zaten varsa widget kontrolünü yap
            checkWidgetSupport();
        }
    }

    private void checkWidgetSupport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);
            if (appWidgetManager != null && !appWidgetManager.isRequestPinAppWidgetSupported()) {
                showWidgetPermissionDialog();
            } else {
                showWidgetSuggestionIfNeeded();
            }
        } else {
            showWidgetSuggestionIfNeeded();
        }
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_notification_denied_title)
            .setMessage(R.string.permission_notification_denied_message)
            .setPositiveButton(R.string.button_open_settings, (dialog, which) -> {
                openAppSettings();
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

    private void showInternetPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_internet_denied_title)
            .setMessage(R.string.permission_internet_denied_message)
            .setPositiveButton(R.string.button_open_settings, (dialog, which) -> {
                openAppSettings();
            })
            .setNegativeButton(R.string.button_cancel, null)
            .show();
    }

    private void showWidgetPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Widget İzni Gerekli")
            .setMessage("Görevlerinizi ana ekranda görüntüleyebilmek için widget ekleme iznine ihtiyacımız var. " +
                      "Bu izni telefon ayarlarınızdan verebilirsiniz.")
            .setPositiveButton("Ayarlara Git", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    private void showWidgetSuggestionIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
        boolean widgetSuggestionShown = prefs.getBoolean("widget_suggestion_shown", false);
        
        if (!widgetSuggestionShown) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Widget Eklemek İster Misiniz?")
                .setMessage("Görevlerinizi ana ekranda görüntüleyebilmeniz için size özel bir widget sunuyoruz. " +
                          "Bu widget sayesinde:\n\n" +
                          "• Aktif görevlerinizi hızlıca görebilirsiniz\n" +
                          "• Görev detaylarına tek dokunuşla ulaşabilirsiniz\n" +
                          "• Görevlerinizin tarih ve saatlerini takip edebilirsiniz\n\n" +
                          "Widget'ı ana ekranınıza eklemek ister misiniz?")
                .setPositiveButton("Evet, Ekle", (dialog, which) -> {
                    if (widgetHelper.canAddWidgets()) {
                        widgetHelper.addWidgetToHomeScreen(new WidgetHelper.OnWidgetAddCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(MainActivity.this, 
                                    "Widget başarıyla eklendi!", 
                                    Toast.LENGTH_SHORT).show();
                                prefs.edit().putBoolean("widget_suggestion_shown", true).apply();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(MainActivity.this, 
                                    "Widget eklenirken bir hata oluştu", 
                                    Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onManualInstructionsShown() {
                                prefs.edit().putBoolean("widget_suggestion_shown", true).apply();
                            }
                        });
                    } else {
                        showWidgetPermissionDialog();
                    }
                })
                .setNegativeButton("Hayır, Teşekkürler", (dialog, which) -> {
                    prefs.edit().putBoolean("widget_suggestion_shown", true).apply();
                })
                .setNeutralButton("Daha Sonra", null)
                .show();
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