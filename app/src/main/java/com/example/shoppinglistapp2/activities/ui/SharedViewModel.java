package com.example.shoppinglistapp2.activities.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.google.common.util.concurrent.ListenableFuture;

public class SharedViewModel extends AndroidViewModel {
    private Integer navigateToRecipeId = null;
    private Meal selectingForMeal = null;
    private final SlaRepository slaRepository;

    public SharedViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
    }

    /** Retrieves the value of navigateToRecipeId, or returns null if not set */
    public Integer getNavigateToRecipeId() {
        return  navigateToRecipeId;
    }

    public void setNavigateToRecipeId(Integer navigateToRecipeId) {
        this.navigateToRecipeId = navigateToRecipeId;
    }

    public void setSelectingForMeal(Meal meal) {
        selectingForMeal = meal;
    }

    public void clearSelectingForMeal(){
        selectingForMeal = null;
    }

    public Meal getSelectingForMeal() {
        return selectingForMeal;
    }

    public ListenableFuture<Integer> saveToMealPlan(int recipeId) {
        //retrieve meal object and update its recipe id
        selectingForMeal.setRecipeId(recipeId);

        //update meal plan in db
        return slaRepository.updateMeal(selectingForMeal);

        //update meal plan ingredients needed accordingly
//        todo slaRepository.addAllItemsFromRecipeToList(recipeId, selectingForMeal.getPlanId());
//        List<Ingredient> newIngredients = slaRepository.getIngredientsByRecipeIdNonLive(recipeId);
//        for(Ingredient ingredient : newIngredients){
//            insertOrMergeItem(SlItemUtils.MEALPLAN_LIST_ID, SlItemUtils.toSlItem(ingredient));
//        }

    }
}
