package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.Objects;

public class MealWithRecipe {
    @Embedded
    private Meal meal;

    @Relation(parentColumn = "recipe_id", entityColumn = "id")
    private Recipe recipe;

    public Meal getMeal() {
        return meal;
    }

    public void setMeal(Meal meal) {
        this.meal = meal;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MealWithRecipe that = (MealWithRecipe) o;
        return Objects.equals(meal, that.meal) && Objects.equals(recipe, that.recipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meal, recipe);
    }
}
