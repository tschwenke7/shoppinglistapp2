package com.example.shoppinglistapp2.activities.ui.recipes.recipelist.selectmeal;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
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

        //get the recipeId we're replacing if one was already present in this meal
        Integer oldRecipeId = meals.getValue().get(pos).getMeal().getRecipeId();

        //update the meal in db
        meal.setRecipeId(recipeId);
        slaRepository.updateMeal(meal);

        //get the ingredient list for the meal plan this meal was part of
        int ingListId = slaRepository.getIngListIdForMealPlan(mealPlanId);

        //add all items from the selected recipe to the mealPlan's ingList
        List<IngListItem> items = slaRepository.getIngredientsByRecipeIdNonLive(recipeId);
        for (IngListItem item : items) {
            slaRepository.insertOrMergeItem(ingListId, item);
        }

        //if we are replacing an existing recipe, delete its ingredients from the meal plan list
        if(oldRecipeId != null) {
            List<IngListItem> itemsToRemove = slaRepository.getIngredientsByRecipeIdNonLive(oldRecipeId);
            for (IngListItem item : itemsToRemove) {
                item.negateQuantities();
                slaRepository.insertOrMergeItem(ingListId, item);
            }
        }
    }

    public String getMealNameAtPos(int pos) {
        return meals.getValue().get(pos).getMeal().getDayTitle();
    }

    public String getRecipeNameAtPos(int pos) {
        return meals.getValue().get(pos).getRecipe().getName();
    }
}