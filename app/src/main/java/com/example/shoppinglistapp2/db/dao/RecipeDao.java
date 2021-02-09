package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;

@Dao
public interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    public LiveData<List<Recipe>> getAllAlphabetical();

    @Query("SELECT * FROM recipes WHERE id = :id")
    public Recipe getById(int id);

    @Query("SELECT * FROM recipes WHERE name = :name LIMIT 1")
    public Recipe getByName(String name);

    @Insert
    public long insert(Recipe recipe);

    @Delete
    public void deleteAll(Recipe... recipes);
}
