package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;

import java.util.List;
import java.util.Objects;

public class RecipeWithTagsAndIngredients {
    @Embedded
    private Recipe recipe;

    @Relation(entity = IngList.class,
            parentColumn = "id",
            entityColumn = "recipe_id")
    private IngListWithItems ingListWithItems;

    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    private List<Tag> tags;

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public IngListWithItems getIngListWithItems() {
        return ingListWithItems;
    }

    public void setIngListWithItems(IngListWithItems ingListWithItems) {
        this.ingListWithItems = ingListWithItems;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<IngListItem> getIngredients() {
        return ingListWithItems.getItems();
    }

    public void setIngredients(List<IngListItem> ingredients){
        if(null == ingListWithItems){
            ingListWithItems = new IngListWithItems();
            ingListWithItems.setItems(ingredients);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeWithTagsAndIngredients that = (RecipeWithTagsAndIngredients) o;
        return Objects.equals(recipe, that.recipe) && Objects.equals(ingListWithItems, that.ingListWithItems) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipe, ingListWithItems, tags);
    }
}
