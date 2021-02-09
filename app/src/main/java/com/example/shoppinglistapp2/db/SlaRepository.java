package com.example.shoppinglistapp2.db;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    public long insertRecipe(final Recipe recipe){
        Callable<Long> insertCallable = () -> recipeDao.insert(recipe);
        long rowId = 0;

        Future<Long> future = SlaDatabase.databaseWriteExecutor.submit(insertCallable);
        try {
            rowId = future.get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return -1;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        }
        return rowId;
    }

    public void insertIngredient(final Ingredient... ingredients){
        SlaDatabase.databaseWriteExecutor.execute(() -> {
            ingredientDao.insertAll(ingredients);
        });
    }

    public LiveData<List<Ingredient>> getIngredientsByRecipeId(int id){
        return ingredientDao.getIngredientsByRecipeId(id);
    }

    public void deleteRecipe(Recipe recipe){
        SlaDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.delete(recipe);
        });
    }
}
