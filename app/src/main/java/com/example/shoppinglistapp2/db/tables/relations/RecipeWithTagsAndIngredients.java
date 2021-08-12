package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;

import java.util.List;

public class RecipeWithTagsAndIngredients {
    @Embedded
    public Recipe recipe;

    @Relation(entity = IngList.class,
            parentColumn = "id",
            entityColumn = "recipe_id")
    public IngListWithItems ingredients;

    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    public List<Tag> tags;
}
