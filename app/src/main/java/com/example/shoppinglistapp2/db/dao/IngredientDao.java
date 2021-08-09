package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.Ingredient;

import java.util.List;

@Dao
public interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE recipe_id = :id")
    public LiveData<List<Ingredient>> getIngredientsByRecipeId(int id);

    @Query("SELECT * FROM ingredients WHERE recipe_id = :id")
    List<Ingredient> getIngredientsByRecipeIdNonLive(int id);

    @Insert
    void insertAll(Ingredient... ingredients);

    @Insert
    void insertAll(List<Ingredient> ingredients);

    @Delete
    void deleteAll(Ingredient... ingredients);

    @Delete
    int deleteAll(List<Ingredient> ingredients);

    @Update
    void update(Ingredient ingredient);


}
