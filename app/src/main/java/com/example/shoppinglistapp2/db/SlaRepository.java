package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.dao.IngListItemDao;
import com.example.shoppinglistapp2.db.dao.MealDao;
import com.example.shoppinglistapp2.db.dao.MealPlanDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.dao.TagDao;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.helpers.SlItemUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SlaRepository {
    private final IngListItemDao ingListItemDao;
    private final RecipeDao recipeDao;
    private final TagDao tagDao;
    private final MealPlanDao mealPlanDao;
    private final MealDao mealDao;

    private final LiveData<List<Recipe>> allRecipes;
    private final LiveData<List<IngListItem>> shoppingListItems;
//    private final LiveData<List<IngListItem>> allMealPlanSlItems;

    public SlaRepository(Context context){
        SlaDatabase db = SlaDatabase.getDatabase(context);
        ingListItemDao = db.ingListItemDao();
        recipeDao = db.recipeDao();
        mealDao = db.mealDao();
        tagDao = db.tagDao();
        mealPlanDao = db.mealPlanDao();
        allRecipes = recipeDao.getAllAlphabetical();
//        allMealPlanSlItems = slItemDao.getAll(SlItemUtils.MEALPLAN_LIST_ID);
        shoppingListItems = ingListItemDao.getAllFromMealPlan(SlItemUtils.SHOPPING_LIST_ID);
    }

    public LiveData<List<Recipe>> getAllRecipes(){
        return allRecipes;
    }

    public long insertRecipe(final Recipe recipe){
        Callable<Long> insertCallable = () -> recipeDao.insert(recipe);
        long rowId = 0;

        Future<Long> future = SlaDatabase.databaseWriteExecutor.submit(insertCallable);
        try {
            rowId = future.get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return -1;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        }
        return rowId;
    }

    public void insertIngredient(final Ingredient... ingredients){
        SlaDatabase.databaseWriteExecutor.execute(() -> {
            ingredientDao.insertAll(ingredients);
        });
    }

    public void insertIngredients(List<Ingredient> ingredients) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingredientDao.insertAll(ingredients));
    }

    public LiveData<List<Ingredient>> getIngredientsByRecipeId(int id){
        return ingredientDao.getIngredientsByRecipeId(id);
    }

    public List<Ingredient> getIngredientsByRecipeIdNonLive(int id){
        Callable<List<Ingredient>> insertCallable = () -> ingredientDao.getIngredientsByRecipeIdNonLive(id);

        Future<List<Ingredient>> future = SlaDatabase.databaseWriteExecutor.submit(insertCallable);

        try{
            return future.get();
        }
        catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public void deleteRecipe(Recipe... recipes){
        SlaDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.deleteAll(recipes);
        });
    }

    public Recipe getRecipeByName(String name){
        Callable<Recipe> queryCallable = () -> recipeDao.getByName(name);

        Future<Recipe> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public boolean recipeNameIsUnique(String name){
        Callable<Recipe> queryCallable = () -> recipeDao.getByName(name);

        Future<Recipe> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
        try {
            return null == future.get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateRecipe(Recipe recipe) {
        SlaDatabase.databaseWriteExecutor.execute(() -> recipeDao.updateRecipe(recipe));
    }

    public Recipe getRecipeById(int id) {
        Callable<Recipe> queryCallable = () -> recipeDao.getById(id);

        Future<Recipe> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
        try {
            return future.get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public LiveData<Recipe> getRecipeByIdLive(int id){
        return recipeDao.getByIdLive(id);
    }

    public void deleteIngredients(Ingredient... ingredients) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingredientDao.deleteAll(ingredients));
    }

    public Future<Integer> deleteIngredients(List<Ingredient> ingredients) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> ingredientDao.deleteAll(ingredients));
    }

    public LiveData<List<IngListItem>> getSlItems(){
        return shoppingListItems;
    }

    /**
     * Returns, if found, an SlItem from the given list which has the same name and checked status,
     * but not the same id (i.e. a potential merge candidate).
     * @param listId list to search within.
     * @param itemToMatch an SlItem to find a match for.
     * @return an item other than itemToMatch with the same name and checked status,
     * or null if none exist.
     */
    public IngListItem findSlItemWithSameName(int listId, IngListItem itemToMatch){
        Callable<IngListItem> queryCallable = () -> ingListItemDao.getAnotherByName(listId, itemToMatch.getName(), itemToMatch.isChecked(), itemToMatch.getId());

        Future<IngListItem> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public void deleteSlItems(SlItem... slItems){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.deleteAll(slItems));
    }

    public ListenableFuture<Long> insertIngListItem(IngListItem slItem){
        Callable<Long> queryCallable = () -> ingListItemDao.insert(slItem);
        return SlaDatabase.databaseWriteExecutor.submit(queryCallable);
    }

    public void updateSlItems(SlItem... slItems){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.update(slItems));
    }

    public void deleteCheckedSlItems(int listId){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.clearAllChecked(listId));
    }

    public void deleteAllSlItems(int listId){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.clearAll(listId));
    }

    /* "Tag" functions */
    public void insertTag(Tag tag){
        SlaDatabase.databaseWriteExecutor.execute(() -> tagDao.insert(tag));
    }

    public void deleteTag(Tag tag){
        SlaDatabase.databaseWriteExecutor.execute(() -> tagDao.delete(tag));
    }

    public void deleteTag(int recipeId, String tag) {
        SlaDatabase.databaseWriteExecutor.execute(() -> tagDao.delete(recipeId, tag));
    }

    public String[] getAllTags(){
        Future<String[]> future = SlaDatabase.databaseWriteExecutor.submit(
                () -> tagDao.getAllTags());
        try {
            return future.get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getTagsByRecipe(int recipeId){
        Future<List<String>> future = SlaDatabase.databaseWriteExecutor.submit(
                () -> tagDao.getTagsByRecipe(recipeId));
        try {
            return future.get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void insertTag(int recipeId, String tagName) {
        SlaDatabase.databaseWriteExecutor.execute(() -> tagDao.insert(new Tag(recipeId, tagName)));
    }

    public void deleteAllRecipes() {
        SlaDatabase.databaseWriteExecutor.execute(()-> recipeDao.deleteEverything());
    }

    /* Meal plans */
    public LiveData<List<MealPlan>> getAllMealPlans(int planId){
        return mealPlanDao.getAll(planId);
    }

    public void insertMealPlan(MealPlan mealPlan){
        SlaDatabase.databaseWriteExecutor.execute(()-> mealPlanDao.insert(mealPlan));
    }

    public void updateMealPlan(MealPlan mealPlan) {
        SlaDatabase.databaseWriteExecutor.execute(()-> mealPlanDao.update(mealPlan));
    }

    public MealPlan getMealPlanById(int id){
        Callable<MealPlan> queryCallable = () -> mealPlanDao.getById(id);

        Future<MealPlan> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public LiveData<List<SlItem>> getAllMealPlanSlItems() {
        return allMealPlanSlItems;
    }

    public void deleteAllMealPlans(int planId){
        SlaDatabase.databaseWriteExecutor.execute(() -> mealPlanDao.deleteAll(planId));
    }

    public void clearAllDays(int planId) {
        SlaDatabase.databaseWriteExecutor.execute(() -> mealPlanDao.clearAllDays(planId));
    }

    public List<SlItem> getAllUncheckedListItems(int listId) {
        Callable<List<SlItem>> queryCallable = () -> slItemDao.getAllUncheckedNonLive(listId);

        Future<List<SlItem>> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public void deleteMealPlan(MealPlan mealPlan) {
        SlaDatabase.databaseWriteExecutor.execute(() -> mealPlanDao.delete(mealPlan));
    }

    public void updateIngredient(Ingredient ingredient) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingredientDao.update(ingredient));
    }

    public Future<List<Long>> insertTags(List<Tag> tags) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> tagDao.insertAll(tags));
    }

    public Future<Integer> deleteAllTagsForRecipe(int recipeId) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> tagDao.deleteAllByRecipeId(recipeId));
    }
}
