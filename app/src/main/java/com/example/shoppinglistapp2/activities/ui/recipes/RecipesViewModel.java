package com.example.shoppinglistapp2.activities.ui.recipes;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.helpers.IngredientUtil;

import java.util.ArrayList;
import java.util.List;

public class RecipesViewModel extends AndroidViewModel {

    private final SlaRepository slaRepository;
    private final LiveData<List<Recipe>> allRecipes;

    private MutableLiveData<List<Ingredient>> newRecipeIngredients;

    public RecipesViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        allRecipes = slaRepository.getAllRecipes();
        newRecipeIngredients = new MutableLiveData<List<Ingredient>>();
        newRecipeIngredients.setValue(new ArrayList<>());
    }

    public LiveData<List<Recipe>> getAllRecipes(){
        return allRecipes;
    }

    public MutableLiveData<List<Ingredient>> getNewRecipeIngredients() {
        return newRecipeIngredients;
    }

    public void addIngredientToNewRecipe(String... ingredients){
        //copy the current list
        List<Ingredient> temp = newRecipeIngredients.getValue();

        //add each new item to the copy
        for (String ingredientText : ingredients){
            //todo - split into qty and name
            Ingredient ingredient = IngredientUtil.toIngredient(ingredientText);
            temp.add(ingredient);
            Log.d("TOM_TEST", "adding item: " + ingredientText);
        }

        //overwrite the original list with the expanded copy to
        newRecipeIngredients.setValue(temp);
    }

    public void clearNewRecipe(){
        newRecipeIngredients.setValue(new ArrayList<>());
    }

    public boolean addNewRecipe(Recipe recipe){
        //insert recipe
        long rowId = slaRepository.insertRecipe(recipe);

        if(rowId == -1){
            Log.d("TOM_TEST","Error adding recipe. Please try again later.");
            return false;
        }

        for(Ingredient ing : newRecipeIngredients.getValue()){
            ing.setRecipeId((int) rowId);
            slaRepository.insertIngredient(ing);
        }

        return true;
    }
}