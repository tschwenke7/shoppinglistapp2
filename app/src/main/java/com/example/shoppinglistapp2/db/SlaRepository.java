package com.example.shoppinglistapp2.db;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;

public class SlaRepository {
    private IngredientDao ingredientDao;
    private RecipeDao recipeDao;

    private LiveData<List<Recipe>> allRecipes;

    public SlaRepository(Context context){
        SlaDatabase db = SlaDatabase.getDatabase(context);
        ingredientDao = db.ingredientDao();
        recipeDao = db.recipeDao();
        allRecipes = recipeDao.getAllAlphabetical();
    }

    public LiveData<List<Recipe>> getAllRecipes(){
        return allRecipes;
    }

    public void insert(final Recipe recipe){
        SlaDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.insertAll(recipe);
        });
    }
}
