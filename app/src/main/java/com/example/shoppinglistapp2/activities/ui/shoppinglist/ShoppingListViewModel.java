package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.helpers.SlItemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    private void insertOrMergeItem(SlItem newItem){
        //attempt to find an existing item with the same name
        SlItem existingItemWithSameName = slaRepository.getSlItemByName(newItem.getName());

        //if none found, just insert
        if(null == existingItemWithSameName){
            try {
                //calling "get()" forces the insert to have completed before checking if the next item
                //is already on the list
                slaRepository.insertSlItem(newItem).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        //if one was found, merge their quantities then persist the change
        else{
            SlItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateSlItems(existingItemWithSameName);
        }
    }

    public void updateSlItem(SlItem slItem){
        slaRepository.updateSlItems(slItem);
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

        //convert each line to an item
        //and either add it or merge it with an existing item of same name
        for (String item : items){
            insertOrMergeItem(SlItemUtils.toSlItem(item.trim()));
        }
    }

    public void addItemsFromRecipe(List<Ingredient> ingredients){
        //record list of items to update in db afterwards, if their qtys are edited during the merge
        List<SlItem> itemsToUpdate = new ArrayList<>();
        //for each ingredient, convert it to an SLItem
        //either add that item if it's new,
        for (Ingredient ingredient : ingredients){
            SlItem item = SlItemUtils.toSlItem(ingredient);
            insertOrMergeItem(item);
        }

        //update all items with modified qtys in db
        slaRepository.updateSlItems(itemsToUpdate.toArray(new SlItem[itemsToUpdate.size()]));
    }
}