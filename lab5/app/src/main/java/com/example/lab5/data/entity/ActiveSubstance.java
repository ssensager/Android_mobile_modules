package com.example.lab5.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// Вынес действующие вещества в отдельный справочник, чтобы связывать с ними препараты и правила совместимости.
@Entity(tableName = "active_substance")
public class ActiveSubstance {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    @NonNull
    public String description;

    public ActiveSubstance(@NonNull String name, @NonNull String description) {
        this.name = name;
        this.description = description;
    }

    @Ignore
    public ActiveSubstance(int id, @NonNull String name, @NonNull String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
