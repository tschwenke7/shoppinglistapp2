package com.example.shoppinglistapp2.activities.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public class SharedViewModel extends AndroidViewModel {
    private Integer navigateToRecipeId = null;
    private Meal selectingForMeal = null;
    private final SlaRepository slaRepository;

    private Integer recipeIdToAdd = null;

    public SharedViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
    }

    /*Stuff for navigating to a recipe from any point in the app */
    /** Retrieves the value of navigateToRecipeId, or returns null if not set */
    public Integer getNavigateToRecipeId() {
        return  navigateToRecipeId;
    }

    public void setNavigateToRecipeId(Integer navigateToRecipeId) {
        this.navigateToRecipeId = navigateToRecipeId;
    }

    /* Stuff for handling selecting a recipe from MealPlanFragment's "add recipe" button */
    public void setSelectingForMeal(Meal meal) {
        selectingForMeal = meal;
    }

    public void clearSelectingForMeal(){
        selectingForMeal = null;
    }

    public Meal getSelectingForMeal() {
        return selectingForMeal;
    }

    public int saveToMealPlan(int recipeId) {
        //retrieve meal object and update its recipe id
        selectingForMeal.setRecipeId(recipeId);

        //get the ingredient list for the meal plan this meal was part of
        int ingListId = slaRepository.getIngListIdForMealPlan(selectingForMeal.getPlanId());

        //update meal plan in db
        slaRepository.updateMeal(selectingForMeal);


        //add all items from the selected recipe to the mealPlan's ingList
        List<IngListItem> items = slaRepository.getIngredientsByRecipeIdNonLive(recipeId);
        for (IngListItem item : items) {
            slaRepository.insertOrMergeItem(ingListId, item);
        }

        return 0;
    }

    /* Stuff for adding a recipe to a meal plan from the view recipe tab, originating at ViewRecipeFragment
    * and activating actionmode in mealplan tab */
    public Integer getRecipeIdToAdd() {
        return recipeIdToAdd;
    }

    public void setRecipeIdToAdd(Integer recipeIdToAdd) {
        this.recipeIdToAdd = recipeIdToAdd;
    }

}
