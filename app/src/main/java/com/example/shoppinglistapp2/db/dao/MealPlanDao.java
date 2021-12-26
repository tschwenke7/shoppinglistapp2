package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.google.common.util.concurrent.ListenableFuture;

@Dao
public interface MealPlanDao extends BaseDao<MealPlan> {
    @Query("SELECT id FROM meal_plans ORDER BY id DESC LIMIT 1")
    ListenableFuture<Integer> getLatestId();

    @Query("SELECT * FROM meal_plans WHERE id = :mealPlanId")
    LiveData<MealPlan> getById(int mealPlanId);
}
