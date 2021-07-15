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

    /**
     * 0 - Shopping list
     * 1+ - Meal plan ingredient list (corresponds to meal_plans.plan_id)
     * NOTE: only 1 meal plan can exist in the current implementation
     */
    @ColumnInfo(defaultValue = "0", name = "list_id")
    private int listId;

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

    public SlItem(){}

    /** Copy constructor to make a deep copy of an SlItem,
     * but without setting the id (which would violate primary key constraint) */
    @Ignore
    public SlItem(SlItem copy) {
        this.listId = copy.getListId();
        this.name = copy.getName();
        this.qty1 = copy.getQty1();
        this.unit1 = copy.getUnit1();
        this.qty2 = copy.getQty2();
        this.unit2 = copy.getUnit2();
        this.checked = copy.isChecked();
    }

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
        SlItem slItem = (SlItem) o;
        return checked == slItem.isChecked() &&
                name.equals(slItem.getName()); //&&
//                listId == slItem.getListId();
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

    public int getListId() {
        return listId;
    }

    public void setListId(int listId) {
        this.listId = listId;
    }

    @Ignore
    @Override
    public String toString(){
        String str = "" + qty1;
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
