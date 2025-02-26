package com.tht.hatirlatik.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.tht.hatirlatik.model.Task;

@Database(entities = {Task.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "hatirlatik_db";
    private static volatile AppDatabase instance;

    public abstract TaskDao taskDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null || !instance.isOpen()) {
            try {
                android.util.Log.d("AppDatabase", "Veritabanı oluşturuluyor veya yeniden açılıyor");
                instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                        .build();
                android.util.Log.d("AppDatabase", "Veritabanı başarıyla oluşturuldu");
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Veritabanı oluşturulurken hata: " + e.getMessage(), e);
                throw e; // Hatayı yukarı fırlat
            }
        }
        return instance;
    }

    public static void destroyInstance() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        instance = null;
    }
} 