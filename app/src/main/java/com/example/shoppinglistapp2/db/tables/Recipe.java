package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

@Fts4
@Entity(
    tableName = "recipes",
    indices = {@Index(value = {"name"}, unique = true)}//the recipe name must be unique
)
public class Recipe {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    private int id;

    @NonNull
    private String name;

    private String category;
    private String url;
    private String notes;

    private int tom_rating;
    private int tier_rating;

    @Ignore
    private List<Ingredient> ingredients;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getTom_rating() {
        return tom_rating;
    }

    public void setTom_rating(int tom_rating) {
        this.tom_rating = tom_rating;
    }

    public int getTier_rating() {
        return tier_rating;
    }

    public void setTier_rating(int tier_rating) {
        this.tier_rating = tier_rating;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }
}
