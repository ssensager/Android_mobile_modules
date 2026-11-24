package com.example.lab5.data.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;

// Собираю препарат вместе с его действующим веществом и списком приёмов за одну транзакцию Room.
public class MedicineWithDetails {

    @Embedded
    public Medicine medicine;

    @Relation(
            parentColumn = "substanceId",
            entityColumn = "id"
    )
    public ActiveSubstance substance;

    @Relation(
            parentColumn = "id",
            entityColumn = "medicineId"
    )
    public List<Intake> intakes = new ArrayList<>();

    public int getTakenCount() {
        if (intakes == null) {
            return 0;
        }
        int count = 0;
        for (Intake intake : intakes) {
            if (intake.isTaken) {
                count++;
            }
        }
        return count;
    }

    public int getTotalIntakesCount() {
        return intakes == null ? 0 : intakes.size();
    }
}
