package com.example.shoppinglistapp2.activities.ui.recipes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListFragment;
import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.helpers.IngredientUtils;
import com.example.shoppinglistapp2.helpers.RecipeWebsiteUtils;
import com.example.shoppinglistapp2.helpers.SlItemUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RecipesViewModel extends AndroidViewModel {

    private final SlaRepository slaRepository;
    /** Contains the Recipes as found in the Recipe table of db */
    private final LiveData<List<Recipe>> allRecipesBase;
    /** Contains the Recipes with their ingredients and tags combined in */
    private final MutableLiveData<List<Recipe>> allRecipes = new MutableLiveData<>();

    private MealPlan selectingForMeal = null;

    private Observer<List<Recipe>> recipeObserver = recipes -> {
        //when db changes, retrieve ingredients and tags for recipes returned
        List<Recipe> populatedRecipes = new ArrayList<>();
        for (Recipe recipe : recipes){
            populatedRecipes.add(populateIngredientsAndTags(recipe));
        }

        //update the populated list livedata
        allRecipes.setValue(populatedRecipes);
    };

    public RecipesViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        allRecipesBase = slaRepository.getAllRecipes();
        //observe recipes db for changes so we can maintain populated list
        allRecipesBase.observeForever(recipeObserver);
    }


    public LiveData<List<Recipe>> getAllRecipes(){
        return allRecipes;
    }

    /** Creates an empty Recipe with placeholder name, and returns its db id
     * @return the id of the newly created empty recipe
     */
    public int generateNewRecipeId(){
        //first, find a unique name to use as placeholder
        int i = 1;
        String recipeName;
        do {
            recipeName = String.format("Untitled recipe %d", i);
            i++;
        } while(!slaRepository.recipeNameIsUnique(recipeName));

        //create the recipe
        Recipe newRecipe = new Recipe();
        newRecipe.setName(recipeName);

        //persist it to db
        return (int) slaRepository.insertRecipe(newRecipe);
    }

    /**
     * Creates and persists a recipe from the provided website and returns its rowId,
     * or -1 if no recipe could be generated (e.g. for an invalid or unsupported website url)
     * @param url - the website to get a recipe from
     * @return - the rowId of the newly generated recipe
     */
    public int generateRecipeIdFromUrl(String url){

        //scrape the website and fill as many Recipe fields as possible
        Recipe newRecipe = RecipeWebsiteUtils.getRecipeFromWebsite(url);

        //if this process failed (e.g. due to invalid url), the recipe will be null
        //and so we should simply return -1 here
        if(null == newRecipe){
            return -1;
        }

        //if no name was provided, generate a unique one
        if(null == newRecipe.getName()){
            int i = 1;
            String recipeName;
            do {
                recipeName = String.format("Untitled recipe %d", i);
                i++;
            } while(!slaRepository.recipeNameIsUnique(recipeName));

            newRecipe.setName(recipeName);
        }

        //if provided name exists already, append a number to make it unique
        int j = 2;
        while(!slaRepository.recipeNameIsUnique(newRecipe.getName())){
            newRecipe.setName(String.format("%s (%d)", newRecipe.getName(), j));
        }

        //persist the recipe to db
        int id = (int) slaRepository.insertRecipe(newRecipe);

        //persist all recipe's ingredients to db
        addIngredientsToRecipe(id, newRecipe.getIngredients());

        //persist any tags also to db
        for(String tagName : newRecipe.getTags()){
            insertTag(id, tagName);
        }

        return id;
    }

    public LiveData<List<Ingredient>> getRecipeIngredientsById(int id) {
        return slaRepository.getIngredientsByRecipeId(id);
    }

    public void deleteIngredients(Ingredient... ingredients){
        slaRepository.deleteIngredients(ingredients);
    }

    public void addIngredientsToRecipe(int recipeId, String... ingredients){

        //add each new item to the database
        for (String ingredientText : ingredients){
            Ingredient ingredient = IngredientUtils.toIngredient(ingredientText);
            ingredient.setRecipeId(recipeId);
            slaRepository.insertIngredient(ingredient);
            Log.d("TOM_TEST", "adding item: " + ingredientText);
        }
    }

    private void addIngredientsToRecipe(int recipeId, List<Ingredient> ingredients){
        //add each new item to the database
        for (Ingredient ingredient : ingredients){
            ingredient.setRecipeId(recipeId);
            slaRepository.insertIngredient(ingredient);
        }
    }

    public void deleteRecipes(Recipe... recipes){
        slaRepository.deleteRecipe(recipes);
    }

    public boolean recipeNameIsUnique(String name){
        return slaRepository.recipeNameIsUnique(name);
    }

    public void updateRecipe(Recipe recipe){
        slaRepository.updateRecipe(recipe);
    }

    public int getRecipeIdAtPosition(int position){
        return allRecipes.getValue().get(position).getId();
    }

    public Recipe getRecipeById(int id){
        return slaRepository.getRecipeById(id);
    }

    public void deleteTag(int recipeId, String tag){
        slaRepository.deleteTag(recipeId, tag);
    }

    public List<String> getTagsByRecipe(int recipeId){
        return slaRepository.getTagsByRecipe(recipeId);
    }

    public String[] getAllTags(){
        return slaRepository.getAllTags();
    }

    public void insertTag(int recipeId, String tagName){
        slaRepository.insertTag(recipeId, tagName);
    }

    public void loadFromBackup(RecipeListFragment frag){
        slaRepository.deleteAllRecipes();
        Log.d("TOM_TEST", "loadFromBackup started");
        Thread t1 = new Thread(() -> {

            BufferedInputStream bufferedInputStream = new BufferedInputStream(frag.getResources().openRawResource(R.raw.recipe_backup_2021_03_04p));
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(bufferedInputStream));
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] row = line.split("\\|");
                    //1- notes
                    //2- tomrating
                    //3- tierrating
                    //4- link
                    //5- tag
                    Log.d("TOM_TEST", line);
//                    Log.d("TOM_TEST", String.format("%s\n%s\n%s\n%s\n%s",row[1],row[2],row[3],row[4],row[5]));

                    try {
                        Recipe recipe = getRecipeById(generateRecipeIdFromUrl(row[4]));

                        if(null != row[1] && !row[1].isEmpty()) {
                            recipe.setNotes(row[1].trim());
                        }

                        recipe.setTom_rating(Integer.parseInt(row[2]) * 2);
                        recipe.setTier_rating(Integer.parseInt(row[3]) * 2);

                        if(row.length > 5 && null != row[5] && !row[5].isEmpty()) {
                            insertTag(recipe.getId(), row[5]);
                        }

                        updateRecipe(recipe);
                    }
                    catch (NullPointerException e){
                        e.printStackTrace();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t1.start();
    }

    private Recipe populateIngredientsAndTags(Recipe recipe) {
        //combine ingredients
        recipe.setIngredients(slaRepository.getIngredientsByRecipeIdNonLive(recipe.getId()));

        //combine tags
        recipe.setTags(getTagsByRecipe(recipe.getId()));

        //return populated recipe
        return recipe;
    }

    @Override
    protected void onCleared() {
        allRecipesBase.removeObserver(recipeObserver);
        super.onCleared();
    }

    public void setSelectingForMeal(MealPlan mealPlan) {
        selectingForMeal = mealPlan;
    }

    public void clearSelectingForMeal(){
        selectingForMeal = null;
    }

    public void saveToMealPlan(int position) {
        //get id of recipe
        int recipeId = getRecipeIdAtPosition(position);

        //retrieve mealplan object and update its recipe id
        selectingForMeal.setRecipeId(recipeId);

        //update meal plan in db
        slaRepository.updateMealPlan(selectingForMeal);

        //update meal plan ingredients needed accordingly
        List<Ingredient> newIngredients = slaRepository.getIngredientsByRecipeIdNonLive(recipeId);
        for(Ingredient ingredient : newIngredients){
            insertOrMergeItem(SlItemUtils.MEALPLAN_LIST_ID, SlItemUtils.toSlItem(ingredient));
        }
    }

    private void insertOrMergeItem(int listId, SlItem newItem){
        //attempt to find an existing item with the same name/checked status
        SlItem existingItem = slaRepository.getSlItemByName(listId, newItem.getName(), newItem.isChecked());

        //if none found, just insert
        if(null == existingItem){
            //if the new item has negative qty (i.e. we're removing), check if there's a crossed off
            //entry we can still merge with/subtract from instead
            if (newItem.getQty1().charAt(0) == '-') {
                if(!newItem.isChecked()) {
                    newItem.setChecked(true);
                    insertOrMergeItem(listId, newItem);
                }
            }
            //if no match and not negative, simply insert it to the db
            else {
                try {
                    //calling "get()" forces the insert to have completed before checking if the next item
                    //is already on the list
                    newItem.setListId(listId);
                    slaRepository.insertSlItem(newItem).get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //if a match was found, merge their quantities then persist the change
        else{
            //perform the merge
            SlItemUtils.mergeQuantities(existingItem, newItem);

            /* Check for and nullify 0 or negative qtys, splitting and re-merging if negatives */
            //if qty1 is 0, set it to null
            if(existingItem.getQty1().equals("0")){
                existingItem.setQty1(null);
                existingItem.setUnit1(null);
            }
            //if qty1 is negative, set it to null...
            else if (existingItem.getQty1().charAt(0) == '-'){
                //if it isn't crossed off, create a new item which IS with the negative qty and merge it
                //else if it's already crossed off, we can ignore the excess negative
                if(!existingItem.isChecked()){
                    SlItem negativeSplit = new SlItem();
                    negativeSplit.setQty1(existingItem.getQty1());
                    negativeSplit.setUnit1(existingItem.getUnit1());
                    negativeSplit.setName(existingItem.getName());
                    negativeSplit.setChecked(true);
                    insertOrMergeItem(listId, negativeSplit);
                }

                existingItem.setQty1(null);
                existingItem.setUnit1(null);
            }

            //if qty2 is 0, set it to null
            if(null != existingItem.getQty2()){
                if(existingItem.getQty2().equals("0")){
                    existingItem.setQty2(null);
                    existingItem.setUnit2(null);
                }

                //if qty2 is negative, set it to null
                else if (existingItem.getQty2().charAt(0) == '-'){
                    //if it isn't crossed off, create a new item which IS with the negative qty and merge it
                    //else if it's already crossed off, we can ignore the excess negative
                    if(!existingItem.isChecked()){
                        SlItem negativeSplit = new SlItem();
                        negativeSplit.setQty2(existingItem.getQty2());
                        negativeSplit.setUnit2(existingItem.getUnit2());
                        negativeSplit.setName(existingItem.getName());
                        negativeSplit.setChecked(true);
                        insertOrMergeItem(listId, negativeSplit);
                    }

                    existingItem.setQty2(null);
                    existingItem.setUnit2(null);
                }
            }

            //if qty1 is null but qty2 isn't, move qty 2 to qty 1
            if (existingItem.getQty1() == null && existingItem.getQty2() != null){
                existingItem.setUnit1(existingItem.getUnit2());
                existingItem.setQty1(existingItem.getQty2());
                existingItem.setUnit2(null);
                existingItem.setQty2(null);
            }

            /* Now that all possible nullifying has happened... */
            //if qty1 is still null, delete the db entry
            if (existingItem.getQty1() == null){
                slaRepository.deleteSlItems(existingItem);
            }

            //else update it
            else{
                slaRepository.updateSlItems(existingItem);
            }
        }
    }

    public void toggleChecked(SlItem item) {
        item.setChecked(!item.isChecked());
        slaRepository.deleteSlItems(item);
        insertOrMergeItem(item.getListId(), item);
    }

    public MealPlan getSelectingForMeal() {
        return selectingForMeal;
    }

    public void removeIngredientsFromList(List<Ingredient> ingredients) {
        //remove the recipe's ingredients from the "ingredients needed" list
        for(Ingredient ingredient : ingredients){
            //remove the right amount of the ingredient
            //by negating the quantity and then "adding" it to the list
            SlItem item = SlItemUtils.toSlItem(ingredient);
            item.setQty1("-" + item.getQty1());
            insertOrMergeItem(SlItemUtils.MEALPLAN_LIST_ID, item);
        }
    }
}