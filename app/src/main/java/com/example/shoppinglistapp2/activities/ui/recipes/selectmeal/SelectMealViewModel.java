package com.example.shoppinglistapp2.activities.ui.recipes.selectmeal;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;

import java.util.List;

public class SelectMealViewModel extends AndroidViewModel {
    private static final String TAG = "T_DBG_SELECT_MEAL_VM";
    private final SlaRepository slaRepository;
    private LiveData<List<MealWithRecipe>> meals;
    private int recipeId;
    private int mealPlanId;

    public SelectMealViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
    }

    public LiveData<List<MealWithRecipe>> getMeals() throws IndexOutOfBoundsException {
        meals = slaRepository.getMealsByPlanId(mealPlanId);
        return meals;
    }

    public String getRecipeNameById(int recipeId) {
        Recipe recipe = slaRepository.getRecipeById(recipeId);
        return recipe.getName();
    }


    public int getRecipeIdAtPos(int pos) {
        MealWithRecipe meal = meals.getValue().get(pos);
        if(meal.getRecipe() != null) {
            return meal.getRecipe().getId();
        }
        //if there was no recipe, return -1
        else {
            return  -1;
        }
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public int getMealPlanId() {
        return mealPlanId;
    }

    public void setMealPlanId(int mealPlanId) {
        this.mealPlanId = mealPlanId;
    }

    public void addRecipeToMealAtPos(int pos) {
        Meal meal = meals.getValue().get(pos).getMeal();
        meal.setRecipeId(recipeId);
        slaRepository.updateMeal(meal);
    }

    public String getMealNameAtPos(int pos) {
        return meals.getValue().get(pos).getMeal().getDayTitle();
    }

    public String getRecipeNameAtPos(int pos) {
        return meals.getValue().get(pos).getRecipe().getName();
    }
}