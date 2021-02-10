package com.example.shoppinglistapp2.activities.ui.recipes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.helpers.IngredientUtil;

import java.util.ArrayList;
import java.util.List;

public class RecipesViewModel extends AndroidViewModel {

    private final SlaRepository slaRepository;
    private final LiveData<List<Recipe>> allRecipes;

    public RecipesViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        allRecipes = slaRepository.getAllRecipes();
    }

    public LiveData<List<Recipe>> getAllRecipes(){
        return allRecipes;
    }

    /** Creates an empty Recipe with placeholder name, and returns its db id
     * @return the id of the newly created empty recipe
     */
    public int generateNewRecipeId(){
        //first, find a unique name to use as placeholder
        int i = 1;
        String recipeName;
        do {
            recipeName = String.format("Untitled recipe %d", i);
            i++;
        } while(!slaRepository.recipeNameIsUnique(recipeName));

        //create the recipe
        Recipe newRecipe = new Recipe();
        newRecipe.setName(recipeName);

        //persist it to db
        long id = slaRepository.insertRecipe(newRecipe);

        return (int) id;
    }

    public LiveData<List<Ingredient>> getRecipeIngredientsById(int id) {
        return slaRepository.getIngredientsByRecipeId(id);
    }

    public void deleteIngredients(Ingredient... ingredients){
        slaRepository.deleteIngredients(ingredients);
    }

    public void addIngredientsToRecipe(int recipeId, String... ingredients){

        //add each new item to the database
        for (String ingredientText : ingredients){
            Ingredient ingredient = IngredientUtil.toIngredient(ingredientText);
            ingredient.setRecipeId(recipeId);
            slaRepository.insertIngredient(ingredient);
            Log.d("TOM_TEST", "adding item: " + ingredientText);
        }
    }


    public boolean addNewRecipe(Recipe recipe){
        //insert recipe
        long rowId = slaRepository.insertRecipe(recipe);

        if(rowId == -1){
            Log.d("TOM_TEST","Error adding recipe. Please try again later.");
            return false;
        }

        return true;
    }

    public void deleteRecipes(Recipe... recipes){
        slaRepository.deleteRecipe(recipes);
    }

    public boolean recipeNameIsUnique(String name){
        return slaRepository.recipeNameIsUnique(name);
    }

    public void updateRecipe(Recipe recipe){
        slaRepository.updateRecipe(recipe);
    }

    public int getRecipeIdAtPosition(int position){
        return allRecipes.getValue().get(position).getId();
    }

    public Recipe getRecipeById(int id){
        return slaRepository.getRecipeById(id);
    }
}