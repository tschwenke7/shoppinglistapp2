package com.example.shoppinglistapp2.activities.ui.recipes.creator;

import android.app.Application;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListFragment;
import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.InvalidIngredientStringException;
import com.example.shoppinglistapp2.helpers.RecipeWebsiteUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CreateRecipeViewModel extends AndroidViewModel {
    private final SlaRepository slaRepository;

    public CreateRecipeViewModel(Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
    }
    /**
     * Creates and persists a recipe from the provided website and returns its rowId,
     * or -1 if no recipe could be generated (e.g. for an invalid or unsupported website url)
     * @param url - the website to get a recipe from
     * @return - the rowId of the newly generated recipe
     */
    public int generateRecipeIdFromUrl(String url) throws InvalidRecipeUrlExeception, ExecutionException, InterruptedException {
        //scrape the website and fill as many Recipe fields as possible
        RecipeWithTagsAndIngredients newRecipe = RecipeWebsiteUtils.getRecipeFromWebsite(url);

        //if this process failed (e.g. due to invalid url), the recipe will be null
        //and so we should simply return -1 here
        if(null == newRecipe){
            return -1;
        }

        //if no name was provided, generate a unique one
        if(null == newRecipe.getRecipe().getName()){
            int i = 1;
            String recipeName;
            do {
                recipeName = String.format("Untitled recipe %d", i);
                i++;
            } while(!slaRepository.recipeNameIsUnique(recipeName));

            newRecipe.getRecipe().setName(recipeName);
        }

        //if provided name exists already, append a number to make it unique
        int j = 2;
        while(!slaRepository.recipeNameIsUnique(newRecipe.getRecipe().getName())){
            newRecipe.getRecipe().setName(String.format("%s (%d)", newRecipe.getRecipe().getName(), j));
        }

        //persist the recipe to db
        int recipeId = slaRepository.insertRecipe(newRecipe.getRecipe()).get();

        //persist all recipe's ingredients to db
        addIngredientsToRecipe(recipeId, newRecipe.getIngredients());

        //persist any tags also to db
        for(Tag tag : newRecipe.getTags()){
            tag.setRecipeId(recipeId);
            slaRepository.insertTag(tag);
        }

        return recipeId;
    }

    private void addIngredientsToRecipe(long recipeId, List<IngListItem> ingredients) throws ExecutionException, InterruptedException {
        //first, we need to create an IngList to add the IngListItems to and link it to the recipe
        long ingListId = slaRepository.insertIngList(recipeId).get();

        //add each new item to the database
        for (IngListItem ingredient : ingredients){
            slaRepository.insertOrMergeItem(ingListId,ingredient);
        }
    }

    /** Creates an empty Recipe with placeholder name, and returns its db id
     * @return the id of the newly created empty recipe
     */
    public int generateNewRecipeId() throws ExecutionException, InterruptedException {
        //first, find a unique name to use as placeholder
        int i = 1;
        String recipeName;
        do {
            recipeName = String.format("%s %d",
                    getApplication().getResources().getString(R.string.default_recipe_name), i);
            i++;
        } while(!slaRepository.recipeNameIsUnique(recipeName));

        //create the recipe
        Recipe newRecipe = new Recipe();
        newRecipe.setName(recipeName);
        newRecipe.setServes(1);



        //persist it to db
        int recipeId = slaRepository.insertRecipe(newRecipe).get();

        //create and ingList and link it to the new recipe
        slaRepository.insertIngList(recipeId).get();

        return recipeId;
    }

    public void loadFromBackup(Fragment frag, Executor backgroundExecutor){
        slaRepository.deleteAllRecipes();
        Log.d("TOM_TEST", "loadFromBackup started");
        Thread t1 = new Thread(() -> {

            BufferedInputStream bufferedInputStream = new BufferedInputStream(frag.getResources().openRawResource(R.raw.recipe_backup_2021_03_04p));
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(bufferedInputStream));
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    final String backupLine = line;
                    backgroundExecutor.execute(() -> {
                        String[] row = backupLine.split("\\|");
                        //1- notes
                        //2- tomrating
                        //3- tierrating
                        //4- link
                        //5- tag
                        Log.d("TOM_TEST", backupLine);
//                    Log.d("TOM_TEST", String.format("%s\n%s\n%s\n%s\n%s",row[1],row[2],row[3],row[4],row[5]));

                        try {
                            RecipeWithTagsAndIngredients populatedRecipe = slaRepository.getPopulatedRecipeById((generateRecipeIdFromUrl(row[4])));

                            Recipe recipe = populatedRecipe.getRecipe();

                            if (null != row[1] && !row[1].isEmpty()) {
                                recipe.setNotes(row[1].trim());
                            }

                            recipe.setTom_rating(Integer.parseInt(row[2]) * 2);
                            recipe.setTier_rating(Integer.parseInt(row[3]) * 2);

                            if (row.length > 5 && null != row[5] && !row[5].isEmpty()) {
                                slaRepository.insertTag(recipe.getId(), row[5]);
                            }

                            slaRepository.updateRecipe(recipe);
                        } catch (NullPointerException  | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        catch (NumberFormatException e) {
                            Log.e("BACKUP_LOAD", "From url: " + row[4], e);
                        }
                        catch (InvalidRecipeUrlExeception e) {
                            Log.e("BACKUP_LOAD", "Could not load recipe from this url" + row[4]);
                        }
                        catch (InvalidIngredientStringException e) {
                            Log.e("BACKUP_LOAD", e.getMessage() + "\nFrom url: " + row[4]);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t1.start();
    }
}
