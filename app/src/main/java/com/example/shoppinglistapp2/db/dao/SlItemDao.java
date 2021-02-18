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
    public void insertAll(SlItem... slItems);

    @Query("SELECT * FROM slitems")
    public LiveData<List<SlItem>> getAll();

    @Delete
    public void deleteAll(SlItem... slItems);

    @Update
    public void update(SlItem slItem);

    @Query("DELETE FROM slitems WHERE checked = 1")
    public void clearAllChecked();

    @Query("DELETE FROM slitems")
    public void clearAll();
}
