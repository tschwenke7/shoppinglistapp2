package com.example.shoppinglistapp2.db.tables.withextras;

import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Adds a "score" property for ranking this when searching for recipes matching a given list of
 * ingredients. Do not attempt to update or insert this to db.
 */
public class PopulatedRecipeWithScore implements Comparable<PopulatedRecipeWithScore> {
    private int score;
    private final List<IngListItem> ingredients = new ArrayList<>();
    private final List<Tag> tags;
    private final Recipe recipe;

    public PopulatedRecipeWithScore (RecipeWithTagsAndIngredients recipeWithTagsAndIngredients) {
        this.recipe = recipeWithTagsAndIngredients.getRecipe();
        this.tags = recipeWithTagsAndIngredients.getTags();

        //deep copy ingredients so we don't affect them when checking/unchecking ingredients
        for (IngListItem ingredient : recipeWithTagsAndIngredients.getIngredients()) {
            ingredients.add(ingredient.deepCopy());
        }
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int incrementScore() {
        return this.score ++;
    }

    public List<IngListItem> getIngredients() {
        return ingredients;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Sort in order of highest score to lowest
     * @param o item to compare to
     * @return
     */
    @Override
    public int compareTo(PopulatedRecipeWithScore o) {
        return o.getScore() - this.score ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PopulatedRecipeWithScore that = (PopulatedRecipeWithScore) o;
        return score == that.score && Objects.equals(ingredients, that.ingredients) && Objects.equals(tags, that.tags) && Objects.equals(recipe, that.recipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, ingredients, tags, recipe);
    }
}
