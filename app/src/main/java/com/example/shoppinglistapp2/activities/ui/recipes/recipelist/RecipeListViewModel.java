package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {
    private final SlaRepository slaRepository;

    /** Contains the Recipes with their ingredients and tags combined in */
    private final LiveData<List<RecipeWithTagsAndIngredients>> allRecipes;

    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        allRecipes = slaRepository.getAllRecipesPopulated();
    }

    public LiveData<List<RecipeWithTagsAndIngredients>> getAllRecipes(){
        return allRecipes;
    }

    public ListenableFuture<Integer> deleteRecipes(List<RecipeWithTagsAndIngredients> populatedRecipes){
        List<Recipe> toDelete = new ArrayList<>();
        for(RecipeWithTagsAndIngredients populatedRecipe : populatedRecipes){
            toDelete.add(populatedRecipe.getRecipe());
        }
        return slaRepository.deleteRecipes(toDelete);
    }
}
