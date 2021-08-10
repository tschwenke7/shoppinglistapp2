package com.example.shoppinglistapp2.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.MealPlan;

@Dao
public interface MealPlanDao {
    @Update
    void update(MealPlan mealPlan);

    @Insert
    long insert(MealPlan mealPlan);

    @Delete
    void delete(MealPlan mealPlan);
}
