package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.helpers.SlItemUtils;

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

    private void insertOrMergeItem(int listId, SlItem newItem){
        //attempt to find an existing item with the same name
        SlItem existingItemWithSameName = slaRepository.findSlItemWithSameName(listId, newItem);

        //if none found, just insert
        if(null == existingItemWithSameName){
            try {
                //calling "get()" forces the insert to have completed before checking if the next item
                //is already on the list
                newItem.setListId(listId);
                slaRepository.insertSlItem(newItem).get();
            } catch (ExecutionException | InterruptedException e) {
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
        slaRepository.deleteCheckedSlItems(SlItemUtils.SHOPPING_LIST_ID);
    }

    public void deleteAllSlItems(){
        slaRepository.deleteAllSlItems(SlItemUtils.SHOPPING_LIST_ID);
    }

    /**
     * Checks or unchecks the slItem at the given position in the list
     * @param position - the position of the item to toggle
     */
    public void toggleChecked(int position) {
        SlItem item = allItems.getValue().get(position);
        item.setChecked(!item.isChecked());
        slaRepository.deleteSlItems(item);
        insertOrMergeItem(item.getListId(), item);
    }

    public void addItems(String inputText) {
        //split input in case of multiple lines
        String[] items = inputText.split("(\\r\\n|\\r|\\n)");

        //convert each line to an item
        //and either add it or merge it with an existing item of same name
        for (String item : items){
            insertOrMergeItem(SlItemUtils.SHOPPING_LIST_ID, SlItemUtils.toSlItem(item.trim()));
        }
    }

    public void addIngredientsToShoppingList(List<Ingredient> ingredients){
        //for each ingredient, convert it to an SLItem
        //either add that item if it's new, or merge qtys if it already existed
        for (Ingredient ingredient : ingredients){
            SlItem item = SlItemUtils.toSlItem(ingredient);
            insertOrMergeItem(SlItemUtils.SHOPPING_LIST_ID, item);
        }
    }

    public void addItemsToShoppingList(List<SlItem> slItems) {
        for (SlItem item : slItems){
            insertOrMergeItem(SlItemUtils.SHOPPING_LIST_ID, new SlItem(item));
        }
    }

    public void editItem(SlItem oldItem, String newItemString) {
        //convert user's string to a new item
        SlItem newItem = SlItemUtils.toSlItem(newItemString);

        //copy values across
        oldItem.setQty1(newItem.getQty1());
        oldItem.setUnit1(newItem.getUnit1());
        oldItem.setName(newItem.getName());

        //if name of ingredient has been changed to one which already exists,
        //we need to merge it with an existing item.
        //therefore, we delete the old item and then merge the modified one in.
        SlItem existingItemWithSameName = slaRepository.findSlItemWithSameName(oldItem.getListId(), oldItem);
        if (null != existingItemWithSameName){
            slaRepository.deleteSlItems(oldItem);
            SlItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateSlItems(existingItemWithSameName);
        }

        //if it wasn't changed to an existing item, simply update this item in the db
        else{
            slaRepository.updateSlItems(oldItem);
        }
    }

    public String  getAllItemsAsString() {
        try{
            StringBuilder builder = new StringBuilder();
            for (SlItem item : allItems.getValue()){
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