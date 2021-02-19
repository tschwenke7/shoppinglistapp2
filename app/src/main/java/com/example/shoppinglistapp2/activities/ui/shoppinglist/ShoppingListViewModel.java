package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaDatabase;
import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.helpers.SlItemUtils;

import java.util.List;

public class ShoppingListViewModel extends AndroidViewModel {
    private final SlaRepository slaRepository;
    private final LiveData<List<SlItem>> allItems;

    public ShoppingListViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        allItems = slaRepository.getSlItems();
    }

    public LiveData<List<SlItem>> getAllItems() {
        return allItems;
    }

    public void deleteSlItems(SlItem... slItems){
        slaRepository.deleteSlItems(slItems);
    }

    private void insertSlItems(SlItem... slItems){
        slaRepository.insertSlItems(slItems);
    }

    public void updateSlItem(SlItem slItem){
        slaRepository.updateSlItem(slItem);
    }

    public void deleteCheckedSlItems(){
        slaRepository.deleteCheckedSlItems();
    }

    public void deleteAllSlItems(){
        slaRepository.deleteAllSlItems();
    }

    /**
     * Checks or unchecks the slItem at the given position in the list
     * @param position - the position of the item to toggle
     */
    public void toggleChecked(int position) {
        SlItem slItem = allItems.getValue().get(position);
        slItem.setChecked(!slItem.isChecked());
        updateSlItem(slItem);
    }

    public void addItems(String inputText) {
        //split input in case of multiple lines
        String[] items = inputText.split("(\\r\\n|\\r|\\n)");

        //convert each line to an item to add and persist to db
        for (String item : items){
            insertSlItems(SlItemUtils.toSlItem(item.trim()));
        }
    }
}