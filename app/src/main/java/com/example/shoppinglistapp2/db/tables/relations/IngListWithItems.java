package com.example.shoppinglistapp2.db.tables.relations;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Relation;

import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.IngListItem;

import java.util.List;

public class IngListWithItems {
    @Embedded public IngList ingList;
    @Relation(parentColumn = "id", entityColumn = "list_id")
    public List<IngListItem> ingListItems;
}
