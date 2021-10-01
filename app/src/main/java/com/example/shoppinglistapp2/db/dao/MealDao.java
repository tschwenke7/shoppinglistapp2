package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;

import java.util.List;

@Dao
public interface MealDao extends BaseDao<Meal> {
    @Query("DELETE FROM meals WHERE plan_id = :planId")
    void deleteAllFromPlan(int planId);

    @Query("UPDATE meals " +
            "SET recipe_id = NULL, notes = NULL " +
            "WHERE plan_id = :planId")
    void clearAllDays(int planId);

    @Query("SELECT * FROM meals WHERE plan_id = :planId")
    LiveData<List<Meal>> getAllFromPlan(int planId);

    @Query("SELECT * FROM meals WHERE day_id = :dayId AND plan_id = :planId")
    Meal getByDayAndPlan(int planId, int dayId);

    @Query("SELECT * FROM meals WHERE id = :id")
    Meal getById(int id);

    @Query("DELETE FROM meals WHERE plan_id = :planId")
    void deleteAllWithPlanId(int planId);

    @Transaction
    @Query("SELECT * FROM meals WHERE plan_id = :mealPlanId")
    LiveData<List<MealWithRecipe>> getPopulatedById(int mealPlanId);
}
