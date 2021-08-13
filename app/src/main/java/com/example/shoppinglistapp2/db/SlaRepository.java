package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.dao.IngListDao;
import com.example.shoppinglistapp2.db.dao.IngListItemDao;
import com.example.shoppinglistapp2.db.dao.MealDao;
import com.example.shoppinglistapp2.db.dao.MealPlanDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.dao.TagDao;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
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
    private final IngListDao ingListDao;

    private final LiveData<List<RecipeWithTagsAndIngredients>> allRecipesPopulated;
    private final LiveData<List<IngListItem>> shoppingListItems;
//    private final LiveData<List<IngListItem>> allMealPlanSlItems;

    public SlaRepository(Context context){
        SlaDatabase db = SlaDatabase.getDatabase(context);
        ingListItemDao = db.ingListItemDao();
        recipeDao = db.recipeDao();
        mealDao = db.mealDao();
        tagDao = db.tagDao();
        mealPlanDao = db.mealPlanDao();
        ingListDao = db.ingListDao();
        allRecipesPopulated = recipeDao.getAllPopulatedAlphabetical();
//        allMealPlanSlItems = slItemDao.getAll(SlItemUtils.MEALPLAN_LIST_ID);
        shoppingListItems = ingListItemDao.getAllFromShoppingList(IngListItemUtils.SHOPPING_LIST_ID);

    }

    public LiveData<List<RecipeWithTagsAndIngredients>> getAllRecipesPopulated(){
        return allRecipesPopulated;
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

    public void insertIngListItems(List<IngListItem> ingListItems) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.insertAll(ingListItems));
    }

    public LiveData<List<IngListItem>> getIngredientsByRecipeId(int id){
        return ingListItemDao.getAllFromRecipe(id);
    }

    public List<IngListItem> getIngListItemsByRecipeIdNonLive(int id){
        Callable<List<IngListItem>> insertCallable = () -> ingListItemDao.getIngredientsByListIdNonLive(id);
        Future<List<IngListItem>> future = SlaDatabase.databaseWriteExecutor.submit(insertCallable);

        try{
            return future.get();
        }
        catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public void deleteRecipe(List<Recipe> recipes){
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
        SlaDatabase.databaseWriteExecutor.execute(() -> recipeDao.update(recipe));
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

    public Future<Integer> deleteIngListItem(IngListItem item){
        return SlaDatabase.databaseWriteExecutor.submit(() -> ingListItemDao.delete(item));
    }

    public Future<Integer> deleteIngListItems(List<IngListItem> ingListItems) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> ingListItemDao.deleteAll(ingListItems));
    }

    public LiveData<List<IngListItem>> getSlItems(){
        return shoppingListItems;
    }

    /**
     * Returns, if found, an SlItem from the given list which has the same name and checked status,
     * but not the same id (i.e. a potential merge candidate).
     * @param listId list to search within.
     * @param idToExclude the id of the item you're searching with to be excluded from search, so we don't
     *               match an item with itself.
     * @param nameToMatch the name to match
     * @param checkedToMatch the checked status to match
     * @return an item other than itemToMatch with the same name and checked status,
     * or null if none exist.
     */
    public IngListItem findIngListItemWithSameName(long listId, int idToExclude, String nameToMatch, boolean checkedToMatch){
        Callable<IngListItem> queryCallable = () -> ingListItemDao.getAnotherByName(listId, nameToMatch, checkedToMatch, idToExclude);

        Future<IngListItem> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public ListenableFuture<Long> insertIngListItem(IngListItem slItem){
        Callable<Long> queryCallable = () -> ingListItemDao.insert(slItem);
        return SlaDatabase.databaseWriteExecutor.submit(queryCallable);
    }

    public void updateOrDeleteIfEmptyIngListItem(IngListItem item){
        //if all quantities have been reduced to zero, delete the item instead
        if(item.isEmpty()){
            SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.delete(item));
        }
        else{
            SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.update(item));
        }
    }

    public void deleteCheckedIngListItems(int listId){
        SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.clearAllChecked(listId));
    }

    public void deleteAllIngListItems(int listId){
        SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.clearAll(listId));
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

    public void insertMealPlan(MealPlan mealPlan){
        SlaDatabase.databaseWriteExecutor.execute(()-> mealPlanDao.insert(mealPlan));
    }

    public void updateMealPlan(MealPlan mealPlan) {
        SlaDatabase.databaseWriteExecutor.execute(()-> mealPlanDao.update(mealPlan));
    }

    public Meal getMealById(int id){
        Callable<Meal> queryCallable = () -> mealDao.getById(id);

        Future<Meal> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public LiveData<List<IngListItem>> getAllMealPlanIngListItems(int mealPlanId) {
        return ingListItemDao.getAllFromMealPlan(mealPlanId);
    }

    public void deleteAllMeals(int planId){
        SlaDatabase.databaseWriteExecutor.execute(() -> mealDao.deleteAllWithPlanId(planId));
    }

    public void clearAllDays(int planId) {
        SlaDatabase.databaseWriteExecutor.execute(() -> mealDao.clearAllDays(planId));
    }

    public ListenableFuture<List<IngListItem>> getAllUncheckedListItems(int listId) {
        return SlaDatabase.databaseWriteExecutor.submit(
                () -> ingListItemDao.getAllUncheckedNonLive(listId));
    }

    public void deleteMeal(Meal meal) {
        SlaDatabase.databaseWriteExecutor.execute(() -> mealDao.delete(meal));
    }

    public Future<List<Long>> insertTags(List<Tag> tags) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> tagDao.insertAll(tags));
    }

    public Future<Integer> deleteAllTagsForRecipe(int recipeId) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> tagDao.deleteAllByRecipeId(recipeId));
    }

    public void insertOrIgnoreShoppingList() {
        SlaDatabase.databaseWriteExecutor.submit(
                () -> ingListDao.insertShoppingList(IngListItemUtils.SHOPPING_LIST_ID));
    }
}
