package com.example.shoppinglistapp2.db.dao;

import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.shoppinglistapp2.db.tables.IngList;

@Dao()
public interface IngListDao extends BaseDao<IngList> {

    @Query("SELECT * FROM ing_lists WHERE id = :id")
    IngList getById(int id);

    @Query("INSERT INTO ing_lists(id) VALUES(:id)")
    void insertShoppingList(int id);
}
