package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Fts4;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "ingredients",
        foreignKeys = @ForeignKey(
                entity = Recipe.class,
                parentColumns = "id",
                childColumns = "recipe_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class Ingredient {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    @NonNull
    @ColumnInfo(name = "recipe_id")
    private int recipeId;

    private String qty;

    private String unit;

    public Ingredient(@NonNull String name, @NonNull int recipeId) {
        this.name = name;
        this.recipeId = recipeId;
    }

    @Ignore
    public Ingredient(@NonNull String name, String qty, String unit) {
        this.name = name;
        this.qty = qty;
        this.unit = unit;
    }

    @Ignore
    public Ingredient(@NonNull String name, String qty, String unit, int recipeId) {
        this.name = name;
        this.recipeId = recipeId;
        this.qty = qty;
        this.unit = unit;
    }

    @Ignore
    public Ingredient(@NonNull String name) {
        this.name = name;
    }

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

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Ignore
    public String toString() {
        String str = "";
        //prepend qty and unit if present to the ingredient's name
        if (null != qty) {
            str += qty + " ";
        }
        if (null != unit) {
            str += unit + " ";
        }

        return str + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return name.equals(that.name) &&
                Objects.equals(qty, that.qty) &&
                Objects.equals(unit, that.unit);
    }
}
