package com.example.shoppinglistapp2.activities.ui.recipes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.helpers.IngredientUtils;
import com.example.shoppinglistapp2.helpers.RecipeWebsiteUtils;

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
        return (int) slaRepository.insertRecipe(newRecipe);
    }

    /**
     * Creates and persists a recipe from the provided website and returns its rowId,
     * or -1 if no recipe could be generated (e.g. for an invalid or unsupported website url)
     * @param url - the website to get a recipe from
     * @return - the rowId of the newly generated recipe
     */
    public int generateRecipeIdFromUrl(String url){

        //scrape the website and fill as many Recipe fields as possible
        Recipe newRecipe = RecipeWebsiteUtils.getRecipeFromWebsite(url);

        //if this process failed (e.g. due to invalid url), the recipe will be null
        //and so we should simply return -1 here
        if(null == newRecipe){
            return -1;
        }

        //if no name was provided, generate a unique one
        if(null == newRecipe.getName()){
            int i = 1;
            String recipeName;
            do {
                recipeName = String.format("Untitled recipe %d", i);
                i++;
            } while(!slaRepository.recipeNameIsUnique(recipeName));

            newRecipe.setName(recipeName);
        }

        //if provided name exists already, append a number to make it unique
        int j = 2;
        while(!slaRepository.recipeNameIsUnique(newRecipe.getName())){
            newRecipe.setName(String.format("%s (%d)", newRecipe.getName(), j));
        }

        //persist the recipe to db
        int id = (int) slaRepository.insertRecipe(newRecipe);

        //persist all recipe's ingredients to db
        addIngredientsToRecipe(id, newRecipe.getIngredients());

        return id;
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
            Ingredient ingredient = IngredientUtils.toIngredient(ingredientText);
            ingredient.setRecipeId(recipeId);
            slaRepository.insertIngredient(ingredient);
            Log.d("TOM_TEST", "adding item: " + ingredientText);
        }
    }

    private void addIngredientsToRecipe(int recipeId, List<Ingredient> ingredients){
        //add each new item to the database
        for (Ingredient ingredient : ingredients){
            ingredient.setRecipeId(recipeId);
            slaRepository.insertIngredient(ingredient);
        }
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

    public void deleteTag(int recipeId, String tag){
        slaRepository.deleteTag(recipeId, tag);
    }
}