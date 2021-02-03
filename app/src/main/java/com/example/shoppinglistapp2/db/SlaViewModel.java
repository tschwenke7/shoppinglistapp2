package com.example.shoppinglistapp2.db;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;

public class SlaViewModel extends AndroidViewModel {

    private SlaRepository slaRepository;
    private final LiveData<List<Recipe>> allRecipes;

    public SlaViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        allRecipes = slaRepository.getAllRecipes();
    }

    public LiveData<List<Recipe>> getAllRecipes(){
        return allRecipes;
    }

    public void addNewRecipe(Recipe recipe){
        slaRepository.insertRecipe(recipe);
    }
}
