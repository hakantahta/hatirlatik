package com.tht.hatirlatik.workers;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.widget.TaskListWidget;

public class WidgetUpdateWorker extends Worker {
    
    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            // Widget'ı güncelle
            Context context = getApplicationContext();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, TaskListWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
            
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                // Widget verilerini güncelle
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
                return Result.success();
            }
            
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
} 