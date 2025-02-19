package com.tht.hatirlatik.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.util.Date;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "dateTime")
    private Date dateTime;
    
    @ColumnInfo(name = "reminderMinutes")
    private int reminderMinutes; // Bildirim için kaç dakika önce
    
    @ColumnInfo(name = "notificationType")
    private NotificationType notificationType;
    
    @ColumnInfo(name = "repeatType")
    private RepeatType repeatType;
    
    @ColumnInfo(name = "isCompleted")
    private boolean isCompleted;
    
    @ColumnInfo(name = "createdAt")
    private Date createdAt;
    
    @ColumnInfo(name = "updatedAt")
    private Date updatedAt;

    public Task(String title, String description, Date dateTime, int reminderMinutes, 
                NotificationType notificationType) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.reminderMinutes = reminderMinutes;
        this.notificationType = notificationType;
        this.repeatType = RepeatType.NONE;
        this.isCompleted = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Date getDateTime() { return dateTime; }
    public int getReminderMinutes() { return reminderMinutes; }
    public NotificationType getNotificationType() { return notificationType; }
    public boolean isCompleted() { return isCompleted; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public RepeatType getRepeatType() {
        return repeatType;
    }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDateTime(Date dateTime) { this.dateTime = dateTime; }
    public void setReminderMinutes(int reminderMinutes) { this.reminderMinutes = reminderMinutes; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public void setCompleted(boolean completed) { 
        isCompleted = completed;
        updatedAt = new Date();
    }
    public void setRepeatType(RepeatType repeatType) {
        this.repeatType = repeatType;
        this.updatedAt = new Date();
    }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // equals ve hashCode metodları
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
} 