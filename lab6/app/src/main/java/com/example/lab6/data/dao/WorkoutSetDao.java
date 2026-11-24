package com.example.lab6.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.lab6.data.entity.WorkoutSet;
import com.example.lab6.data.projection.DailyVolumePoint;
import com.example.lab6.data.projection.WorkoutSetWithEquipmentName;

import java.util.List;

/**
 * DAO для подходов и аналитики тренировок.
 * Все агрегаты (COUNT, SUM, MAX), группировка по дням и удаление старых записей выполняются в SQL.
 */
@Dao
public interface WorkoutSetDao {

    @Insert
    long insert(WorkoutSet workoutSet);

    @Delete
    void delete(WorkoutSet workoutSet);

    @Query("SELECT * FROM workout_sets WHERE equipmentId = :equipmentId ORDER BY dateMs DESC, id DESC")
    LiveData<List<WorkoutSet>> getSetsForEquipment(int equipmentId);

    /**
     * Синхронно читаю текущий рекорд по тренажёру перед добавлением нового подхода.
     * Вызываю строго в фоновом потоке.
     */
    @Query("SELECT MAX(weightKg) FROM workout_sets WHERE equipmentId = :equipmentId")
    Float getMaxWeightForEquipmentSync(int equipmentId);

    @Query("SELECT COUNT(*) FROM workout_sets WHERE dateMs >= :startOfDayMs")
    LiveData<Integer> getTodaySetsCount(long startOfDayMs);

    @Query("SELECT COALESCE(SUM(weightKg * reps), 0) FROM workout_sets WHERE dateMs >= :startOfDayMs")
    LiveData<Float> getTodayVolumeKg(long startOfDayMs);

    /**
     * Лучший общий рекорд по всем тренажёрам с названием упражнения через INNER JOIN.
     */
    @Query("SELECT ws.id, ws.equipmentId, ws.weightKg, ws.reps, ws.dateMs, " +
            "e.name AS equipmentName, e.muscleGroup AS muscleGroup " +
            "FROM workout_sets ws " +
            "INNER JOIN equipment e ON e.id = ws.equipmentId " +
            "ORDER BY ws.weightKg DESC, ws.reps DESC, ws.dateMs DESC " +
            "LIMIT 1")
    LiveData<WorkoutSetWithEquipmentName> getBestOverallRecord();

    /**
     * Полная история подходов с названиями тренажёров для экспорта в CSV.
     * Вызываю только в фоновом потоке.
     */
    @Query("SELECT ws.id, ws.equipmentId, ws.weightKg, ws.reps, ws.dateMs, " +
            "e.name AS equipmentName, e.muscleGroup AS muscleGroup " +
            "FROM workout_sets ws " +
            "INNER JOIN equipment e ON e.id = ws.equipmentId " +
            "ORDER BY ws.dateMs DESC, ws.id DESC")
    List<WorkoutSetWithEquipmentName> getAllSetsWithEquipmentNameSync();

    /**
     * Считаю суммарный тоннаж по дням за выбранный период прямо в SQLite через GROUP BY.
     */
    @Query("SELECT strftime('%Y-%m-%d', dateMs / 1000, 'unixepoch', 'localtime') AS dayKey, " +
            "COALESCE(SUM(weightKg * reps), 0) AS totalVolume, " +
            "COUNT(*) AS setsCount " +
            "FROM workout_sets " +
            "WHERE dateMs >= :fromMs " +
            "GROUP BY dayKey " +
            "ORDER BY dayKey ASC")
    LiveData<List<DailyVolumePoint>> getDailyVolumeSince(long fromMs);

    @Query("SELECT COUNT(*) FROM workout_sets WHERE dateMs < :thresholdMs")
    LiveData<Integer> countOlderThan(long thresholdMs);

    @Query("SELECT COUNT(*) FROM workout_sets WHERE dateMs < :thresholdMs")
    int countOlderThanSync(long thresholdMs);

    @Query("DELETE FROM workout_sets WHERE dateMs < :thresholdMs")
    int deleteOlderThan(long thresholdMs);
}
