package com.example.shoppinglistapp2.activities.ui.recipes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Recipe;

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

    public void addNewRecipe(Recipe recipe){
        slaRepository.insert(recipe);
    }
}