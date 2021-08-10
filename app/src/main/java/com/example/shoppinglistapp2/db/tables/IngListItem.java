package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import androidx.room.Ignore;
import androidx.room.PrimaryKey;


import java.util.Objects;

@Entity(tableName = "ing_list_items")
public class IngListItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "list_id")
    private int listId;

    @NonNull
    private String name;

    @ColumnInfo(defaultValue = "0")
    private boolean checked;

    public IngListItem() {
    }

    @Ignore
    public IngListItem(String name) {
        this.name = name;
    }

    @Ignore
    public IngListItem(int listId, @NonNull String name) {
        this.listId = listId;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getListId() {
        return listId;
    }

    public void setListId(int listId) {
        this.listId = listId;
    }

    @Ignore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngListItem that = (IngListItem) o;
        return id == that.id && name.equals(that.name);
    }

    @Ignore
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
