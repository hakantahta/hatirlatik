package com.tht.hatirlatik.utils;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tht.hatirlatik.widget.TaskListWidget;

public class WidgetHelper {
    private static final String TAG = "WidgetHelper";
    private final Context context;

    public WidgetHelper(Context context) {
        this.context = context;
    }

    public boolean canAddWidgets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
            if (appWidgetManager != null) {
                return appWidgetManager.isRequestPinAppWidgetSupported();
            }
        }
        return false;
    }

    public void addWidgetToHomeScreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
                ComponentName widgetProvider = new ComponentName(context, TaskListWidget.class);

                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported()) {
                    appWidgetManager.requestPinAppWidget(widgetProvider, null, null);
                    Toast.makeText(context, "Widget başarıyla eklendi!", Toast.LENGTH_SHORT).show();
                } else {
                    openWidgetSettings();
                }
            } else {
                openWidgetSettings();
            }
        } catch (Exception e) {
            Log.e(TAG, "Widget eklenirken hata oluştu: " + e.getMessage());
            Toast.makeText(context, "Widget eklenirken bir hata oluştu", Toast.LENGTH_SHORT).show();
            openWidgetSettings();
        }
    }

    public void openWidgetSettings() {
        try {
            // Ana ekrana git
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);

            // Kısa bir gecikme ile talimatları göster
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                new MaterialAlertDialogBuilder(context)
                    .setTitle("Widget Nasıl Eklenir?")
                    .setMessage("1. Ana ekranda boş bir alana uzun basın\n" +
                              "2. Açılan menüden 'Widgets' seçeneğine dokunun\n" +
                              "3. Widgetlar listesinde 'Hatırlatık' widget'ını bulun\n" +
                              "4. Widget'ı basılı tutup ana ekranda istediğiniz yere sürükleyin")
                    .setPositiveButton("Anladım", null)
                    .show();
            }, 500);
        } catch (Exception e) {
            Log.e(TAG, "Widget ayarları açılırken hata oluştu: " + e.getMessage());
            Toast.makeText(context, "Widget ayarları açılamadı. Ana ekrana gidip manuel olarak widget ekleyebilirsiniz.", Toast.LENGTH_LONG).show();
        }
    }
} 