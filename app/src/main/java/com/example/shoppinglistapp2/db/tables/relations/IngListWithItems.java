package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.IngListItem;

import java.util.List;
import java.util.Objects;

public class IngListWithItems {
    @Embedded private IngList ingList;

    @Relation(parentColumn = "id", entityColumn = "list_id")
    private List<IngListItem> items;

    public IngList getIngList() {
        return ingList;
    }

    public void setIngList(IngList ingList) {
        this.ingList = ingList;
    }

    public List<IngListItem> getItems() {
        return items;
    }

    public void setItems(List<IngListItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngListWithItems that = (IngListWithItems) o;
        return Objects.equals(ingList, that.ingList) && Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingList, items);
    }
}
