package com.example.lab5.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.lab5.data.entity.ConflictRule;

import java.util.List;

// Доступ к таблице правил несовместимости держу отдельно от логики проверки.
@Dao
public interface ConflictRuleDao {

    @Query("SELECT * FROM conflict_rule ORDER BY id ASC")
    LiveData<List<ConflictRule>> getAll();

    @Query("SELECT * FROM conflict_rule ORDER BY id ASC")
    List<ConflictRule> getAllSync();

    @Insert
    long insert(ConflictRule rule);

    @Insert
    void insertAll(List<ConflictRule> rules);
}
