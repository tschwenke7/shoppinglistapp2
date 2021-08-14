package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.dao.IngListDao;
import com.example.shoppinglistapp2.db.dao.IngListItemDao;
import com.example.shoppinglistapp2.db.dao.MealDao;
import com.example.shoppinglistapp2.db.dao.MealPlanDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.dao.TagDao;
import com.example.shoppinglistapp2.db.tables.IngList;
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

    public ListenableFuture<Integer> insertRecipe(final Recipe recipe){
        return SlaDatabase.databaseWriteExecutor.submit(() -> (int) recipeDao.insert(recipe));
    }

    public void insertIngListItems(List<IngListItem> ingListItems) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.insertAll(ingListItems));
    }

    public void insertOrMergeItem(long listId, IngListItem newItem) {
        IngListItem existingItemWithSameName = findItemWithMatchingName(listId, newItem);

        //if there's still no match, just insert
        if(null == existingItemWithSameName){

            //calling "get()" forces the insert to have completed before this function completes.
            //This helps in cases where multiple ingredients are added in a row, preventing duplicates
            //merging being a race condition with the database's inserting happening slower than its
            //next "findIngListItemWithSameName" call.
            newItem.setListId(listId);
            insertIngListItem(newItem);
        }
        //if one was found, merge their quantities then persist the change
        else{
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            updateOrDeleteIfEmptyIngListItem(existingItemWithSameName);
        }
    }

    /**
     * Attempts to find an IngListItem in the database other than this one with a matching name
     * and checked flag value. Also checks for plurals, e.g. potato matches potatoes,
     * by attempting to match name without trailing "s" or "es", or by adding those suffixes,
     * if the original name is not found.
     * @param listId the id of the list to search within.
     * @param newItem the item to find a match for.
     * @return an existing item within the list with the same or the de/pluralised form, or null
     * if no match.
     */
    public IngListItem findItemWithMatchingName(long listId, IngListItem newItem){
        //attempt to find an existing item with the same name
        IngListItem existingItemWithSameName =
                findIngListItemWithSameName(listId, newItem.getId(), newItem.getName(), newItem.isChecked());

        //if none found, try some variations of plura/non plural for the name
        if(null == existingItemWithSameName){
            String name = newItem.getName();
            //try accounting for plural variation in name
            //remove "s" from the end if applicable
            if(name.endsWith("s")){
                existingItemWithSameName =
                       findIngListItemWithSameName(listId, newItem.getId(),
                                name.substring(0, newItem.getName().length() - 1),
                                newItem.isChecked());
                //if still no match, try removing an "es" suffix if applicable
                if(null == existingItemWithSameName){
                    if (name.endsWith("es")){
                        existingItemWithSameName =
                                findIngListItemWithSameName(listId, newItem.getId(),
                                        name.substring(0, newItem.getName().length() - 2),
                                        newItem.isChecked());
                    }
                }
                //if we found a matching item by de-pluralising the name, convert its name to the
                //plural form since we're about to merge newItem's qty into it and newItem is already
                //plural
                if (null != existingItemWithSameName){
                    existingItemWithSameName.setName(newItem.getName());
                }
            }
            //otherwise try turning the name into a plural
            else{
                existingItemWithSameName =
                        findIngListItemWithSameName(listId, newItem.getId(),
                                name + "s",
                                newItem.isChecked());

                //try adding an "es" to the end if that still hasn't worked
                if(null == existingItemWithSameName){
                    existingItemWithSameName =
                            findIngListItemWithSameName(listId, newItem.getId(),
                                    name + "es",
                                    newItem.isChecked());
                }
            }
        }
        return existingItemWithSameName;
    }

    public LiveData<List<IngListItem>> getIngredientsByRecipeId(int id){
        return ingListItemDao.getAllFromRecipe(id);
    }

    public ListenableFuture<List<IngListItem>> getIngredientsByRecipeIdNonLive(int id){
        return SlaDatabase.databaseWriteExecutor.submit(
                () -> ingListItemDao.getAllFromRecipeNonLive(id)
        );
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

    public ListenableFuture<Integer> deleteRecipes(List<Recipe> recipe){
        return SlaDatabase.databaseWriteExecutor.submit(() -> recipeDao.deleteAll(recipe));
    }

    public ListenableFuture<Integer> deleteRecipe(Recipe recipe){
        return SlaDatabase.databaseWriteExecutor.submit(() -> recipeDao.delete(recipe));
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

    public boolean recipeNameIsUnique(String name) throws ExecutionException, InterruptedException {
        return SlaDatabase.databaseWriteExecutor.submit(() -> recipeDao.getByName(name) == null).get();
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

    public long insertIngListItem(IngListItem ingListItem){
        return ingListItemDao.insert(ingListItem);
//        Callable<Long> queryCallable = () -> ingListItemDao.insert(ingListItem);
//        return SlaDatabase.databaseWriteExecutor.submit(queryCallable);
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

    public ListenableFuture<Integer> deleteTag(Tag tag){
        return SlaDatabase.databaseWriteExecutor.submit(() -> tagDao.delete(tag));
    }

    public void deleteTag(int recipeId, String tag) {
        SlaDatabase.databaseWriteExecutor.execute(() -> tagDao.delete(recipeId, tag));
    }

    public ListenableFuture<String[]> getAllTagNames(){
        return SlaDatabase.databaseWriteExecutor.submit(tagDao::getAllTagNames);
    }

    public ListenableFuture<List<Tag>> getTagsByRecipe(int recipeId){
        return SlaDatabase.databaseWriteExecutor.submit(() -> tagDao.getTagsByRecipe(recipeId));
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

    public ListenableFuture<Integer> updateMeal(Meal meal) {
        return SlaDatabase.databaseWriteExecutor.submit(()-> mealDao.update(meal));
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

    public ListenableFuture<Long> insertIngList(long recipeId) {
        IngList ingList = new IngList();
        ingList.setRecipeId(recipeId);

        return SlaDatabase.databaseWriteExecutor.submit(() -> ingListDao.insert(ingList));
    }

    public ListenableFuture<RecipeWithTagsAndIngredients> getPopulatedRecipeById(int id) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> recipeDao.getPopulatedByIdNonLive(id));
    }
}
