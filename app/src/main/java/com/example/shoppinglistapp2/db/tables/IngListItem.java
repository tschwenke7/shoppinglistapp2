package com.example.shoppinglistapp2.db.tables;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
    tableName = "ing_list_items",
    foreignKeys = {
        @ForeignKey(
                entity = IngList.class,
                parentColumns = "id",
                childColumns = "list_id",
                onDelete = ForeignKey.CASCADE
        )
    },
    indices = {@Index(value = "list_id, name")}
)
public class IngListItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;

    private long listId;

    @ColumnInfo(name = "volume_unit")
    private String volumeUnit;

    @ColumnInfo(name = "volume_qty")
    private double volumeQty;

    @ColumnInfo(name = "mass_unit")
    private String massUnit;

    @ColumnInfo(name = "mass_qty")
    private double massQty;

    @ColumnInfo(name = "whole_item_qty")
    private double wholeItemQty;

    @ColumnInfo(name = "other_unit")
    private String otherUnit;

    @ColumnInfo(name = "other_qty")
    private double otherQty;

    @ColumnInfo(defaultValue = "0")
    private boolean checked;

    public IngListItem(){}

    @Ignore
    public IngListItem(String name) {
        this.name = name;
    }

    @Ignore
    public IngListItem(String name, long listId) {
        this.name = name;
        this.listId = listId;
    }

    @Ignore
    public IngListItem(long listId) {
        this.listId = listId;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
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

    public String getVolumeUnit() {
        return volumeUnit;
    }

    public void setVolumeUnit(String volumeUnit) {
        this.volumeUnit = volumeUnit;
    }

    public double getVolumeQty() {
        return volumeQty;
    }

    public void setVolumeQty(double volumeQty) {
        this.volumeQty = volumeQty;
    }

    public String getMassUnit() {
        return massUnit;
    }

    public void setMassUnit(String massUnit) {
        this.massUnit = massUnit;
    }

    public double getMassQty() {
        return massQty;
    }

    public void setMassQty(double massQty) {
        this.massQty = massQty;
    }

    public double getWholeItemQty() {
        return wholeItemQty;
    }

    public void setWholeItemQty(double wholeItemQty) {
        this.wholeItemQty = wholeItemQty;
    }

    public String getOtherUnit() {
        return otherUnit;
    }

    public void setOtherUnit(String otherUnit) {
        this.otherUnit = otherUnit;
    }

    public double getOtherQty() {
        return otherQty;
    }

    public void setOtherQty(double otherQty) {
        this.otherQty = otherQty;
    }

    public long getListId() {
        return listId;
    }

    public void setListId(long listId) {
        this.listId = listId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngListItem that = (IngListItem) o;
        return id == that.id && Double.compare(that.volumeQty, volumeQty) == 0 && Double.compare(that.massQty, massQty) == 0 && Double.compare(that.wholeItemQty, wholeItemQty) == 0 && Double.compare(that.otherQty, otherQty) == 0 && Objects.equals(name, that.name) && Objects.equals(volumeUnit, that.volumeUnit) && Objects.equals(massUnit, that.massUnit) && Objects.equals(otherUnit, that.otherUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, volumeUnit, volumeQty, massUnit, massQty, wholeItemQty, otherUnit, otherQty);
    }

    @Override
    public String toString() {
        //concatenate different parts of amount that may or may not be present
        String str = "";
        if (wholeItemQty != 0) {
            str += wholeItemQty + " ";
        }
        if(volumeQty != 0) {
            if (!str.isEmpty()){
                str += "+ ";
            }
            str += volumeQty + " " + volumeUnit + " ";
        }
        if(massQty != 0) {
            if (!str.isEmpty()){
                str += "+ ";
            }
            str += massQty + " " + massUnit + " ";
        }
        if(otherQty != 0) {
            if (!str.isEmpty()){
                str += "+ ";
            }
            str += otherQty + " " + otherUnit + " ";
        }

        //hide amount entirely if it's just "1" (1 whole unit)
        if(str.equals("1 ")){
            return name;
        }

        return str + name;
    }
}
