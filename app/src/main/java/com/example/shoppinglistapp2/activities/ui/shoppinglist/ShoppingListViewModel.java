package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.example.shoppinglistapp2.helpers.InvalidIngredientStringException;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ShoppingListViewModel extends AndroidViewModel {
    private final SlaRepository slaRepository;
    private final LiveData<List<IngListItem>> slItems;

    public ShoppingListViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        //makes sure that the shopping list's row in "ing_lists" table exists
        slaRepository.insertOrIgnoreShoppingList();

        slItems = slaRepository.getSlItems();
    }

    public LiveData<List<IngListItem>> getSlItems() {
        return slItems;
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
        //if we directly edit the item from the list which the adapter is using, it won't be different
        //when setList is called.
        IngListItem item = slItems.getValue().get(position).deepCopy();

        item.setChecked(!item.isChecked());
        slaRepository.deleteIngListItem(item);
        slaRepository.insertOrMergeItem(item.getListId(), item);
    }

    public void addItems(String inputText) throws InvalidIngredientStringException {
        //split input in case of multiple lines
        String[] items = inputText.split("(\\r\\n|\\r|\\n)");

        //convert each line to an item
        //and either add it or merge it with an existing item of same name
        for (String item : items){
            slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, IngListItemUtils.toIngListItem(item.trim()));
        }

    }

    public void addItemsToShoppingList(List<IngListItem> ingListItems) {
        for (IngListItem item : ingListItems){
            slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, item);
        }
    }

    public void editItem(IngListItem oldItem, String newItemString) throws InvalidIngredientStringException {
        //convert user's string to a new item
        IngListItem newItem = IngListItemUtils.toIngListItem(newItemString);

        //copy identifying values over from oldItem to new item
        newItem.setId(oldItem.getId());
        newItem.setListId(oldItem.getListId());
        newItem.setChecked(oldItem.isChecked());

        //if name of ingredient has been changed to one which already exists,
        //we need to merge it with an existing item.
        //therefore, we delete the old item and then merge the modified one in.
        IngListItem existingItemWithSameName = slaRepository.findItemWithMatchingName(oldItem.getListId(), newItem);
        if (null != existingItemWithSameName){
            slaRepository.deleteIngListItem(oldItem);
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateOrDeleteIfEmptyIngListItem(existingItemWithSameName);
        }

        //if it wasn't changed to an existing item, simply update the original item in the db
        else{
            //since newItem has the same "id" as oldItem, it will overwrite the database entry
            slaRepository.updateOrDeleteIfEmptyIngListItem(newItem);
        }
    }

    public String getAllItemsAsString(boolean includeChecked) {
        try{
            StringBuilder builder = new StringBuilder();
            for (IngListItem item : slItems.getValue()){
                if(includeChecked || !item.isChecked()){
                    builder.append(item.toString());
                    builder.append("\n");
                }
            }
            return builder.toString().trim();
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            return "";
        }
    }
}