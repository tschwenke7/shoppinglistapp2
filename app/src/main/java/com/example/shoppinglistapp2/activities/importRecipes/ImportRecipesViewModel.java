package com.example.shoppinglistapp2.activities.importRecipes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ImportRecipesViewModel extends AndroidViewModel {

    private final MutableLiveData<List<RecipeWithTagsAndIngredients>> recipesToImport = new MutableLiveData<>();
    private final List<Tag> customTags = new ArrayList<>();
    private SlaRepository slaRepository;

    public static final int DELETE_OLD = 0;
    public static final int DELETE_NEW = 1;
    public static final int KEEP_BOTH = 2;
    public static final int NOT_SET = -1;

    public ImportRecipesViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
    }

    public void importFromJson(String jsonRecipes) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<RecipeWithTagsAndIngredients>>(){}.getType();
        List<RecipeWithTagsAndIngredients> recipes = gson.fromJson(jsonRecipes, listType);

        //remove rating data from imported recipes - user will set that themselves
        for (RecipeWithTagsAndIngredients recipe : recipes) {
            recipe.getRecipe().setTom_rating(0);
            recipe.getRecipe().setTier_rating(0);
        }

        recipesToImport.postValue(recipes);
    }

    public LiveData<List<RecipeWithTagsAndIngredients>> getRecipesToImport() {
        return recipesToImport;
    }

    public void saveRecipe(RecipeWithTagsAndIngredients r) throws ExecutionException, InterruptedException, DuplicateRecipeNameException {
        if (!slaRepository.recipeNameIsUnique(r.getRecipe().getName())) {
            throw new DuplicateRecipeNameException();
        }
        saveRecipe(r, NOT_SET);
    }

    public void saveRecipe(RecipeWithTagsAndIngredients r, int conflictStrategy) throws ExecutionException, InterruptedException {
        Recipe recipe = r.getRecipe();
        List<IngListItem> ingredients = r.getIngredients();
        List<Tag> tags = r.getTags();

        //check if recipe name is unique
        if (!slaRepository.recipeNameIsUnique(recipe.getName())) {
            switch (conflictStrategy) {
                case DELETE_OLD:
                    slaRepository.deleteRecipe(slaRepository.getRecipeByName(recipe.getName())).get();
                    break;
                case DELETE_NEW:
                    return;
                case KEEP_BOTH:
                    String originalName = recipe.getName();
                    int i = 2;
                    do {
                        recipe.setName(String.format("%s (%d)", originalName, i));
                        i++;
                    } while(!slaRepository.recipeNameIsUnique(recipe.getName()));
                    break;
            }
        }

        //unset recipe id
        recipe.setId(0);

        //insert the recipe and get its id
        long recipeId = slaRepository.insertRecipe(recipe).get();

        //create new ingList, linking to recipe id, and record the id
        long ingListId = slaRepository.insertIngList(recipeId).get();

        //for each ingredient
        for(IngListItem item : ingredients) {
            //unset id
            item.setId(0);
            //set their listId to that of the new ingList
            item.setListId(ingListId);

            item.setChecked(false);
        }

        //insert ingredients
        slaRepository.insertIngListItems(ingredients);

        //for each tag
        for (Tag tag : tags) {
            //unset id
            tag.setId(0);
            //set recipeId to new recipe's id
            tag.setRecipeId((int) recipeId);
        }

        //add extra tags if provided
        for (Tag tag: customTags) {
            tag.setRecipeId((int) recipeId);
        }

        slaRepository.insertTags(tags);
        slaRepository.insertTags(customTags);
    }

    public void addTag(String tagName) {
        customTags.add(new Tag(tagName));
    }

    public void deleteTag(String tagName) {
        for (Tag tag: customTags) {
            if (tag.getName().equals(tagName)) {
                customTags.remove(tag);
                break;
            }
        }
    }

    public ListenableFuture<List<String>> getDistinctTagNames() {
        return slaRepository.getDistinctTagNames();
    }

    public static class DuplicateRecipeNameException extends Exception {}
}