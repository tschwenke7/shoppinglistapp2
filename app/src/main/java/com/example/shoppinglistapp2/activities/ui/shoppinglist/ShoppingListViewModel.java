package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ShoppingListViewModel extends AndroidViewModel {
    private final SlaRepository slaRepository;
    private final LiveData<List<IngListItem>> slItems;

    public ShoppingListViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        slItems = slaRepository.getSlItems();
    }

    public LiveData<List<IngListItem>> getSlItems() {
        return slItems;
    }

    private void insertOrMergeItem(long listId, IngListItem newItem){
        //attempt to find an existing item with the same name
        IngListItem existingItemWithSameName = slaRepository.findIngListItemWithSameName(listId, newItem);

        //if none found, just insert
        if(null == existingItemWithSameName){
            try {
                //calling "get()" forces the insert to have completed before checking if the next item
                //is already on the list
                newItem.setListId(listId);
                slaRepository.insertIngListItem(newItem).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }
        //if one was found, merge their quantities then persist the change
        else{
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateIngListItem(existingItemWithSameName);
        }
    }

    public void updateIngListItem(IngListItem ingListItem){
        slaRepository.updateIngListItem(ingListItem);
    }

    public void deleteCheckedSlItems(){
        slaRepository.deleteCheckedIngListItems(IngListItemUtils.SHOPPING_LIST_ID);
    }

    public void clearShoppingList(){
        slaRepository.deleteAllIngListItems(IngListItemUtils.SHOPPING_LIST_ID);
    }

    /**
     * Checks or unchecks the slItem at the given position in the list
     * @param position - the position of the item to toggle
     */
    public void toggleChecked(int position) {
        IngListItem item = slItems.getValue().get(position);
        item.setChecked(!item.isChecked());
        slaRepository.deleteIngListItem(item);
        insertOrMergeItem(item.getListId(), item);
    }

    public void addItems(String inputText) {
        //split input in case of multiple lines
        String[] items = inputText.split("(\\r\\n|\\r|\\n)");

        //convert each line to an item
        //and either add it or merge it with an existing item of same name
        for (String item : items){
            insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, IngListItemUtils.toIngListItem(item.trim()));
        }
    }

    public void addItemsToShoppingList(List<IngListItem> ingListItems) {
        for (IngListItem item : ingListItems){
            insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, item);
        }
    }

    public void editItem(IngListItem oldItem, String newItemString) {
        //convert user's string to a new item
        IngListItem newItem = IngListItemUtils.toIngListItem(newItemString);

        //if name of ingredient has been changed to one which already exists,
        //we need to merge it with an existing item.
        //therefore, we delete the old item and then merge the modified one in.
        IngListItem existingItemWithSameName = slaRepository.findIngListItemWithSameName(oldItem.getListId(), oldItem);
        if (null != existingItemWithSameName){
            slaRepository.deleteIngListItem(oldItem);
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateIngListItem(existingItemWithSameName);
        }

        //if it wasn't changed to an existing item, simply update the original item in the db
        else{
            //copy identifying values over from oldItem to new item, then update it in the db
            //to overwrite all other values of oldItem
            newItem.setId(oldItem.getId());
            newItem.setListId(oldItem.getListId());
            newItem.setChecked(oldItem.isChecked());
            slaRepository.updateIngListItem(oldItem);
        }
    }

    public String getAllItemsAsString() {
        try{
            StringBuilder builder = new StringBuilder();
            for (IngListItem item : slItems.getValue()){
                builder.append(item.toString());
                builder.append("\n");
            }
            return builder.toString().trim();
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            return "";
        }
    }
}