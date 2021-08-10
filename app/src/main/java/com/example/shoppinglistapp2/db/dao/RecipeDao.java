package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;

@Dao
public interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    LiveData<List<Recipe>> getAllAlphabetical();

    @Query("SELECT * FROM recipes WHERE id = :id")
    Recipe getById(int id);

    @Query("SELECT * FROM recipes WHERE id = :id")
    LiveData<Recipe> getByIdLive(int id);

    @Query("SELECT * FROM recipes WHERE name = :name LIMIT 1")
    Recipe getByName(String name);

    @Insert
    long insert(Recipe recipe);

    @Delete
    void deleteAll(Recipe... recipes);

    @Update
    void updateRecipe(Recipe recipe);

    @Query("DELETE FROM recipes")
    void deleteEverything();
}
