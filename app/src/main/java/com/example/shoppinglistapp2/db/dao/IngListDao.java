package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.relations.IngListWithItems;

@Dao()
public interface IngListDao extends BaseDao<IngList> {

    @Query("SELECT * FROM ing_lists WHERE id = :id")
    IngList getById(int id);

    @Query("INSERT INTO ing_lists(id) VALUES(:id)")
    void insertShoppingList(int id);

    @Transaction
    @Query("SELECT * FROM ing_lists WHERE meal_plan_id = :mealPlanId")
    LiveData<IngListWithItems> getIngListForMealPlan(int mealPlanId);

    @Query("SELECT id FROM ing_lists WHERE meal_plan_id = :mealPlanId")
    int getIngListIdForMealPlan(int mealPlanId);

    @Query("SELECT EXISTS(SELECT * FROM ing_lists WHERE id = :listId)")
    boolean listIdExists(int listId);
}
