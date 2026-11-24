package com.example.lab5.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.lab5.data.entity.Medicine;
import com.example.lab5.data.entity.MedicineWithDetails;

import java.util.List;

// Использую @Transaction для составных выборок, чтобы Room читал препарат и связи согласованно.
@Dao
public interface MedicineDao {

    @Transaction
    @Query("SELECT * FROM medicine ORDER BY id DESC")
    LiveData<List<MedicineWithDetails>> getAllWithDetails();

    @Transaction
    @Query("SELECT * FROM medicine WHERE id = :medicineId LIMIT 1")
    LiveData<MedicineWithDetails> getByIdWithDetails(int medicineId);

    @Transaction
    @Query("SELECT * FROM medicine WHERE id = :medicineId LIMIT 1")
    MedicineWithDetails getByIdWithDetailsSync(int medicineId);

    @Insert
    long insert(Medicine medicine);

    @Update
    void update(Medicine medicine);

    @Delete
    void delete(Medicine medicine);
}
