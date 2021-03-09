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

    @Query("SELECT * FROM slitems ORDER BY checked")
    LiveData<List<SlItem>> getAll();

    @Query("SELECT * FROM slitems WHERE checked = 0")
    List<SlItem> getAllUncheckedNonLive();

    @Query("SELECT * FROM slitems WHERE name = :name")
    SlItem getByName(String name);

    @Delete
    void deleteAll(SlItem... slItems);

    @Update
    void update(SlItem... slItems);

    @Query("DELETE FROM slitems WHERE checked = 1")
    void clearAllChecked();

    @Query("DELETE FROM slitems")
    void clearAll();
}
