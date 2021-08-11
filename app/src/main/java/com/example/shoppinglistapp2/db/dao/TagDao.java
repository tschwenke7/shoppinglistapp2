package com.example.shoppinglistapp2.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.tables.Tag;

import java.util.List;

@Dao
public interface TagDao extends BaseDao<Tag> {
    @Query("DELETE FROM tags WHERE recipe_id = :recipeId AND name = :tagName")
    void delete(int recipeId, String tagName);

    @Query("SELECT DISTINCT name FROM tags")
    String[] getAllTags();

    @Query("SELECT name FROM tags WHERE recipe_id = :recipeId")
    List<String> getTagsByRecipe(int recipeId);

    @Insert
    List<Long> insertAll(List<Tag> tags);

    @Query("DELETE FROM tags WHERE recipe_id = :recipeId")
    int deleteAllByRecipeId(int recipeId);
}
