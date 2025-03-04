package com.tht.hatirlatik;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.receivers.DailyWidgetUpdateReceiver;
import com.tht.hatirlatik.utils.AdHelper;
import com.tht.hatirlatik.widget.TaskWidgetProvider;
import com.tht.hatirlatik.notification.AlarmHelper;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class HatirlatikApplication extends Application implements Configuration.Provider {
    private static final String TAG = "HatirlatikApplication";
    public static final String ACTION_DAILY_WIDGET_UPDATE = "com.tht.hatirlatik.ACTION_DAILY_WIDGET_UPDATE";
    private static HatirlatikApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // AdMob'u başlat
        AdHelper.getInstance().initialize(this);
        
        // Uygulama başladığında widget'ı güncelle
        updateWidgets();
        
        // Günlük widget güncelleme alarmını kur
        setupDailyWidgetUpdateAlarm();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Veritabanı instance'ını temizle
        AppDatabase.destroyInstance();
        // WorkManager'ı temizle
        WorkManager.getInstance(this).cancelAllWork();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
    
    /**
     * Widget'ları günceller
     */
    public static void updateWidgets() {
        if (instance == null) {
            Log.e(TAG, "updateWidgets: Uygulama instance'ı null");
            return;
        }
        
        // Widget'ı yenilemek için intent oluştur
        Intent intent = new Intent(instance, TaskWidgetProvider.class);
        intent.setAction(TaskWidgetProvider.ACTION_DATA_UPDATED);
        instance.sendBroadcast(intent);
        
        // Ayrıca doğrudan tüm widget'ları güncelle
        TaskWidgetProvider.updateAllWidgets(instance);
    }
    
    /**
     * Günlük widget güncelleme alarmını kurar.
     */
    private void setupDailyWidgetUpdateAlarm() {
        try {
            AlarmHelper alarmHelper = new AlarmHelper(this);
            alarmHelper.scheduleDailyWidgetUpdate();
            Log.d(TAG, "setupDailyWidgetUpdateAlarm: Günlük widget güncelleme alarmı kuruldu");
        } catch (Exception e) {
            Log.e(TAG, "setupDailyWidgetUpdateAlarm: Alarm kurulurken hata oluştu", e);
        }
    }
} 