package com.example.shoppinglistapp2.activities.importRecipes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    private final MutableLiveData<List<Tag>> customTagsLive = new MutableLiveData<>();
    private SlaRepository slaRepository;

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

    public LiveData<List<Tag>> getCustomTags() {
        return customTagsLive;
    }

    private void saveRecipe(RecipeWithTagsAndIngredients r) throws ExecutionException, InterruptedException {
        Recipe recipe = r.getRecipe();
        List<IngListItem> ingredients = r.getIngredients();
        List<Tag> tags = r.getTags();

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

        slaRepository.insertTags(tags);
    }

    public void addTag(String tagName) {
        customTags.add(new Tag(tagName));
        customTagsLive.postValue(customTags);
    }

    public void deleteTag(String tagName) {
        for (Tag tag: customTags) {
            if (tag.getName().equals(tagName)) {
                customTags.remove(tag);
                break;
            }
        }
        customTagsLive.postValue(customTags);
    }

    public ListenableFuture<List<String>> getDistinctTagNames() {
        return slaRepository.getDistinctTagNames();
    }
}