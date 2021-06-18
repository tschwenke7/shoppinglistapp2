package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.MealPlan;

import java.util.List;

@Dao
public interface MealPlanDao {
    @Query("DELETE FROM meal_plans WHERE plan_id = :planId")
    void deleteAll(int planId);

    @Query("UPDATE meal_plans " +
            "SET recipe_id = NULL, notes = NULL " +
            "WHERE plan_id = :planId")
    void clearAllDays(int planId);

    @Update
    void update(MealPlan mealPlan);

    @Query("SELECT * FROM meal_plans WHERE plan_id = :planId")
    LiveData<List<MealPlan>> getAll(int planId);

    @Query("SELECT * FROM meal_plans WHERE day_id = :dayId AND plan_id = :planId")
    MealPlan getByDayId(int planId, int dayId);

    @Insert
    long insert(MealPlan mealPlan);

    @Delete
    void delete(MealPlan mealPlan);
}
