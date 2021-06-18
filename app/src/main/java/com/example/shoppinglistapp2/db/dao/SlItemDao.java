package com.example.shoppinglistapp2.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoppinglistapp2.db.tables.SlItem;

import java.util.List;

@Dao
public interface SlItemDao {
    @Insert
    long insert(SlItem slItem);

    @Query("SELECT * FROM slitems WHERE list_id = :listId ORDER BY checked")
    LiveData<List<SlItem>> getAll(int listId);

    @Query("SELECT * FROM slitems WHERE list_id = :listId AND checked = 0")
    List<SlItem> getAllUncheckedNonLive(int listId);

    @Query("SELECT * FROM slitems WHERE list_id = :listId AND name = :name AND checked = 0")
    SlItem getByName(int listId, String name);

    @Delete
    void deleteAll(SlItem... slItems);

    @Update
    void update(SlItem... slItems);

    @Query("DELETE FROM slitems WHERE list_id = :listId AND checked = 1")
    void clearAllChecked(int listId);

    @Query("DELETE FROM slitems WHERE list_id = :listId")
    void clearAll(int listId);
}
