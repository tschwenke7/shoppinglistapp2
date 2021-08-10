package com.example.shoppinglistapp2.db.tables;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "meals",
        foreignKeys = {
            @ForeignKey(
                entity = Recipe.class,
                parentColumns = "id",
                childColumns = "recipe_id",
                onDelete = ForeignKey.SET_NULL
            ),
            @ForeignKey(
                entity = MealPlan.class,
                parentColumns = "id",
                childColumns = "plan_id",
                onDelete = ForeignKey.CASCADE
            )
        }
)
public class Meal {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "plan_id")
    private int planId;

    @ColumnInfo(name = "day_id")
    private int dayId;

    @ColumnInfo(name = "day_title")
    private String dayTitle;

    @ColumnInfo(name = "recipe_id")
    private Integer recipeId;

    private String notes;

    @Ignore
    private Recipe recipe;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    @Ignore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meal meal = (Meal) o;
        return id == meal.id && planId == meal.planId && dayId == meal.dayId && Objects.equals(dayTitle, meal.dayTitle) && Objects.equals(recipeId, meal.recipeId) && Objects.equals(notes, meal.notes) && Objects.equals(recipe, meal.recipe);
    }

    @Ignore
    @Override
    public int hashCode() {
        return Objects.hash(id, planId, dayId, dayTitle, recipeId, notes, recipe);
    }
}
