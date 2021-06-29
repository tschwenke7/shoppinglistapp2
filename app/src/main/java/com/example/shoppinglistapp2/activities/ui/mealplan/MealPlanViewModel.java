package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.helpers.SlItemUtils;

import java.util.ArrayList;
import java.util.List;

public class MealPlanViewModel extends AndroidViewModel {
    /** Mealplans with recipes populated (when possible) */
    private final MutableLiveData<List<MealPlan>> allMealPlans = new MutableLiveData<>();
    private final LiveData<List<SlItem>> allMealPlanSlItems;
    private final SlaRepository slaRepository;

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

        /** Observe db entries for meal plans, and populate objects with recipes/ingredients when modified */
        slaRepository.getAllMealPlans(SlItemUtils.MEALPLAN_LIST_ID).observeForever(mealPlanObserver);
        allMealPlanSlItems = slaRepository.getAllMealPlanSlItems();
    }

    public MutableLiveData<List<MealPlan>> getMealPlans(){
        return allMealPlans;
    }

    public LiveData<List<SlItem>> getAllMealPlanSlItems() {
        return allMealPlanSlItems;
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

    public void removeRecipe(MealPlan mealPlan) {
        //remove the recipe from this meal
        mealPlan.setRecipeId(null);
        slaRepository.updateMealPlan(mealPlan);
    }

    /** Clears all recipes and ingredients from meal plans, but doesn't delete the plans themselves. */
    public void removeAllRecipes(){
        List<MealPlan> plans = allMealPlans.getValue();
        //delete plans if any
        slaRepository.clearAllDays(1);

        //clear ingredient list
        slaRepository.deleteAllSlItems(SlItemUtils.MEALPLAN_LIST_ID);
    }

    public void resetMealPlan(){
        //delete all days of this meal plan
        slaRepository.deleteAllMealPlans(1);
        //clear ingredient list
        slaRepository.deleteAllSlItems(SlItemUtils.MEALPLAN_LIST_ID);
    }

    public List<SlItem> getAllUncheckedMealPlanSlItems() {
        return slaRepository.getAllUncheckedListItems(SlItemUtils.MEALPLAN_LIST_ID);
    }

    public void updateNotes(int position, String newNotes) {
        MealPlan mealPlan = allMealPlans.getValue().get(position);
        //delete notes entirely if they are empty
        if(newNotes.isEmpty()){
            mealPlan.setNotes(null);
        }
        else{
            mealPlan.setNotes(newNotes);
        }
        slaRepository.updateMealPlan(mealPlan);
    }
}