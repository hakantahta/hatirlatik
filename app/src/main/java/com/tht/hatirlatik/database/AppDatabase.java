package com.tht.hatirlatik.database;

import android.content.Context;
import android.util.Log;
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
    private static final String TAG = "AppDatabase";

    public abstract TaskDao taskDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null || !instance.isOpen()) {
            try {
                Log.d(TAG, "Veritabanı oluşturuluyor veya yeniden açılıyor");
                instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries() // Widget için gerekli
                        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                        .build();
                Log.d(TAG, "Veritabanı başarıyla oluşturuldu");
            } catch (Exception e) {
                Log.e(TAG, "Veritabanı oluşturulurken hata: " + e.getMessage(), e);
                return null; // Hata durumunda null döndür
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