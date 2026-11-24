package com.example.lab6.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Сущность тренажёра или упражнения.
 * Проиндексировал поля name и muscleGroup, чтобы поиск и фильтрация в SQL работали без полного перебора.
 */
@Entity(tableName = "equipment")
public class Equipment {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(index = true)
    public String name;

    @ColumnInfo(index = true)
    public String muscleGroup;

    public String notes;

    public String photoPath;

    public long createdAt;

    public Equipment() {
    }

    @Ignore
    public Equipment(String name, String muscleGroup, String notes, String photoPath, long createdAt) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.notes = notes;
        this.photoPath = photoPath;
        this.createdAt = createdAt;
    }
}
