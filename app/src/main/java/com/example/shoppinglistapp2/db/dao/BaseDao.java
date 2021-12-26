package com.example.shoppinglistapp2.db.dao;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;

import java.util.List;

public interface BaseDao<T> {
    @Insert
    long insert (T row);

    @Insert
    List<Long> insertAll(List<T> rows);

    @Update
    int update(T row);

    @Update
    int updateAll(List<T> row);

    @Delete
    int delete(T row);

    @Delete
    int deleteAll(List<T> row);
}
