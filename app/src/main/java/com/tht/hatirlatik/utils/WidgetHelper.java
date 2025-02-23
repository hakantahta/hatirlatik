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

    public void addWidgetToHomeScreen(OnWidgetAddCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
                ComponentName widgetProvider = new ComponentName(context, TaskListWidget.class);

                if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                    appWidgetManager.requestPinAppWidget(widgetProvider, null, null);
                    callback.onSuccess();
                } else {
                    showManualWidgetInstructions(callback);
                }
            } else {
                showManualWidgetInstructions(callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Widget eklenirken hata oluştu: " + e.getMessage());
            callback.onError(e.getMessage());
            showManualWidgetInstructions(callback);
        }
    }

    private void showManualWidgetInstructions(OnWidgetAddCallback callback) {
        if (context instanceof Activity) {
            new MaterialAlertDialogBuilder(context)
                .setTitle("Widget Nasıl Eklenir?")
                .setMessage("1. Ana ekranda boş bir alana uzun basın\n" +
                          "2. Açılan menüden 'Widgets' seçeneğine dokunun\n" +
                          "3. Widgetlar listesinde 'Hatırlatık' widget'ını bulun\n" +
                          "4. Widget'ı basılı tutup ana ekranda istediğiniz yere sürükleyin")
                .setPositiveButton("Tamam", (dialog, which) -> {
                    // Ana ekrana git
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(homeIntent);
                    callback.onManualInstructionsShown();
                })
                .show();
        }
    }

    public interface OnWidgetAddCallback {
        void onSuccess();
        void onError(String error);
        void onManualInstructionsShown();
    }
} 