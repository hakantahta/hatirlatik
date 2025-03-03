package com.tht.hatirlatik.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.model.Task;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        private final TextView statusTextView;
        private final ImageView statusImageView;
        private final TextView overdueTextView;
        private final ImageView overdueImageView;
        private final ImageButton menuButton;
        private final CardView cardView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_task_title);
            descriptionTextView = itemView.findViewById(R.id.text_task_description);
            dateTimeTextView = itemView.findViewById(R.id.chip_task_datetime);
            statusTextView = itemView.findViewById(R.id.chip_task_status);
            statusImageView = null;
            overdueTextView = itemView.findViewById(R.id.text_task_overdue);
            overdueImageView = null;
            menuButton = itemView.findViewById(R.id.image_task_menu);
            cardView = itemView.findViewById(R.id.card_view);

            // Kısa tıklama ile detay sayfasına git
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskClicked(getItem(position));
                }
            });

            // Uzun basma ile görevi tamamla/aktifleştir
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = getItem(position);
                    listener.onTaskCheckedChanged(task, !task.isCompleted());
                    
                    // Görsel geri bildirim için hafif bir animasyon
                    cardView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            cardView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                        })
                        .start();
                    return true;
                }
                return false;
            });

            // Menü butonu ile popup menü göster
            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = getItem(position);
                    showPopupMenu(v, task);
                }
            });
        }

        private void showPopupMenu(View view, Task task) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_task_item, popup.getMenu());
            
            // Tamamlanma durumuna göre menü metnini güncelle
            popup.getMenu().findItem(R.id.action_toggle_complete)
                .setTitle(task.isCompleted() ? "Aktif Yap" : "Tamamla");

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_toggle_complete) {
                    // Görsel geri bildirim için animasyon
                    cardView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            cardView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                            
                            // Durumu değiştir
                            boolean newStatus = !task.isCompleted();
                            task.setCompleted(newStatus); // Görevi hemen güncelle
                            
                            // Görsel değişikliği hemen uygula
                            bind(task);
                            
                            // Veritabanını güncelle
                            listener.onTaskCheckedChanged(task, newStatus);
                        })
                        .start();
                    return true;
                } else if (itemId == R.id.action_delete) {
                    listener.onTaskDeleteClicked(task);
                    return true;
                }
                return false;
            });

            popup.show();
        }

        public void bind(Task task) {
            titleTextView.setText(task.getTitle());
            descriptionTextView.setText(task.getDescription());
            dateTimeTextView.setText(dateFormat.format(task.getDateTime()));

            // Görev süresinin geçip geçmediğini kontrol et
            boolean isOverdue = task.getDateTime().before(new Date());

            // Görev durumu metnini ayarla
            if (task.isCompleted()) {
                statusTextView.setText(R.string.task_completed);
                // Tamamlanmış görevler için kırmızı arka plan
                cardView.setCardBackgroundColor(cardView.getContext().getResources().getColor(
                    isNightMode() ? R.color.task_completed_background_dark : R.color.task_completed_background));
            } else {
                statusTextView.setText(R.string.task_active);
                // Aktif görevler için yeşil arka plan
                cardView.setCardBackgroundColor(cardView.getContext().getResources().getColor(
                    isNightMode() ? R.color.task_active_background_dark : R.color.task_active_background));
            }

            // Tarihi geçen görevler için ek bildirim göster/gizle
            if (isOverdue && !task.isCompleted()) {
                overdueTextView.setVisibility(View.VISIBLE);
            } else {
                overdueTextView.setVisibility(View.GONE);
            }

            // Tamamlanmış görevlerin görünümünü güncelle
            float alpha = task.isCompleted() ? 0.5f : 1.0f;
            titleTextView.setAlpha(alpha);
            descriptionTextView.setAlpha(alpha);
            dateTimeTextView.setAlpha(alpha);
            statusTextView.setAlpha(alpha);
            overdueTextView.setAlpha(alpha);
            
            // Görsel geri bildirim için animasyon
            cardView.animate()
                .alpha(alpha)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start();
        }
        
        private boolean isNightMode() {
            int nightModeFlags = cardView.getContext().getResources().getConfiguration().uiMode & 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }

        public CardView getCardView() {
            return cardView;
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