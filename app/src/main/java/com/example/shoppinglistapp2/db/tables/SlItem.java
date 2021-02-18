package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "slitems",
        indices = {@Index(value = {"name"}, unique = true)}//the list item name must be unique)
        )
public class SlItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    private int qty1;

    /** Store "na" if unit is just generic "item/s"*/
    private String unit1;

    private int qty2;

    /** Store "na" if unit is just generic "item/s"*/
    private String unit2;

    @ColumnInfo(defaultValue = "0")
    private boolean checked;

    /**
     * Test whether this list item is the same type of item/is checked off or not.
     * Does not consider qty
     * @param o object for comparison
     * @return true if same type of item/is checked or not
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlItem SLItem = (SlItem) o;
        return checked == SLItem.checked &&
                name.equals(SLItem.name);
    }

    /**
     * Generates hashcode based solely on name and checked - ignores qtys
     * Does not consider qty
     * @return - hashcode for this type of item
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, checked);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getQty1() {
        return qty1;
    }

    public void setQty1(int qty1) {
        this.qty1 = qty1;
    }

    public String getUnit1() {
        return unit1;
    }

    public void setUnit1(String unit1) {
        this.unit1 = unit1;
    }

    public int getQty2() {
        return qty2;
    }

    public void setQty2(int qty2) {
        this.qty2 = qty2;
    }

    public String getUnit2() {
        return unit2;
    }

    public void setUnit2(String unit2) {
        this.unit2 = unit2;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
