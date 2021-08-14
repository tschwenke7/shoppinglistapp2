//package com.example.shoppinglistapp2.activities.ui.recipes;
//
//import android.app.Application;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.lifecycle.AndroidViewModel;
//import androidx.lifecycle.LiveData;
//import androidx.lifecycle.MutableLiveData;
//import androidx.lifecycle.Observer;
//
//import com.example.shoppinglistapp2.App;
//import com.example.shoppinglistapp2.R;
//import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListFragment;
//import com.example.shoppinglistapp2.db.SlaRepository;
//import com.example.shoppinglistapp2.db.tables.Meal;
//import com.example.shoppinglistapp2.db.tables.Recipe;
//import com.example.shoppinglistapp2.db.tables.Tag;
//import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutionException;
//
//public class RecipesViewModel extends AndroidViewModel {
//
//    private final SlaRepository slaRepository;
//
//    /** Contains the Recipes with their ingredients and tags combined in */
//    private final LiveData<List<RecipeWithTagsAndIngredients>> allRecipes;
//
//    private Meal selectingForMeal = null;
//
//    public RecipesViewModel(@NonNull Application application) {
//        super(application);
//        slaRepository = new SlaRepository(application);
//        allRecipes = slaRepository.getAllRecipesPopulated();
//    }
//
//
//    public LiveData<List<RecipeWithTagsAndIngredients>> getAllRecipes(){
//        return allRecipes;
//    }
//
//
//
//    public LiveData<List<Ingredient>> getRecipeIngredientsById(int id) {
//        return slaRepository.getIngredientsByRecipeId(id);
//    }
//
//    public void deleteIngredients(Ingredient... ingredients){
//        slaRepository.deleteIngredients(ingredients);
//    }
//
//
//
//
//
//
//
//    public void updateRecipe(Recipe recipe){
//        slaRepository.updateRecipe(recipe);
//    }
//
//    public int getRecipeIdAtPosition(int position){
//        return allRecipes.getValue().get(position).getId();
//    }
//
//    public Recipe getRecipeById(int id){
//        return slaRepository.getRecipeById(id);
//    }
//
//    public void deleteTag(int recipeId, String tag){
//        slaRepository.deleteTag(recipeId, tag);
//    }
//
//    public List<String> getTagsByRecipe(int recipeId){
//        return slaRepository.getTagsByRecipe(recipeId);
//    }
//
//    public String[] getAllTags(){
//        return slaRepository.getAllTagNames();
//    }
//
//
//
//    public void loadFromBackup(RecipeListFragment frag){
//        slaRepository.deleteAllRecipes();
//        Log.d("TOM_TEST", "loadFromBackup started");
//        Thread t1 = new Thread(() -> {
//
//            BufferedInputStream bufferedInputStream = new BufferedInputStream(frag.getResources().openRawResource(R.raw.recipe_backup_2021_03_04p));
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(bufferedInputStream));
//            try {
//                String line;
//                while ((line = bufferedReader.readLine()) != null) {
//                    String[] row = line.split("\\|");
//                    //1- notes
//                    //2- tomrating
//                    //3- tierrating
//                    //4- link
//                    //5- tag
//                    Log.d("TOM_TEST", line);
////                    Log.d("TOM_TEST", String.format("%s\n%s\n%s\n%s\n%s",row[1],row[2],row[3],row[4],row[5]));
//
//                    try {
//                        Recipe recipe = getRecipeById(generateRecipeIdFromUrl(row[4]));
//
//                        if(null != row[1] && !row[1].isEmpty()) {
//                            recipe.setNotes(row[1].trim());
//                        }
//
//                        recipe.setTom_rating(Integer.parseInt(row[2]) * 2);
//                        recipe.setTier_rating(Integer.parseInt(row[3]) * 2);
//
//                        if(row.length > 5 && null != row[5] && !row[5].isEmpty()) {
//                            insertTag(recipe.getId(), row[5]);
//                        }
//
//                        updateRecipe(recipe);
//                    }
//                    catch (NullPointerException e){
//                        e.printStackTrace();
//                    }
//
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        t1.start();
//    }
//
//    public void setSelectingForMeal(Meal meal) {
//        selectingForMeal = meal;
//    }
//
//    public void clearSelectingForMeal(){
//        selectingForMeal = null;
//    }
//
//
//
//    private void insertOrMergeItem(int listId, SlItem newItem){
//        //attempt to find an existing item with the same name/checked status
//        SlItem existingItem = slaRepository.findSlItemWithSameName(listId, newItem);
//
//        //if none found, just insert
//        if(null == existingItem){
//            //if the new item has negative qty (i.e. we're removing), check if there's a crossed off
//            //entry we can still merge with/subtract from instead
//            if (newItem.getQty1().charAt(0) == '-') {
//                if(!newItem.isChecked()) {
//                    newItem.setChecked(true);
//                    insertOrMergeItem(listId, newItem);
//                }
//            }
//            //if no match and not negative, simply insert it to the db
//            else {
//                try {
//                    //calling "get()" forces the insert to have completed before checking if the next item
//                    //is already on the list
//                    newItem.setListId(listId);
//                    slaRepository.insertSlItem(newItem).get();
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        //if a match was found, merge their quantities then persist the change
//        else{
//            //perform the merge
//            SlItemUtils.mergeQuantities(existingItem, newItem);
//
//            /* Check for and nullify 0 or negative qtys, splitting and re-merging if negatives */
//            //if qty1 is 0, set it to null
//            if(existingItem.getQty1().equals("0")){
//                existingItem.setQty1(null);
//                existingItem.setUnit1(null);
//            }
//            //if qty1 is negative, set it to null...
//            else if (existingItem.getQty1().charAt(0) == '-'){
//                //if it isn't crossed off, create a new item which IS with the negative qty and merge it
//                //else if it's already crossed off, we can ignore the excess negative
//                if(!existingItem.isChecked()){
//                    SlItem negativeSplit = new SlItem();
//                    negativeSplit.setQty1(existingItem.getQty1());
//                    negativeSplit.setUnit1(existingItem.getUnit1());
//                    negativeSplit.setName(existingItem.getName());
//                    negativeSplit.setChecked(true);
//                    insertOrMergeItem(listId, negativeSplit);
//                }
//
//                existingItem.setQty1(null);
//                existingItem.setUnit1(null);
//            }
//
//            //if qty2 is 0, set it to null
//            if(null != existingItem.getQty2()){
//                if(existingItem.getQty2().equals("0")){
//                    existingItem.setQty2(null);
//                    existingItem.setUnit2(null);
//                }
//
//                //if qty2 is negative, set it to null
//                else if (existingItem.getQty2().charAt(0) == '-'){
//                    //if it isn't crossed off, create a new item which IS with the negative qty and merge it
//                    //else if it's already crossed off, we can ignore the excess negative
//                    if(!existingItem.isChecked()){
//                        SlItem negativeSplit = new SlItem();
//                        negativeSplit.setQty2(existingItem.getQty2());
//                        negativeSplit.setUnit2(existingItem.getUnit2());
//                        negativeSplit.setName(existingItem.getName());
//                        negativeSplit.setChecked(true);
//                        insertOrMergeItem(listId, negativeSplit);
//                    }
//
//                    existingItem.setQty2(null);
//                    existingItem.setUnit2(null);
//                }
//            }
//
//            //if qty1 is null but qty2 isn't, move qty 2 to qty 1
//            if (existingItem.getQty1() == null && existingItem.getQty2() != null){
//                existingItem.setUnit1(existingItem.getUnit2());
//                existingItem.setQty1(existingItem.getQty2());
//                existingItem.setUnit2(null);
//                existingItem.setQty2(null);
//            }
//
//            /* Now that all possible nullifying has happened... */
//            //if qty1 is still null, delete the db entry
//            if (existingItem.getQty1() == null){
//                slaRepository.deleteSlItems(existingItem);
//            }
//
//            //else update it
//            else{
//                slaRepository.updateSlItems(existingItem);
//            }
//        }
//    }
//
//    public void toggleChecked(SlItem item) {
//        item.setChecked(!item.isChecked());
//        slaRepository.deleteSlItems(item);
//        insertOrMergeItem(item.getListId(), item);
//    }
//
//    public Meal getSelectingForMeal() {
//        return selectingForMeal;
//    }
//
//    public void removeIngredientsFromList(List<Ingredient> ingredients) {
//        //remove the recipe's ingredients from the "ingredients needed" list
//        for(Ingredient ingredient : ingredients){
//            //remove the right amount of the ingredient
//            //by negating the quantity and then "adding" it to the list
//            SlItem item = SlItemUtils.toSlItem(ingredient);
//            item.setQty1("-" + item.getQty1());
//            insertOrMergeItem(SlItemUtils.MEALPLAN_LIST_ID, item);
//        }
//    }
//
//    public void editIngredient(Ingredient oldIngredient, String newIngredientText) {
//        //convert string to ingredient
//        Ingredient newIngredient = IngredientUtils.toIngredient(newIngredientText);
//
//        //overwrite old ingredient values with those from the newly converted one
//        oldIngredient.setName(newIngredient.getName());
//        oldIngredient.setQty(newIngredient.getQty());
//        oldIngredient.setUnit(newIngredient.getUnit());
//
//        //update the ingredient in the db
//        slaRepository.updateIngredient(oldIngredient);
//    }
//
//    public void resetIngredientsToBackup(int recipeId, List<Ingredient> ingredientsBackup) {
//
//        //compare the current ingredients with the backup list to see if they are the same, or
//        //if we need to restore the backup
//        LiveData<List<Ingredient>> currentIngredients = getRecipeIngredientsById(recipeId);
//        currentIngredients.observeForever(new Observer<List<Ingredient>>() {
//            @Override
//            public void onChanged(List<Ingredient> ingredients) {
//                currentIngredients.removeObserver(this);
//
//                ((App) getApplication()).backgroundExecutorService.execute(() -> {
//                    //if the lists are different lengths, then a change must've occurred.
//                    if (ingredients.size() != ingredientsBackup.size()){
//                        //restore old values
//                        restoreOldValues(ingredients, ingredientsBackup);
//                    }
//                    //otherwise compare all elements of lists to see if there has been a change
//                    else {
//                        boolean different = false;
//                        int i = 0;
//                        while (!different && i < ingredients.size()){
//                            if (!ingredients.get(i).equals(ingredientsBackup.get(i))){
//                                different = true;
//                            }
//                            i++;
//                        }
//
//                        if (different) {
//                            //restore old values
//                            restoreOldValues(ingredients, ingredientsBackup);
//                        }
//                    }
//                    //otherwise they are the same, and we don't need to restore the db or do anything
//                });
//            }
//
//            private void restoreOldValues(List<Ingredient> ingredients, List<Ingredient> ingredientsBackup) {
//                try {
//                    //get() forces deletion to complete and block before inserting ingredients in
//                    //which may have duplicate ids (primary keys)
//                    slaRepository.deleteIngredients(ingredients).get();
//                    slaRepository.insertIngredients(ingredientsBackup);
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//
//    public LiveData<Boolean> restoreTagsToBackup(int recipeId, List<String> tagsBackup) {
//        MutableLiveData<Boolean> needsRestoring = new MutableLiveData<>();
//
//        //check whether tags have actually changed since backup
//        ((App) getApplication()).backgroundExecutorService.execute(() -> {
//            List<String> currentTags = slaRepository.getTagsByRecipe(recipeId);
//
//            //if lists are the same length, we need to compare all items
//            if (currentTags.size() == tagsBackup.size()){
//                boolean different = false;
//                int i = 0;
//                while (!different && i < currentTags.size()){
//                    if (!currentTags.get(i).equals(tagsBackup.get(i))){
//                        different = true;
//                    }
//                    i++;
//                }
//
//                if (different) {
//                    //restore database values to backup
//                    restoreTags(recipeId, tagsBackup, needsRestoring);
//
//                }
//                else{
//                    needsRestoring.postValue(false);
//                }
//            }
//            //if lists are different lengths, then there have definitely been changes
//            else {
//                restoreTags(recipeId, tagsBackup, needsRestoring);
//            }
//        });
//        return needsRestoring;
//    }
//
//    private void restoreTags(int recipeId, List<String> tagsBackup, MutableLiveData<Boolean> needsRestoring) {
//        try{
//            //restore database values to backup
//            slaRepository.deleteAllTagsForRecipe(recipeId).get();
//            List<Tag> tagsToInsert = new ArrayList<>();
//            for (String name : tagsBackup){
//                tagsToInsert.add(new Tag(recipeId, name));
//            }
//            slaRepository.insertTags(tagsToInsert).get();
//            needsRestoring.postValue(true);
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//            needsRestoring.postValue(false);
//        }
//    }
//}