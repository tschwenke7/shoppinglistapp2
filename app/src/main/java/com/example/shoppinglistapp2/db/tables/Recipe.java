package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

@Entity(
        tableName = "recipes",
        indices = {@Index(value = {"name"}, unique = true)}//the recipe name must be unique
)
public class Recipe {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    /**Preparation time in minutes*/
    @ColumnInfo(name="prep_time")
    private int prepTime;

    /**Cooking time in minutes*/
    @ColumnInfo(name = "cook_time")
    private int cookTime;

    private int serves;

    private String url;
    private String notes;

    private int tom_rating;
    private int tier_rating;

    @Ignore
    private List<String> tags;

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

    public int getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(int prepTime) {
        this.prepTime = prepTime;
    }

    public int getCookTime() {
        return cookTime;
    }

    public void setCookTime(int cookTime) {
        this.cookTime = cookTime;
    }

    public int getServes() {
        return serves;
    }

    public void setServes(int serves) {
        this.serves = serves;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Ignore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return id == recipe.id &&
                prepTime == recipe.prepTime &&
                cookTime == recipe.cookTime &&
                tom_rating == recipe.tom_rating &&
                tier_rating == recipe.tier_rating &&
                Objects.equals(name, recipe.name) &&
                Objects.equals(url, recipe.url) &&
                Objects.equals(notes, recipe.notes);
    }
}
