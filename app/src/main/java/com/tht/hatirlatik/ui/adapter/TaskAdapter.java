package com.tht.hatirlatik.ui.adapter;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {
    
    private final TaskItemListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("tr"));

    public TaskAdapter(TaskItemListener listener) {
        super(new TaskDiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task);
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final TextView dateTimeTextView;
        private final CheckBox checkBox;
        private final ImageButton menuButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_task_title);
            descriptionTextView = itemView.findViewById(R.id.text_task_description);
            dateTimeTextView = itemView.findViewById(R.id.text_task_datetime);
            checkBox = itemView.findViewById(R.id.checkbox_task);
            menuButton = itemView.findViewById(R.id.image_task_menu);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskClicked(getItem(position));
                }
            });

            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskCheckedChanged(getItem(position), checkBox.isChecked());
                }
            });

            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskDeleteClicked(getItem(position));
                }
            });
        }

        public void bind(Task task) {
            titleTextView.setText(task.getTitle());
            descriptionTextView.setText(task.getDescription());
            dateTimeTextView.setText(dateFormat.format(task.getDateTime()));
            checkBox.setChecked(task.isCompleted());

            // Tamamlanmış görevlerin görünümünü güncelle
            float alpha = task.isCompleted() ? 0.5f : 1.0f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                titleTextView.setAlpha(alpha);
                descriptionTextView.setAlpha(alpha);
                dateTimeTextView.setAlpha(alpha);
            } else {
                // API 11'den düşük sürümler için alternatif yöntem
                int visibility = task.isCompleted() ? View.GONE : View.VISIBLE;
                titleTextView.setVisibility(visibility);
                descriptionTextView.setVisibility(visibility);
                dateTimeTextView.setVisibility(visibility);
            }
        }
    }

    public interface TaskItemListener {
        void onTaskClicked(Task task);
        void onTaskCheckedChanged(Task task, boolean isChecked);
        void onTaskDeleteClicked(Task task);
    }

    private static class TaskDiffCallback extends DiffUtil.ItemCallback<Task> {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.equals(newItem);
        }
    }
} 