package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.Recipe;


public class RecipeWithIngredients {
    @Embedded public Recipe recipe;

    @Relation(entity = IngList.class,
    parentColumn = "id",
    entityColumn = "recipe_id")
    public IngListWithItems ingredients;

}
