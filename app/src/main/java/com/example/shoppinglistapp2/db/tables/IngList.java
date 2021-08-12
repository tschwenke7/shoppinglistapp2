package com.example.shoppinglistapp2.db.tables;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "ing_lists",
    foreignKeys = {
        @ForeignKey(
                entity = Recipe.class,
                parentColumns = "id",
                childColumns = "recipe_id",
                onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
                entity = MealPlan.class,
                parentColumns = "id",
                childColumns = "meal_plan_id",
                onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "recipe_id"),
        @Index(value = "meal_plan_id")
    }
)
public class IngList {
    @Ignore
    public static int TYPE_RECIPE = 1, TYPE_LIST = 2;

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "recipe_id")
    private Long recipeId;

    @ColumnInfo(name = "meal_plan_id")
    private Long mealPlanId;

    public IngList(){}

    @Ignore
    public IngList(long refId, int listType){
        if (listType == 1){
            recipeId = refId;
        }
        else{
            mealPlanId = refId;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public Long getMealPlanId() {
        return mealPlanId;
    }

    public void setMealPlanId(Long mealPlanId) {
        this.mealPlanId = mealPlanId;
    }
}
