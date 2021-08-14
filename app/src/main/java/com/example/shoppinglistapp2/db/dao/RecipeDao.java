package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithIngredients;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;

import java.util.List;

@Dao
public interface RecipeDao extends BaseDao<Recipe> {

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    LiveData<List<Recipe>> getAllAlphabetical();

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    LiveData<List<RecipeWithTagsAndIngredients>> getAllPopulatedAlphabetical();

    @Query("SELECT * FROM recipes WHERE id = :id")
    Recipe getById(int id);

    @Query("SELECT * FROM recipes WHERE id = :id")
    LiveData<Recipe> getByIdLive(int id);

    @Query("SELECT * FROM recipes WHERE name = :name LIMIT 1")
    Recipe getByName(String name);

    @Query("DELETE FROM recipes")
    void deleteEverything();

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY recipes.name ASC")
    LiveData<List<RecipeWithIngredients>> getAllWithIngLists();

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY recipes.name ASC")
    LiveData<List<RecipeWithTagsAndIngredients>> getAllWithTagsAndIngLists();

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    RecipeWithTagsAndIngredients getPopulatedByIdNonLive(int id);

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    LiveData<RecipeWithIngredients> getByIdLiveWithIngList(int id);

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    RecipeWithIngredients getByIdWithIngList(int id);
}
