package com.tht.hatirlatik.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import com.tht.hatirlatik.data.database.entity.TaskEntity;
import java.util.Date;
import java.util.List;

/**
 * Görev veritabanı erişim nesnesi
 */
@Dao
public interface TaskDao {
    @Insert
    long insert(TaskEntity task);

    @Update
    void update(TaskEntity task);

    @Delete
    void delete(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskEntity getById(long taskId);

    @Query("SELECT * FROM tasks ORDER BY due_date ASC")
    List<TaskEntity> getAll();

    @Query("SELECT * FROM tasks WHERE date(due_date/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    List<TaskEntity> getByDate(Date date);

    @Query("SELECT * FROM tasks WHERE due_date BETWEEN :startDate AND :endDate ORDER BY due_date ASC")
    List<TaskEntity> getByDateRange(Date startDate, Date endDate);

    @Query("SELECT * FROM tasks WHERE is_completed = 1 ORDER BY updated_at DESC")
    List<TaskEntity> getCompleted();

    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_date ASC")
    List<TaskEntity> getIncomplete();

    @Query("SELECT * FROM tasks WHERE is_completed = 0 AND due_date >= :now ORDER BY due_date ASC LIMIT :limit")
    List<TaskEntity> getUpcoming(Date now, int limit);

    @Query("SELECT * FROM tasks WHERE reminder_time IS NOT NULL AND is_completed = 0 ORDER BY reminder_time ASC")
    List<TaskEntity> getWithReminders();

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1")
    int getCompletedCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0")
    int getIncompleteCount();

    @Query("SELECT COUNT(*) FROM tasks")
    int getTotalCount();
} 