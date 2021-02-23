package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.tables.Ingredient;

import java.util.List;

@Dao
public interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE recipe_id = :id")
    public LiveData<List<Ingredient>> getIngredientsByRecipeId(int id);

    @Query("SELECT * FROM ingredients WHERE recipe_id = :id")
    List<Ingredient> getIngredientsByRecipeIdNonLive(int id);

    @Insert
    public void insertAll(Ingredient... ingredients);

    @Delete
    public void deleteAll(Ingredient... ingredients);


}
