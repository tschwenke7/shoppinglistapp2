package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.ArrayList;
import java.util.List;

public class MealPlanViewModel extends AndroidViewModel {
    /** Mealplans as retrieved from database, without recipes included */
    private LiveData<List<MealPlan>> allMealPlansBase;
    /** Mealplans with recipes populated (when possible) */
    private MutableLiveData<List<MealPlan>> allMealPlans = new MutableLiveData<>();
    private SlaRepository slaRepository;

    private Observer<List<MealPlan>> mealPlanObserver = mealPlans -> {
        //when db changes, retrieve ingredients and tags for recipes returned
        List<MealPlan> populatedMealPlans = new ArrayList<>();
        for (MealPlan mealPlan : mealPlans){
            populatedMealPlans.add(populateRecipeAndIngredients(mealPlan));
        }

        //update the populated list livedata
        allMealPlans.setValue(populatedMealPlans);
    };

    public MealPlanViewModel(@NonNull Application application){
        super(application);
        slaRepository = new SlaRepository(application);

        allMealPlansBase = slaRepository.getAllMealPlans(1);
        allMealPlansBase.observeForever(mealPlanObserver);
    }

    public MutableLiveData<List<MealPlan>> getMealPlans(){
        return allMealPlans;
    }

    /** If a recipe id is associated with this meal plan, populates the MealPlan object with a recipe,
     * which is in turn populated with its ingredients.
     * @param mealPlan - a MealPlan
     * @return - the same MealPlan, with a populated "recipe" property if it had a valid recipeId
     */
    private MealPlan populateRecipeAndIngredients(MealPlan mealPlan) {
        Integer recipeId = mealPlan.getRecipeId();
        if (null != recipeId){
            Recipe recipe = slaRepository.getRecipeById(recipeId);
            recipe.setIngredients(slaRepository.getIngredientsByRecipeIdNonLive(recipeId));
            mealPlan.setRecipe(recipe);
        }

        return mealPlan;
    }

    public void addDay(){
        MealPlan mealPlan = new MealPlan();
        mealPlan.setDayId(allMealPlans.getValue().size());
        mealPlan.setDayTitle("Title");
        mealPlan.setPlanId(1);
        slaRepository.insertMealPlan(mealPlan);
    }

    public void updateDayTitle(int pos, String newTitle){
        MealPlan mp = allMealPlans.getValue().get(pos);
        mp.setDayTitle(newTitle);
        slaRepository.updateMealPlan(mp);
    }
}