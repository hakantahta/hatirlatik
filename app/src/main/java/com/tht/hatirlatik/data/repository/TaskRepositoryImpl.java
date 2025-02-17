package com.tht.hatirlatik.data.repository;

import com.tht.hatirlatik.data.database.dao.TaskDao;
import com.tht.hatirlatik.data.database.entity.TaskEntity;
import com.tht.hatirlatik.domain.model.Task;
import com.tht.hatirlatik.domain.repository.TaskRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TaskRepository implementasyonu
 */
public class TaskRepositoryImpl implements TaskRepository {
    private final TaskDao taskDao;

    public TaskRepositoryImpl(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @Override
    public long insertTask(Task task) {
        return taskDao.insert(toEntity(task));
    }

    @Override
    public void updateTask(Task task) {
        taskDao.update(toEntity(task));
    }

    @Override
    public void deleteTask(long taskId) {
        TaskEntity entity = taskDao.getById(taskId);
        if (entity != null) {
            taskDao.delete(entity);
        }
    }

    @Override
    public Task getTaskById(long taskId) {
        TaskEntity entity = taskDao.getById(taskId);
        return entity != null ? toModel(entity) : null;
    }

    @Override
    public List<Task> getAllTasks() {
        return toModelList(taskDao.getAll());
    }

    @Override
    public List<Task> getTasksByDate(Date date) {
        return toModelList(taskDao.getByDate(date));
    }

    @Override
    public List<Task> getTasksByDateRange(Date startDate, Date endDate) {
        return toModelList(taskDao.getByDateRange(startDate, endDate));
    }

    @Override
    public List<Task> getCompletedTasks() {
        return toModelList(taskDao.getCompleted());
    }

    @Override
    public List<Task> getIncompleteTasks() {
        return toModelList(taskDao.getIncomplete());
    }

    @Override
    public List<Task> getUpcomingTasks(int limit) {
        return toModelList(taskDao.getUpcoming(new Date(), limit));
    }

    @Override
    public List<Task> getTasksWithReminders() {
        return toModelList(taskDao.getWithReminders());
    }

    @Override
    public int getCompletedTaskCount() {
        return taskDao.getCompletedCount();
    }

    @Override
    public int getIncompleteTaskCount() {
        return taskDao.getIncompleteCount();
    }

    @Override
    public int getTotalTaskCount() {
        return taskDao.getTotalCount();
    }

    // Entity -> Model dönüşümü
    private Task toModel(TaskEntity entity) {
        Task task = new Task();
        task.setId(entity.getId());
        task.setTitle(entity.getTitle());
        task.setDescription(entity.getDescription());
        task.setDueDate(entity.getDueDate());
        task.setReminderTime(entity.getReminderTime());
        task.setReminderMinutesBefore(entity.getReminderMinutesBefore());
        task.setCompleted(entity.isCompleted());
        task.setPriority(entity.getPriority());
        task.setRepeatType(entity.getRepeatType());
        task.setNotificationType(entity.getNotificationType());
        return task;
    }

    // Model -> Entity dönüşümü
    private TaskEntity toEntity(Task task) {
        TaskEntity entity = new TaskEntity();
        entity.setId(task.getId());
        entity.setTitle(task.getTitle());
        entity.setDescription(task.getDescription());
        entity.setDueDate(task.getDueDate());
        entity.setReminderTime(task.getReminderTime());
        entity.setReminderMinutesBefore(task.getReminderMinutesBefore());
        entity.setCompleted(task.isCompleted());
        entity.setPriority(task.getPriority());
        entity.setRepeatType(task.getRepeatType());
        entity.setNotificationType(task.getNotificationType());
        entity.setCreatedAt(task.getCreatedAt());
        entity.setUpdatedAt(task.getUpdatedAt());
        return entity;
    }

    // Entity listesi -> Model listesi dönüşümü
    private List<Task> toModelList(List<TaskEntity> entities) {
        List<Task> tasks = new ArrayList<>();
        for (TaskEntity entity : entities) {
            tasks.add(toModel(entity));
        }
        return tasks;
    }
} 