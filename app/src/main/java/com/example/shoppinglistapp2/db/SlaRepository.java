package com.example.shoppinglistapp2.db;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;

public class SlaRepository {
    private IngredientDao ingredientDao;
    private RecipeDao recipeDao;

    private LiveData<List<Recipe>> allRecipes;

    public SlaRepository(Application application){
        SlaDatabase db = SlaDatabase.getDatabase(application);
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
