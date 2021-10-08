package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
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
import com.example.shoppinglistapp2.db.tables.relations.IngListWithItems;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.example.shoppinglistapp2.helpers.InvalidIngredientStringException;
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
        return Transformations.distinctUntilChanged(allRecipesPopulated);
    }

    public ListenableFuture<Integer> insertRecipe(final Recipe recipe){
        return SlaDatabase.databaseWriteExecutor.submit(() -> (int) recipeDao.insert(recipe));
    }

    public void insertIngListItems(List<IngListItem> ingListItems) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingListItemDao.insertAll(ingListItems));
    }


    public LiveData<List<IngListItem>> getIngredientsByRecipeId(int id){
        return Transformations.distinctUntilChanged(ingListItemDao.getAllFromRecipe(id));
    }

    public List<IngListItem> getIngredientsByRecipeIdNonLive(int id){
        return ingListItemDao.getAllFromRecipeNonLive(id);
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

    public int deleteIngListItems(List<IngListItem> ingListItems) {
        return ingListItemDao.deleteAll(ingListItems);
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

    public List<Tag> getTagsByRecipe(int recipeId){
        return tagDao.getTagsByRecipe(recipeId);
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

    public RecipeWithTagsAndIngredients getPopulatedRecipeById(int id) {
        return recipeDao.getPopulatedByIdNonLive(id);
    }

    public ListenableFuture<Integer> deleteRecipeWithId(int recipeId) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> recipeDao.deleteById(recipeId));
    }

    public void editItem(IngListItem oldItem, String newItemString) throws InvalidIngredientStringException {
        //convert user's string to a new item
        IngListItem newItem = IngListItemUtils.toIngListItem(newItemString);

        //copy identifying values over from oldItem to new item
        newItem.setId(oldItem.getId());
        newItem.setListId(oldItem.getListId());
        newItem.setChecked(oldItem.isChecked());
        newItem.setListOrder(oldItem.getListOrder());

        //if name of ingredient has been changed to one which already exists,
        //we need to merge it with an existing item.
        //therefore, we delete the old item and then merge the modified one in.
        IngListItem existingItemWithSameName = findItemWithMatchingName(oldItem.getListId(), newItem);
        if (null != existingItemWithSameName){
            deleteIngListItem(oldItem);
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            updateOrDeleteIfEmptyIngListItem(existingItemWithSameName);
        }

        //if it wasn't changed to an existing item, simply update the original item in the db
        else{
            //since newItem has the same "id" as oldItem, it will overwrite the database entry
            updateOrDeleteIfEmptyIngListItem(newItem);
        }
    }

    public void insertOrMergeItem(long listId, IngListItem newItem) {
        IngListItem existingItemWithSameName = findItemWithMatchingName(listId, newItem);

        //if there's still no match, just insert
        if(null == existingItemWithSameName){
            //if we are adding a negative quantity item (e.g. to remove a quantity of it from the list)
            if (newItem.hasNegativeQtys()){
                //remove any negative quantities
                IngListItem negativeQtyOverflow = newItem.extractNegativeQtys();
                //if no match was found with un-checked items, match negative qtys
                // against checked (crossed off) items
                if(!newItem.isChecked()){
                    negativeQtyOverflow.setChecked(true);
                    insertOrMergeItem(listId, negativeQtyOverflow);
                }
            }

            //create a copy of this ingListItem to add to the new list if it already has an id
            //i.e. it already exists in the database in another list so it needs a unique id
            if(newItem.getId() != 0){
                IngListItem copy = newItem.deepCopy();
                //clear the id, so it will be added as a new entry in the ingListItems table
                //id will be autogenerated when inserted
                copy.setId(0);
                //set the list id
                copy.setListId(listId);
                newItem = copy;
            }

            //insert the new item, assuming it has some positive quantity associated
            if(!newItem.isEmpty()) {
                newItem.setListId(listId);
                insertIngListItem(newItem);
            }
        }
        //if one was found, merge their quantities then persist the change
        else{
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);

            //if the merge resulted in negative quantities, check to see if there's a match with a
            //crossed off item we can deduct any negative quantities from.
            if (!newItem.isChecked() && existingItemWithSameName.hasNegativeQtys()) {
                IngListItem negativeQtyOverflow = existingItemWithSameName.extractNegativeQtys();
                negativeQtyOverflow.setChecked(true);

                insertOrMergeItem(listId, negativeQtyOverflow);
            }

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

    public ListenableFuture<List<String>> getAllRecipeNames() {
        return recipeDao.getAllNames();
    }

    public ListenableFuture<List<String>> getDistinctIngredientNames() {
        return ingListItemDao.getDistinctIngredientNames(IngListItemUtils.SHOPPING_LIST_ID);
    }

    public ListenableFuture<List<String>> getDistinctTagNames() {
        return tagDao.getAllTagNames();
    }

    public LiveData<List<MealWithRecipe>> getMealsByPlanId(int mealPlanId) {
        return (mealDao.getPopulatedById(mealPlanId));
    }

    public ListenableFuture<Integer> getMostRecentMealPlanId() {
        return mealPlanDao.getLatestId();
    }

    public LiveData<IngListWithItems> getIngListForMealPlan(int mealPlanId) {
        return Transformations.distinctUntilChanged(ingListDao.getIngListForMealPlan(mealPlanId));
    }

    public LiveData<MealPlan> getMealPlanById(int mealPlanId) {
        return Transformations.distinctUntilChanged(mealPlanDao.getById(mealPlanId));
    }

    public ListenableFuture<Long> insertMeal(Meal meal) {
        return SlaDatabase.databaseWriteExecutor.submit(() -> mealDao.insert(meal));
    }

    public int createNewMealPlan() throws ExecutionException, InterruptedException {
        //create mealPlan db entry
        MealPlan mealPlan = new MealPlan();
        mealPlan.setName(App.getRes().getString(R.string.default_meal_plan_title));
        Future<Long> future = SlaDatabase.databaseWriteExecutor.submit(() -> mealPlanDao.insert(mealPlan));
        Long mpId = future.get();

        //create corresponding inglist
        IngList ingList = new IngList();
        ingList.setMealPlanId(mpId);
        Future<Long> future2 = SlaDatabase.databaseWriteExecutor.submit(() -> ingListDao.insert(ingList));
        future2.get();
        return mpId.intValue();
    }

    public int getIngListIdForMealPlan(int mealPlanId) {
        return ingListDao.getIngListIdForMealPlan(mealPlanId);
    }

    public void updateMeals(List<Meal> meals) {
        SlaDatabase.databaseWriteExecutor.submit(() -> mealDao.updateAll(meals));
    }
}
