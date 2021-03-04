package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.dao.SlItemDao;
import com.example.shoppinglistapp2.db.dao.TagDao;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.db.tables.Tag;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SlaRepository {
    private IngredientDao ingredientDao;
    private RecipeDao recipeDao;
    private SlItemDao slItemDao;
    private TagDao tagDao;

    private LiveData<List<Recipe>> allRecipes;
    private LiveData<List<SlItem>> allSlItems;

    public SlaRepository(Context context){
        SlaDatabase db = SlaDatabase.getDatabase(context);
        ingredientDao = db.ingredientDao();
        recipeDao = db.recipeDao();
        slItemDao = db.slItemDao();
        tagDao = db.tagDao();
        allRecipes = recipeDao.getAllAlphabetical();
        allSlItems = slItemDao.getAll();
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

    public LiveData<List<Ingredient>> getIngredientsByRecipeId(int id){
        return ingredientDao.getIngredientsByRecipeId(id);
    }
    public Future<List<Ingredient>> getIngredientsByRecipeIdNonLive(int id){
        Callable<List<Ingredient>> insertCallable = () -> ingredientDao.getIngredientsByRecipeIdNonLive(id);

       return SlaDatabase.databaseWriteExecutor.submit(insertCallable);
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
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
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

    public void deleteIngredients(Ingredient... ingredients) {
        SlaDatabase.databaseWriteExecutor.execute(() -> ingredientDao.deleteAll(ingredients));
    }

    public LiveData<List<SlItem>> getSlItems(){
        return allSlItems;
    }

    public SlItem getSlItemByName(String name){
        Callable<SlItem> queryCallable = () -> slItemDao.getByName(name);

        Future<SlItem> future = SlaDatabase.databaseWriteExecutor.submit(queryCallable);
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

    public void deleteSlItems(SlItem... slItems){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.deleteAll(slItems));
    }

    public void insertSlItems(SlItem... slItems){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.insertAll(slItems));
    }

    public void updateSlItems(SlItem... slItems){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.update(slItems));
    }

    public void deleteCheckedSlItems(){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.clearAllChecked());
    }

    public void deleteAllSlItems(){
        SlaDatabase.databaseWriteExecutor.execute(() -> slItemDao.clearAll());
    }

    /* "Tag" functions */
    public void insert(Tag tag){
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
}
