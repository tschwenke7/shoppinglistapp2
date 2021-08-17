package com.example.shoppinglistapp2.activities.ui;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class BaseDiffCallback<T> extends DiffUtil.Callback {
    protected List<T> newList;
    protected List<T> oldList;

    public BaseDiffCallback(List<T> newList, List<T> oldList) {
        this.newList = newList;
        this.oldList = oldList;
    }

    @Override
    public int getOldListSize() {
        if(oldList == null){
            return 0;
        }
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        if (newList == null){
            return 0;
        }
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition) == newList.get(newItemPosition);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
