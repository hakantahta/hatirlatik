package com.tht.hatirlatik.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.util.Log;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class TaskListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "TaskListRemoteViewsFactory";
    private final Context context;
    private List<Task> tasks;
    private AppDatabase database;
    private final SimpleDateFormat dateFormat;

    public TaskListRemoteViewsFactory(Context context) {
        this.context = context;
        this.tasks = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("tr"));
    }

    @Override
    public void onCreate() {
        try {
            database = AppDatabase.getInstance(context);
            loadTasks();
        } catch (Exception e) {
            Log.e(TAG, "Veritabanı başlatılırken hata: " + e.getMessage());
            database = null;
        }
    }

    private void loadTasks() {
        if (database == null) {
            Log.e(TAG, "Veritabanı başlatılmamış");
            tasks = new ArrayList<>();
            return;
        }

        try {
            // Senkron olarak yükle
            List<Task> newTasks = database.taskDao().getActiveTasksForWidget();
            if (newTasks != null) {
                tasks = new ArrayList<>(newTasks);
                Log.d(TAG, "Widget için " + tasks.size() + " aktif görev yüklendi");
            } else {
                tasks = new ArrayList<>();
                Log.w(TAG, "Veritabanından görev listesi alınamadı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Veriler yüklenirken hata: " + e.getMessage());
            tasks = new ArrayList<>();
        }
    }

    @Override
    public void onDataSetChanged() {
        loadTasks();
    }

    @Override
    public void onDestroy() {
        tasks.clear();
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        try {
            if (position == -1 || tasks == null || position >= tasks.size()) {
                return null;
            }

            Task task = tasks.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);

            views.setTextViewText(R.id.task_title, task.getTitle());
            String formattedDate = dateFormat.format(task.getDateTime());
            views.setTextViewText(R.id.task_date, formattedDate);
            views.setInt(R.id.task_status_indicator, "setBackgroundResource", 
                    task.isCompleted() ? R.color.task_status_completed : R.color.accent);

            Intent fillInIntent = new Intent();
            fillInIntent.putExtra("task_id", task.getId());
            views.setOnClickFillInIntent(R.id.widget_task_item, fillInIntent);

            return views;
        } catch (Exception e) {
            Log.e(TAG, "Görünüm oluşturulurken hata: " + e.getMessage());
            return null;
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
} 