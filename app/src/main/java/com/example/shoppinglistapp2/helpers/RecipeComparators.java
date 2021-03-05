package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.Comparator;

public class RecipeComparators {
    public static class CompareRecipeName implements Comparator<Recipe> {
        @Override
        public int compare(Recipe r1, Recipe r2) {
            return (r1.getName().compareTo(r2.getName()));
        }
    }

    public static class ComparePrepTime implements Comparator<Recipe> {
        @Override
        public int compare(Recipe r1, Recipe r2) {
            return r1.getPrepTime() - r2.getPrepTime();
        }
    }

    public static class CompareTotalTime implements Comparator<Recipe> {
        @Override
        public int compare(Recipe r1, Recipe r2) {
            return (r1.getPrepTime() + r1.getCookTime()) - (r2.getPrepTime() + r2.getCookTime());
        }
    }

    public static class CompareTomRating implements Comparator<Recipe> {
        @Override
        public int compare(Recipe r1, Recipe r2) {
            return r2.getTom_rating() - r1.getTom_rating();
        }
    }

    public static class CompareTiernanRating implements Comparator<Recipe> {
        @Override
        public int compare(Recipe r1, Recipe r2) {
            return r2.getTier_rating() - r1.getTier_rating();
        }
    }

    public static class CompareCombinedRating implements Comparator<Recipe> {
        @Override
        public int compare(Recipe r1, Recipe r2) {
            return (r2.getTom_rating() + r2.getTier_rating()) - (r1.getTom_rating() + r1.getTier_rating());
        }
    }
}
