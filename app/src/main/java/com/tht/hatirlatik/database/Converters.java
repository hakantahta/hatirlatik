package com.tht.hatirlatik.database;

import androidx.room.TypeConverter;
import com.tht.hatirlatik.model.NotificationType;
import java.util.Date;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static NotificationType fromString(String value) {
        return value == null ? null : NotificationType.valueOf(value);
    }

    @TypeConverter
    public static String notificationTypeToString(NotificationType type) {
        return type == null ? null : type.name();
    }
} 