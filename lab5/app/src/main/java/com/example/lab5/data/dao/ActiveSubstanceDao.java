package com.example.lab5.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.lab5.data.entity.ActiveSubstance;

import java.util.List;

// Вынес запросы к справочнику действующих веществ в отдельный DAO.
@Dao
public interface ActiveSubstanceDao {

    @Query("SELECT * FROM active_substance ORDER BY name ASC")
    LiveData<List<ActiveSubstance>> getAll();

    @Query("SELECT * FROM active_substance ORDER BY name ASC")
    List<ActiveSubstance> getAllSync();

    @Insert
    long insert(ActiveSubstance substance);

    @Insert
    void insertAll(List<ActiveSubstance> substances);

    @Query("SELECT COUNT(*) FROM active_substance")
    int countSync();
}
