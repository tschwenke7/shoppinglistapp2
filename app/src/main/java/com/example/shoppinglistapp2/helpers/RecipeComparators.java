package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;

import java.util.Comparator;

public class RecipeComparators {
    public static class CompareRecipeName implements Comparator<RecipeWithTagsAndIngredients> {
        @Override
        public int compare(RecipeWithTagsAndIngredients r1, RecipeWithTagsAndIngredients r2) {
            return (r1.getRecipe().getName().compareTo(r2.getRecipe().getName()));
        }
    }

    public static class ComparePrepTime implements Comparator<RecipeWithTagsAndIngredients> {
        @Override
        public int compare(RecipeWithTagsAndIngredients r1, RecipeWithTagsAndIngredients r2) {
            return r1.getRecipe().getPrepTime() - r2.getRecipe().getPrepTime();
        }
    }

    public static class CompareTotalTime implements Comparator<RecipeWithTagsAndIngredients> {
        @Override
        public int compare(RecipeWithTagsAndIngredients r1, RecipeWithTagsAndIngredients r2) {
            return (r1.getRecipe().getPrepTime() + r1.getRecipe().getCookTime())
                    - (r2.getRecipe().getPrepTime() + r2.getRecipe().getCookTime());
        }
    }

    public static class CompareTomRating implements Comparator<RecipeWithTagsAndIngredients> {
        @Override
        public int compare(RecipeWithTagsAndIngredients r1, RecipeWithTagsAndIngredients r2) {
            return r2.getRecipe().getTom_rating() - r1.getRecipe().getTom_rating();
        }
    }

    public static class CompareTiernanRating implements Comparator<RecipeWithTagsAndIngredients> {
        @Override
        public int compare(RecipeWithTagsAndIngredients r1, RecipeWithTagsAndIngredients r2) {
            return r2.getRecipe().getTier_rating() - r1.getRecipe().getTier_rating();
        }
    }

    public static class CompareCombinedRating implements Comparator<RecipeWithTagsAndIngredients> {
        @Override
        public int compare(RecipeWithTagsAndIngredients r1, RecipeWithTagsAndIngredients r2) {
            return (r2.getRecipe().getTom_rating() + r2.getRecipe().getTier_rating()) - (r1.getRecipe().getTom_rating() + r1.getRecipe().getTier_rating());
        }
    }
}
