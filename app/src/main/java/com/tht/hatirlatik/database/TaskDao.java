package com.tht.hatirlatik.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tht.hatirlatik.model.Task;
import java.util.Date;
import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY dateTime ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dateTime ASC")
    LiveData<List<Task>> getActiveTasks();

    @Query("SELECT * FROM tasks WHERE dateTime BETWEEN :startDate AND :endDate")
    LiveData<List<Task>> getTasksBetweenDates(Date startDate, Date endDate);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskById(long taskId);

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    void updateTaskCompletionStatus(long taskId, boolean isCompleted);

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    void deleteCompletedTasks();

    @Query("SELECT * FROM tasks WHERE dateTime < :date AND isCompleted = 0")
    List<Task> getOverdueTasks(Date date);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dateTime ASC")
    List<Task> getActiveTasksForWidget();
} 