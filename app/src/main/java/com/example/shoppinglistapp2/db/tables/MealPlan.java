package com.example.shoppinglistapp2.db.tables;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "meal_plans")
public class MealPlan {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;

    public MealPlan() {
    }

    @Ignore
    public MealPlan(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Ignore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MealPlan mealPlan = (MealPlan) o;
        return id == mealPlan.id && Objects.equals(name, mealPlan.name);
    }

    @Ignore
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
