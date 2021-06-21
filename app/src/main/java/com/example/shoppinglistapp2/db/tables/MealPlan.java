package com.example.shoppinglistapp2.db.tables;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "meal_plans",
        foreignKeys = @ForeignKey(
                entity = Recipe.class,
                parentColumns = "id",
                childColumns = "recipe_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class MealPlan {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(defaultValue = "1", name = "plan_id")
    private int planId;

    @ColumnInfo(name = "day_id")
    private int dayId;

    @ColumnInfo(name = "day_title")
    private String dayTitle;

    @ColumnInfo(name = "recipe_id")
    private Integer recipeId;

    private String notes;

    @Ignore
    public Recipe recipe;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getDayId() {
        return dayId;
    }

    public void setDayId(int dayId) {
        this.dayId = dayId;
    }

    public String getDayTitle() {
        return dayTitle;
    }

    public void setDayTitle(String dayTitle) {
        this.dayTitle = dayTitle;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
        MealPlan mealPlan = (MealPlan) o;
        return id == mealPlan.id &&
                planId == mealPlan.planId &&
                dayId == mealPlan.dayId &&
                Objects.equals(dayTitle, mealPlan.dayTitle) &&
                Objects.equals(recipeId, mealPlan.recipeId) &&
                Objects.equals(notes, mealPlan.notes) &&
                Objects.equals(recipe, mealPlan.recipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, planId, dayId, dayTitle, recipeId, notes, recipe);
    }
}
