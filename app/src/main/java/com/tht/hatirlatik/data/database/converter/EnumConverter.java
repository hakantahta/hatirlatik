package com.tht.hatirlatik.data.database.converter;

import androidx.room.TypeConverter;
import com.tht.hatirlatik.domain.model.NotificationType;
import com.tht.hatirlatik.domain.model.RepeatType;
import com.tht.hatirlatik.domain.model.TaskPriority;

/**
 * Room veritabanı için Enum tip dönüştürücü
 */
public class EnumConverter {
    @TypeConverter
    public static NotificationType toNotificationType(String value) {
        return value == null ? null : NotificationType.valueOf(value);
    }

    @TypeConverter
    public static String fromNotificationType(NotificationType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static RepeatType toRepeatType(String value) {
        return value == null ? null : RepeatType.valueOf(value);
    }

    @TypeConverter
    public static String fromRepeatType(RepeatType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static TaskPriority toTaskPriority(String value) {
        return value == null ? null : TaskPriority.valueOf(value);
    }

    @TypeConverter
    public static String fromTaskPriority(TaskPriority priority) {
        return priority == null ? null : priority.name();
    }
} 