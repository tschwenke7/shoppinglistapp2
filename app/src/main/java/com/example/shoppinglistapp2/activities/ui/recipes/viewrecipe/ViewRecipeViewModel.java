package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.example.shoppinglistapp2.helpers.InvalidIngredientStringException;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class ViewRecipeViewModel extends AndroidViewModel {
    private final SlaRepository slaRepository;
    private RecipeWithTagsAndIngredients backup;

    public ViewRecipeViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
    }

    public LiveData<Recipe> getRecipeById(int id){
        return slaRepository.getRecipeByIdLive(id);
    }

    public void saveBackupOfRecipe(int recipeId){
        try {
            backup = slaRepository.getPopulatedRecipeById(recipeId).get();
        }
        catch (Exception e){}
    }

    public LiveData<List<IngListItem>> getRecipeIngredientsById(int recipeId) {
        return slaRepository.getIngredientsByRecipeId(recipeId);
    }

    public void deleteTag(Tag tag) {
        slaRepository.deleteTag(tag);
    }

    public String[] getAllTagNames() throws ExecutionException, InterruptedException {
        return slaRepository.getAllTagNames().get();
    }

    public List<Tag> getTagsByRecipe(int recipeId) throws ExecutionException, InterruptedException {
        return slaRepository.getTagsByRecipe(recipeId).get();
    }

    public void insertTag(Tag tag){
        slaRepository.insertTag(tag);
    }

    public boolean addIngredientsToRecipe(int recipeId, String... ingredients) throws ExecutionException, InterruptedException {

        //add each new item to the database
        for (String ingredientText : ingredients){
            IngListItem ingredient = IngListItemUtils.toIngListItem(ingredientText);

            //get ingList to add to
            int listId = 0;//todo
            slaRepository.insertOrMergeItem(listId, ingredient);
        }
        
        return true;
    }

    public boolean recipeNameIsUnique(String name) throws ExecutionException, InterruptedException {
        return slaRepository.recipeNameIsUnique(name);
    }

    public void updateRecipe(Recipe recipe){
        slaRepository.updateRecipe(recipe);
    }

    public boolean addIngredientsToShoppingList(List<IngListItem> ingListItems) throws ExecutionException, InterruptedException {
        for (IngListItem item : ingListItems){
            slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, item);
        }
        return true;
    }

    public void deleteIngredient(IngListItem item) {
        slaRepository.deleteIngListItem(item);
    }

    public void editItem(IngListItem oldItem, String newItemString) {
        //convert user's string to a new item
        IngListItem newItem = IngListItemUtils.toIngListItem(newItemString);

        //if name of ingredient has been changed to one which already exists,
        //we need to merge it with an existing item.
        //therefore, we delete the old item and then merge the modified one in.
        IngListItem existingItemWithSameName = slaRepository.findItemWithMatchingName(oldItem.getListId(), newItem);
        if (null != existingItemWithSameName){
            slaRepository.deleteIngListItem(oldItem);
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateOrDeleteIfEmptyIngListItem(existingItemWithSameName);
        }

        //if it wasn't changed to an existing item, simply update the original item in the db
        else{
            //copy identifying values over from oldItem to new item, then update it in the db
            //to overwrite all other values of oldItem
            newItem.setId(oldItem.getId());
            newItem.setListId(oldItem.getListId());
            newItem.setChecked(oldItem.isChecked());
            slaRepository.updateOrDeleteIfEmptyIngListItem(newItem);
        }
    }

    public void deleteRecipe(Recipe recipe) {
        slaRepository.deleteRecipe(recipe);
    }

    public boolean resetIngredientsToBackup() throws ExecutionException, InterruptedException {
        //compare the current ingredients with the backup list to see if they are the same, or
        //if we need to restore the backup

        List<IngListItem> currentIngredients =
                slaRepository.getIngredientsByRecipeIdNonLive(backup.getRecipe().getId()).get();
        List<IngListItem> ingredientsBackup = backup.getIngredients();

        if (currentIngredients.size() != ingredientsBackup.size()){
            //restore old values
            restoreOldValues(currentIngredients, ingredientsBackup);
            return true;
        }
        //otherwise compare all elements of lists to see if there has been a change
        else {
            boolean different = false;
            int i = 0;
            while (!different && i < currentIngredients.size()){
                if (!currentIngredients.get(i).equals(ingredientsBackup.get(i))){
                    different = true;
                }
                i++;
            }

            if (different) {
                //restore old values
                restoreOldValues(currentIngredients, ingredientsBackup);
                return true;
            }

            //otherwise they are the same, and we don't need to restore the db or do anything
            return false;
        }
    }
    
    private void restoreOldValues(List<IngListItem> ingredients, List<IngListItem> ingredientsBackup) {
        try {
            //get() forces deletion to complete and block before inserting ingredients in
            //which may have duplicate ids (primary keys)
            slaRepository.deleteIngListItems(ingredients).get();
            slaRepository.insertIngListItems(ingredientsBackup);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return null if no need to update, otherwise the list of tags to re-insert
     * @throws ExecutionException
     * @throws InterruptedException background threads get interrupted for some reason
     */
    public List<Tag> restoreTagsToBackup() throws ExecutionException, InterruptedException {
        List<Tag> currentTags = slaRepository.getTagsByRecipe(backup.getRecipe().getId()).get();
        List<Tag> backupTags = backup.getTags();

        if (currentTags.size() == backupTags.size()){
            boolean different = false;
            int i = 0;
            while (!different && i < currentTags.size()){
                if (!currentTags.get(i).equals(backupTags.get(i))){
                    different = true;
                }
                i++;
            }

            if (different) {
                //restore database values to backup
                restoreTags(backup.getRecipe().getId(), backupTags);
                return backupTags;
            }
            else{
                return null;
            }
        }
        //if lists are different lengths, then there have definitely been changes
        else {
            //restore database values to backup
            restoreTags(backup.getRecipe().getId(), backupTags);
            return backupTags;
        }
    }

    private void restoreTags(int recipeId, List<Tag> backupTags) {
        slaRepository.deleteAllTagsForRecipe(recipeId);
        slaRepository.insertTags(backupTags);
    }
}
