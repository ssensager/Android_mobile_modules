package com.example.lab5.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lab5.data.entity.Intake;

import java.util.List;

// Запросы к таблице приёмов нужны и для экрана деталей, и для фоновой отметки из уведомления.
@Dao
public interface IntakeDao {

    @Query("SELECT * FROM intake WHERE medicineId = :medicineId ORDER BY scheduledAtMs DESC")
    LiveData<List<Intake>> getByMedicineId(int medicineId);

    @Query("SELECT * FROM intake WHERE id = :intakeId LIMIT 1")
    Intake getByIdSync(int intakeId);

    @Insert
    long insert(Intake intake);

    @Insert
    void insertAll(List<Intake> intakes);

    @Update
    void update(Intake intake);

    @Query("UPDATE intake SET isTaken = :taken WHERE id = :intakeId")
    void markTaken(int intakeId, boolean taken);

    @Delete
    void delete(Intake intake);
}
