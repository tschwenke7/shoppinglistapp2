package com.example.shoppinglistapp2.db.tables;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.RoundingMode;
import java.text.DecimalFormat;
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
    indices = {@Index(value = "name")}
)
public class IngListItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;

    @ColumnInfo(name = "list_id", index = true)
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

    @Ignore
    public IngListItem deepCopy() {
        IngListItem copy = new IngListItem();
        copy.setId(this.getId());
        copy.setListId(this.getListId());
        copy.setMassQty(this.getMassQty());
        copy.setMassUnit(this.getMassUnit());
        copy.setVolumeUnit(this.getVolumeUnit());
        copy.setVolumeQty(this.getVolumeQty());
        copy.setWholeItemQty(this.getWholeItemQty());
        copy.setOtherQty(this.getOtherQty());
        copy.setChecked(this.isChecked());
        copy.setName(this.getName());
        return copy;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngListItem that = (IngListItem) o;
        return id == that.id && listId == that.listId && Double.compare(that.volumeQty, volumeQty) == 0 && Double.compare(that.massQty, massQty) == 0 && Double.compare(that.wholeItemQty, wholeItemQty) == 0 && Double.compare(that.otherQty, otherQty) == 0 && checked == that.checked && Objects.equals(name, that.name) && Objects.equals(volumeUnit, that.volumeUnit) && Objects.equals(massUnit, that.massUnit) && Objects.equals(otherUnit, that.otherUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, listId, volumeUnit, volumeQty, massUnit, massQty, wholeItemQty, otherUnit, otherQty, checked);
    }

    @Ignore
    public boolean isEmpty(){
        return wholeItemQty <= 0 && massQty <= 0 && volumeQty <= 0 && otherQty <= 0;
    }

    @Override
    public String toString() {
        DecimalFormat twodp = new DecimalFormat("#.##");
        twodp.setRoundingMode(RoundingMode.CEILING);

        //concatenate different parts of amount that may or may not be present
        String str = "";
        if(volumeQty != 0) {
            str += twodp.format(volumeQty) + " " + volumeUnit + " ";
        }
        if(massQty != 0) {
            if (!str.isEmpty()){
                str += "+ ";
            }
            str += twodp.format(massQty) + " " + massUnit + " ";
        }
        if(otherQty != 0) {
            if (!str.isEmpty()){
                str += "+ ";
            }
            str += twodp.format(otherQty) + " " + otherUnit + " ";
        }
        if (wholeItemQty != 0) {
            if (!str.isEmpty()){
                str += "+ ";
            }
            str += twodp.format(wholeItemQty) + " ";
        }

        //hide amount entirely if it's just "1" (1 whole unit)
        if(str.equals("1 ")){
            return name;
        }

        return str + name;
    }

    @Ignore
    public void negateQuantities() {
        massQty *= -1;
        volumeQty *= -1;
        wholeItemQty *= -1;
        otherQty *= -1;
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<IngListItem> {

        @Override
        public boolean areItemsTheSame(@NonNull IngListItem oldItem, @NonNull IngListItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull IngListItem oldItem, @NonNull IngListItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
