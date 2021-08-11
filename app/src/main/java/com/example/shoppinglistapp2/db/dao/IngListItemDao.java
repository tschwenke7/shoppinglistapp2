package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.tables.IngListItem;

import java.util.List;

@Dao
public interface IngListItemDao extends BaseDao<IngListItem> {
    @Query("SELECT * FROM ing_list_items WHERE listId = :id ORDER BY checked")
    public LiveData<List<IngListItem>> getIngredientsByListId(int id);

    @Query("SELECT * FROM ing_list_items WHERE listId = :id")
    List<IngListItem> getIngredientsByListIdNonLive(int id);

    @Query("SELECT * FROM ing_list_items WHERE listId = :listId AND checked = 0")
    List<IngListItem> getAllUncheckedNonLive(int listId);

    @Query("SELECT * FROM ing_list_items WHERE listId = :listId AND name = :name AND checked = :checked AND id != :id")
    IngListItem getAnotherByName(long listId, String name, boolean checked, int id);

    @Query("DELETE FROM ing_list_items WHERE listId = :listId AND checked = 1")
    void clearAllChecked(int listId);

    @Query("DELETE FROM ing_list_items WHERE listId = :listId")
    void clearAll(int listId);

    @Query("SELECT ing_list_items.id, name,listId, checked, mass_unit, mass_qty, other_qty, other_unit, other_qty, volume_qty, volume_unit, whole_item_qty FROM ing_list INNER JOIN ing_list_items ON ing_list.id = ing_list_items.listId WHERE ing_list.meal_plan_id = :mealPlanId")
    LiveData<List<IngListItem>> getAllFromMealPlan(int mealPlanId);

    @Query("SELECT ing_list_items.id, name,listId, checked, mass_unit, mass_qty, other_qty, other_unit, other_qty, volume_qty, volume_unit, whole_item_qty FROM ing_list INNER JOIN ing_list_items ON ing_list.id = ing_list_items.listId WHERE ing_list.recipe_id = :recipeId")
    LiveData<List<IngListItem>> getAllFromRecipe(int recipeId);
}
