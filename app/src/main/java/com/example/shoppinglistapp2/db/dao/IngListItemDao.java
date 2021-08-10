package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.IngListItem;

import java.util.List;

@Dao
public interface IngListItemDao {
    @Query("SELECT * FROM ing_list_items WHERE list_id = :id ORDER BY checked")
    public LiveData<List<IngListItem>> getIngredientsByListId(int id);

    @Query("SELECT * FROM ing_list_items WHERE list_id = :id")
    List<IngListItem> getIngredientsByListIdNonLive(int id);

    @Insert
    void insertAll(IngListItem... ingredients);

    @Insert
    void insertAll(List<IngListItem> ingredients);

    @Delete
    void deleteAll(IngListItem... ingredients);

    @Delete
    int deleteAll(List<IngListItem> ingredients);

    @Update
    void update(IngListItem ingredient);

    @Query("SELECT * FROM ing_list_items WHERE list_id = :listId AND checked = 0")
    List<IngListItem> getAllUncheckedNonLive(int listId);

    @Query("SELECT * FROM ing_list_items WHERE list_id = :listId AND name = :name AND checked = :checked AND id != :id")
    IngListItem getAnotherByName(int listId, String name, boolean checked, int id);

    @Query("DELETE FROM ing_list_items WHERE list_id = :listId AND checked = 1")
    void clearAllChecked(int listId);

    @Query("DELETE FROM ing_list_items WHERE list_id = :listId")
    void clearAll(int listId);
}
