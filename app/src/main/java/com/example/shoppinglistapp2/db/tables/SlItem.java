package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "slitems"
        )
public class SlItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    private String qty1;

    /** Store "na" if unit is just generic "item/s"*/
    private String unit1;

    private String qty2;

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
    @Ignore
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
    @Ignore
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

    public String getQty1() {
        return qty1;
    }

    public void setQty1(String qty1) {
        this.qty1 = qty1;
    }

    public String getUnit1() {
        return unit1;
    }

    public void setUnit1(String unit1) {
        this.unit1 = unit1;
    }

    public String getQty2() {
        return qty2;
    }

    public void setQty2(String qty2) {
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

    @Ignore
    @Override
    public String toString(){
        String str = qty1;
        if(null != unit1 && !"na".equals(unit1)){
            str += " " + unit1;
        }
        if(qty2 != null){
            str += " + " + qty2;
            if(null != unit2 && !"na".equals(unit2)){
                str += " " + unit2;
            }
        }

        //if there was only default qty and no unit, hide the "1 " for readability
        if(str.equals("1")){
            return name;
        }

        str += " " + name;
        return str;
    }
}
