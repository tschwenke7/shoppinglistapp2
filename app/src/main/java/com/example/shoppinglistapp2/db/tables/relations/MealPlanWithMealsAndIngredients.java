package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.MealPlan;

import java.util.List;

public class MealPlanWithMealsAndIngredients {
    @Embedded
    private MealPlan mealPlan;

    @Relation(entity = IngList.class,
            parentColumn = "id",
            entityColumn = "meal_plan_id")
    private IngListWithItems ingListWithItems;

    @Relation(entity = MealWithRecipe.class,
            parentColumn = "id",
            entityColumn = "plan_id")
    private List<MealWithRecipe> meals;

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    public void setMealPlan(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    }

    public IngListWithItems getIngListWithItems() {
        return ingListWithItems;
    }

    public void setIngListWithItems(IngListWithItems ingListWithItems) {
        this.ingListWithItems = ingListWithItems;
    }

    public List<MealWithRecipe> getMeals() {
        return meals;
    }

    public void setMeals(List<MealWithRecipe> meals) {
        this.meals = meals;
    }

    public List<IngListItem> getIngredients(){
        return ingListWithItems.getItems();
    }
}
