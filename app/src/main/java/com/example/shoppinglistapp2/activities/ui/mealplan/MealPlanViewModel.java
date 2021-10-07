package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.relations.IngListWithItems;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.db.tables.withextras.PopulatedRecipeWithScore;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.example.shoppinglistapp2.helpers.InvalidIngredientStringException;
import com.example.shoppinglistapp2.helpers.MealPlanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MealPlanViewModel extends AndroidViewModel {

    private static final String TAG = "Tom_debug_MPViewModel";
    private MutableLiveData<MealPlan> mealPlan = new MutableLiveData<>();
    private LiveData<MealPlan> mealPlanDb;


    private MutableLiveData<List<MealWithRecipe>> meals = new MutableLiveData<>();
    private LiveData<List<MealWithRecipe>> mealsDb;

    private MutableLiveData<List<IngListItem>> mealPlanIngredients = new MutableLiveData<>();
    private LiveData<IngListWithItems> mealPlanIngListDb;

    private final SlaRepository slaRepository;

    private int currentMPId;

    private LiveData<List<RecipeWithTagsAndIngredients>> allRecipes;

    public MealPlanViewModel(@NonNull Application application){
        super(application);
        slaRepository = new SlaRepository(application);
        allRecipes = slaRepository.getAllRecipesPopulated();
        allRecipes.observeForever(recipeWithTagsAndIngredients -> {
            //do nothing - this is just here to make sure the livedata is kept up to date
        });
    }

    public boolean setMealPlan(int mealPlanId) throws IndexOutOfBoundsException {
        try {
            //if -1 provided, just get the most recent meal plan id instead
            if(mealPlanId == -1){
                Integer latestId = slaRepository.getMostRecentMealPlanId().get();
                if(latestId == null){
                    //create a new meal plan if none exist
                    latestId = slaRepository.createNewMealPlan();
                }
                mealPlanId = latestId;
            }

            this.currentMPId = mealPlanId;

            //swap the mealsObserver to listen to meal for new mealPlan
            if(null != mealsDb) {
                mealsDb.removeObserver(mealsObserver);
            }
            mealsDb = slaRepository.getMealsByPlanId(mealPlanId);
            mealsDb.observeForever(mealsObserver);

            //swap livedata for ingredients
            if(null != mealPlanIngListDb) {
                mealPlanIngListDb.removeObserver(ingListObserver);
            }
            mealPlanIngListDb = slaRepository.getIngListForMealPlan(mealPlanId);
            mealPlanIngListDb.observeForever(ingListObserver);

            //swap livedata for mealPlan
            if (null != mealPlanDb) {
                mealPlanDb.removeObserver(mealPlanObserver);
            }
            mealPlanDb = slaRepository.getMealPlanById(mealPlanId);
            mealPlanDb.observeForever(mealPlanObserver);

            return true;
        }
        catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("No meal plan exists with id " + mealPlanId + ".\n Original exception: " + e.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "setMealPlan: ", e);
            return false;
        }
    }

    private final Observer<MealPlan> mealPlanObserver = mp -> mealPlan.postValue(mp);

    public LiveData<MealPlan> getMealPlan() {
        return mealPlan;
    }

    public int getCurrentMPId() {
        return currentMPId;
    }

    /**
     * Listen for updates to current mealplan's db livedata and post values to UIs LiveData
     */
    private final Observer<List<MealWithRecipe>> mealsObserver = mealWithRecipes ->
            meals.postValue(mealWithRecipes);

    /**
     * LiveData which watches the list of meals for the current meal plan.
     * Calling setMealPlan(int) will change which mealPlan this is watching.
     * @return - the list of meals for the current mealPlanId, as set by
     * mealPlanViewModel.setMealPlan
     */
    public LiveData<List<MealWithRecipe>> getMeals() {
        return meals;
    }

    private final Observer<IngListWithItems> ingListObserver = ingList ->
            mealPlanIngredients.postValue(ingList.getItems());

    public LiveData<List<IngListItem>> getMealPlanIngredients() {
        return mealPlanIngredients;
    }


    public void addMeal(){
        List<MealWithRecipe> existingMeals = meals.getValue();

        Meal meal = new Meal();
        meal.setDayId(existingMeals.size());

        //generate a name for the new meal (picks next day of week if previous meal
        //contains a day of the week in the title)
        String previousTitle = null;
        if(existingMeals.size() != 0){
            previousTitle = existingMeals.get(existingMeals.size() - 1).getMeal().getDayTitle();
        }

        meal.setDayTitle(MealPlanUtils.suggestNextMealTitle(previousTitle));

        meal.setPlanId(currentMPId);
        slaRepository.insertMeal(meal);
    }

    public void updateDayTitle(int pos, String newTitle){
        Meal meal = meals.getValue().get(pos).getMeal();
        meal.setDayTitle(newTitle);
        slaRepository.updateMeal(meal);
    }

    /** Clears all recipes, notes and ingredients from meals, but doesn't delete the meals themselves. */
    public void clearAllMeals(){
        for(MealWithRecipe m : meals.getValue()){
            Meal meal = m.getMeal().deepCopy();
            meal.setRecipeId(null);
            meal.setNotes(null);
            slaRepository.updateMeal(meal);
        }

        //clear ingredient list
        slaRepository.deleteIngListItems(mealPlanIngredients.getValue());
    }

    public void resetMealPlan(){
        //delete all days of this meal plan
        slaRepository.deleteAllMeals(currentMPId);

        //clear ingredient list
        slaRepository.deleteIngListItems(mealPlanIngredients.getValue());
    }


    public void updateNotes(int position, String newNotes) {
        Meal meal = meals.getValue().get(position).getMeal();

        //delete notes entirely if they are empty
        if(newNotes.isEmpty()){
            meal.setNotes(null);
        }
        else{
            meal.setNotes(newNotes);
        }
        slaRepository.updateMeal(meal);
    }

    public void deleteMeal(int position) {
        Meal meal = meals.getValue().get(position).getMeal();
        Integer recipeId = meal.getRecipeId();

        slaRepository.deleteMeal(meal);

        if (null != recipeId) {
            removeRecipesIngredients(recipeId);
        }
    }

    public void removeRecipeFromMealAtPos(int position) {
        Meal meal = getMeals().getValue().get(position).getMeal();
        int recipeId = meal.getRecipeId();

        //remove the recipe from the meal plan slot
        meal.setRecipeId(null);
        slaRepository.updateMeal(meal);

        //remove its ingredients from the ingredients needed list
        removeRecipesIngredients(recipeId);
    }

    private void removeRecipesIngredients(int recipeId) {
        int ingListId = mealPlanIngListDb.getValue().getIngList().getId();

        //remove ingredients used by this recipe by adding a negative qty of each ingredient
        //opposite to the amount used by the recipe
        for(IngListItem item : slaRepository.getIngredientsByRecipeIdNonLive(recipeId)){
            item.negateQuantities();
            slaRepository.insertOrMergeItem(ingListId, item);
        }
    }

    public boolean exportToShoppingList() {
        for(IngListItem item : mealPlanIngredients.getValue()) {
            if (!item.isChecked()){
                //create a copy of this item with no id so we don't run into PK constraint violations
                IngListItem copy = item.deepCopy();
                copy.setId(0);
                slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, copy);
            }
        }

        return true;
    }

    public void toggleChecked(int position) {
        //if we directly edit the item from the list which the adapter is using, it won't be different
        //when setList is called.
        IngListItem item = mealPlanIngredients.getValue().get(position).deepCopy();

        item.setChecked(!item.isChecked());
        slaRepository.deleteIngListItem(item);
        slaRepository.insertOrMergeItem(item.getListId(), item);
    }

    public void editIngListItem(IngListItem oldItem, String newItemString) throws InvalidIngredientStringException {
        slaRepository.editItem(oldItem, newItemString);
    }

    public List<PopulatedRecipeWithScore> getSearchSuggestions(int maxNumSuggestions) {
        List<PopulatedRecipeWithScore> scoredSuggestions = new ArrayList<>();
        List<IngListItem> ingsToMatch = mealPlanIngredients.getValue();

        //loop through each recipe
        if(meals.getValue() != null && allRecipes.getValue() != null && ingsToMatch != null){
            //compile list of recipe ids already in the meal plan so we don't search ones which are already
            //in use.
            List<Integer> existingRecipeIds = new ArrayList<>();
            for (MealWithRecipe mealWithRecipe : meals.getValue()){
                if(mealWithRecipe.getRecipe() != null) {
                    existingRecipeIds.add(mealWithRecipe.getRecipe().getId());
                }
            }

            for (RecipeWithTagsAndIngredients recipe : allRecipes.getValue()){
                //ignore recipes which are already in the meal plan
                if(!existingRecipeIds.contains(recipe.getRecipe().getId())) {
                    //copy into object with additional "score" property for ranking suggestions
                    PopulatedRecipeWithScore scoredRecipe = new PopulatedRecipeWithScore(recipe);

                    //loop through each ingredient
                    for (IngListItem ingredient : scoredRecipe.getIngredients()) {
                        //set to "checked" (not matched) by default
                        ingredient.setChecked(true);
                        for (IngListItem ingToMatch : ingsToMatch) {
                            if (ingredient.getName().equals(ingToMatch.getName())) {
                                //mark this ingredient as matched
                                ingredient.setChecked(false);
                                //increase this recipe's score
                                scoredRecipe.incrementScore();
                                //no need to continue checking for matches if we found one
                                break;
                            }
                        }
                    }
                    //ignore recipes which had no matches/zero score
                    if(scoredRecipe.getScore() > 0) {
                        scoredSuggestions.add(scoredRecipe);
                    }
                }
                else{
                    Log.d(TAG, "getSearchSuggestions: ");
                }
            }

            //sort the scored results in descending order
            Collections.sort(scoredSuggestions);
            Collections.sort(scoredSuggestions);

            //take the top results
            List<PopulatedRecipeWithScore> finalSuggestions = new ArrayList<>();
            int i = 0;
            while (i < maxNumSuggestions && i < scoredSuggestions.size()) {
                finalSuggestions.add(scoredSuggestions.get(i));
                i++;
            }

            return finalSuggestions;
        }

        return null;
    }

    public void updateMeals(List<Meal> mealsToPersist) {
        slaRepository.updateMeals(mealsToPersist);
    }
}