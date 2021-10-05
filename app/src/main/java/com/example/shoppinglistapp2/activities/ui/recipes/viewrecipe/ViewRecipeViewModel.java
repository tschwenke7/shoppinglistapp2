package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
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

    public LiveData<Recipe> getRecipe(int recipeId){
        return slaRepository.getRecipeByIdLive(recipeId);
    }

    public void saveBackupOfRecipe(int recipeId){
        backup = slaRepository.getPopulatedRecipeById(recipeId);
    }

    public LiveData<List<IngListItem>> getRecipeIngredientsById(int recipeId) {
        return slaRepository.getIngredientsByRecipeId(recipeId);
    }

    public void deleteTag(Tag tag) {
        slaRepository.deleteTag(tag);
    }

    public ListenableFuture<List<String>> getDistinctTagNames() {
        return slaRepository.getDistinctTagNames();
    }

    public List<Tag> getRecipeTags(int recipeId) {
        return slaRepository.getTagsByRecipe(recipeId);
    }

    public void insertTag(Tag tag){
        slaRepository.insertTag(tag);
    }

    public boolean addIngredientsToRecipe(String... ingredients) {
        //get ingList to add to
        int listId = backup.getIngListWithItems().getIngList().getId();

        //add each new item to the database
        for (String ingredientText : ingredients){
            IngListItem ingredient = IngListItemUtils.toIngListItem(ingredientText);
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

    public boolean addIngredientsToShoppingList(List<IngListItem> ingListItems) {
        for (IngListItem item : ingListItems){
            //make a copy of the item with no "id" so it will be inserted into the db as a new row
            IngListItem copy = item.deepCopy();
            copy.setId(0);
            slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, copy);
        }
        return true;
    }

    public void deleteIngredient(IngListItem item) {
        slaRepository.deleteIngListItem(item);
    }

    public void editItem(IngListItem oldItem, String newItemString) {
        slaRepository.editItem(oldItem, newItemString);
    }

    public void deleteRecipe(int recipeId) {
        slaRepository.deleteRecipeWithId(recipeId);
    }

    public boolean resetIngredientsToBackup() {
        //compare the current ingredients with the backup list to see if they are the same, or
        //if we need to restore the backup

        List<IngListItem> currentIngredients =
                slaRepository.getIngredientsByRecipeIdNonLive(backup.getRecipe().getId());
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
        //delete all ingredients, then add back in all from backup
        slaRepository.deleteIngListItems(ingredients);
        slaRepository.insertIngListItems(ingredientsBackup);
    }

    /**
     *
     * @return null if no need to update, otherwise the list of tags to re-insert
     * @throws ExecutionException
     * @throws InterruptedException background threads get interrupted for some reason
     */
    public List<Tag> restoreTagsToBackup() {
        List<Tag> currentTags = slaRepository.getTagsByRecipe(backup.getRecipe().getId());
        List<Tag> backupTags = new ArrayList<>(backup.getTags());

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

    public void changeAllQtys(int oldVal, int newVal) {
        List<IngListItem> ingredients =
                slaRepository.getIngredientsByRecipeIdNonLive(backup.getRecipe().getId());
        for (IngListItem ing : ingredients) {
            ing.setMassQty(ing.getMassQty() * newVal / oldVal);
            ing.setVolumeQty(ing.getVolumeQty() * newVal / oldVal);
            ing.setWholeItemQty(ing.getWholeItemQty() * newVal / oldVal);
            ing.setOtherQty(ing.getOtherQty() * newVal / oldVal);

            slaRepository.updateOrDeleteIfEmptyIngListItem(ing);
        }
    }
}
