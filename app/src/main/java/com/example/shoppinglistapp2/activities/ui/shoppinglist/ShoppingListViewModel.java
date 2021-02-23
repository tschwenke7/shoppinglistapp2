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
import java.util.HashMap;
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

    private void insertSlItems(SlItem... slItems){
        slaRepository.insertSlItems(slItems);
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

        //convert each line to an item to add and persist to db
        for (String item : items){
            insertSlItems(SlItemUtils.toSlItem(item.trim()));
        }
    }

    public void addItemsFromRecipe(List<Ingredient> ingredients){

        //compile hashmap of slItems to more quickly find duplicate list items
        HashMap<SlItem, SlItem> existingItems = new HashMap<>();
        for(SlItem slItem : slaRepository.getSlItemsNonLive()){
            existingItems.put(slItem, slItem);
        }

        //record list of items to update in db afterwards, if their qtys are edited during the merge
        List<SlItem> itemsToUpdate = new ArrayList<>();
        //for each ingredient, convert it to an SLItem
        //either add that item if it's new,
        for (Ingredient ingredient : ingredients){
            SlItem item = SlItemUtils.toSlItem(ingredient);
            //either merge the qtys if item is already on the list
            if (existingItems.containsKey(item)){
                SlItemUtils.mergeQuantities(existingItems.get(item), item);
                //record reference to this object so we can persist changes to db later
                itemsToUpdate.add(existingItems.get(item));
            }
            //or add a new item to the list
            else{
                insertSlItems(item);

                //todo - allow items being added to be considered as "existing items" for following items
            }
        }

        //update all items with modified qtys in db
        slaRepository.updateSlItems(itemsToUpdate.toArray(new SlItem[itemsToUpdate.size()]));
    }
}