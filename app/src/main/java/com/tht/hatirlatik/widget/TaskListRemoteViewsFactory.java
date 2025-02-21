package com.tht.hatirlatik.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.model.Task;

import java.util.List;

public class TaskListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<Task> tasks;
    private AppDatabase database;
    private final SimpleDateFormat dateFormat;

    public TaskListRemoteViewsFactory(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("tr"));
    }

    @Override
    public void onCreate() {
        database = AppDatabase.getInstance(context);
    }

    @Override
    public void onDataSetChanged() {
        // Aktif görevleri veritabanından al
        tasks = database.taskDao().getActiveTasksForWidget();
    }

    @Override
    public void onDestroy() {
        tasks = null;
    }

    @Override
    public int getCount() {
        return tasks == null ? 0 : tasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == -1 || tasks == null || tasks.size() <= position) {
            return null;
        }

        Task task = tasks.get(position);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);
        rv.setTextViewText(R.id.task_title, task.getTitle());
        rv.setTextViewText(R.id.task_date, dateFormat.format(task.getDateTime()));

        // Görev detayına gitmek için intent ayarla
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("task_id", task.getId());
        rv.setOnClickFillInIntent(R.id.widget_task_item, fillInIntent);

        return rv;
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
        return tasks.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
} 