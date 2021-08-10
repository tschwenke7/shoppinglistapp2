package com.example.shoppinglistapp2.db.tables;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ing_list")
public class IngList {
    @PrimaryKey(autoGenerate = true)
    private int id;

    public IngList(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
