package com.tht.hatirlatik.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tht.hatirlatik.model.RoutineSettings;
import java.util.List;

@Dao
public interface RoutineSettingsDao {
    @Insert
    long insert(RoutineSettings routineSettings);

    @Update
    void update(RoutineSettings routineSettings);

    @Delete
    void delete(RoutineSettings routineSettings);

    @Query("SELECT * FROM routine_settings WHERE taskId = :taskId")
    LiveData<RoutineSettings> getRoutineSettingsByTaskId(long taskId);

    @Query("SELECT * FROM routine_settings WHERE taskId = :taskId")
    RoutineSettings getRoutineSettingsByTaskIdSync(long taskId);

    @Query("DELETE FROM routine_settings WHERE taskId = :taskId")
    void deleteRoutineSettingsByTaskId(long taskId);
} 