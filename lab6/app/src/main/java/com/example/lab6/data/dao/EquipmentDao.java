package com.example.lab6.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lab6.data.entity.Equipment;
import com.example.lab6.data.projection.EquipmentWithMaxSet;

import java.util.List;

/**
 * DAO для работы с тренажёрами.
 * Вынес поиск, фильтрацию по группе мышц, подсчёт подходов и поиск рекорда в единый SQL-запрос.
 */
@Dao
public interface EquipmentDao {

    @Insert
    long insert(Equipment equipment);

    @Update
    void update(Equipment equipment);

    @Delete
    void delete(Equipment equipment);

    @Query("SELECT * FROM equipment WHERE id = :id LIMIT 1")
    LiveData<Equipment> getById(int id);

    @Query("SELECT * FROM equipment WHERE id = :id LIMIT 1")
    Equipment getByIdSync(int id);

    /**
     * Комбинированный запрос для главного экрана:
     * LEFT JOIN с подзапросами сразу возвращает личный рекорд и последний подход,
     * а также фильтрует по группе мышц и подстроке названия прямо в SQLite.
     * Не загружаю notes и photoPath, чтобы экономить память списка.
     */
    @Query("SELECT e.id, e.name, e.muscleGroup, " +
            "COALESCE(ws_max.weightKg, 0) AS maxWeightKg, " +
            "COALESCE(ws_max.reps, 0) AS maxWeightReps, " +
            "(SELECT COUNT(*) FROM workout_sets ws_cnt WHERE ws_cnt.equipmentId = e.id) AS totalSets, " +
            "COALESCE(ws_last.weightKg, 0) AS lastWeightKg, " +
            "COALESCE(ws_last.reps, 0) AS lastReps " +
            "FROM equipment e " +
            "LEFT JOIN workout_sets ws_max ON ws_max.id = (" +
            "    SELECT id FROM workout_sets " +
            "    WHERE equipmentId = e.id " +
            "    ORDER BY weightKg DESC, reps DESC, dateMs DESC " +
            "    LIMIT 1" +
            ") " +
            "LEFT JOIN workout_sets ws_last ON ws_last.id = (" +
            "    SELECT id FROM workout_sets " +
            "    WHERE equipmentId = e.id " +
            "    ORDER BY dateMs DESC, id DESC " +
            "    LIMIT 1" +
            ") " +
            "WHERE (:muscleGroup = '' OR e.muscleGroup = :muscleGroup) " +
            "  AND (:query = '' OR e.name LIKE :query OR e.name LIKE :queryLower OR e.name LIKE :queryCap) " +
            "ORDER BY e.createdAt DESC")
    LiveData<List<EquipmentWithMaxSet>> searchEquipmentWithStats(
            String muscleGroup,
            String query,
            String queryLower,
            String queryCap
    );

    /**
     * Выбираю пути к фото тех тренажёров, у которых после автоочистки не осталось ни одного подхода.
     * Вызываю только в фоновом потоке.
     */
    @Query("SELECT photoPath FROM equipment " +
            "WHERE photoPath IS NOT NULL AND photoPath != '' " +
            "  AND createdAt < :thresholdMs " +
            "  AND id NOT IN (SELECT DISTINCT equipmentId FROM workout_sets)")
    List<String> getExpiredUnusedPhotoPathsSync(long thresholdMs);

    @Query("UPDATE equipment SET photoPath = NULL " +
            "WHERE photoPath IS NOT NULL AND photoPath != '' " +
            "  AND createdAt < :thresholdMs " +
            "  AND id NOT IN (SELECT DISTINCT equipmentId FROM workout_sets)")
    void clearExpiredUnusedPhotoPathsSync(long thresholdMs);
}
