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
        //makes sure that the shopping list's row in "ing_lists" table exists
        slaRepository.insertOrIgnoreShoppingList();

        slItems = slaRepository.getSlItems();
    }

    public LiveData<List<IngListItem>> getSlItems() {
        return slItems;
    }

    private void insertOrMergeItem(long listId, IngListItem newItem){
        IngListItem existingItemWithSameName = findItemWithMatchingName(listId, newItem);

        //if there's still no match, just insert
        if(null == existingItemWithSameName){
            try {
                //calling "get()" forces the insert to have completed before this function completes.
                //This helps in cases where multiple ingredients are added in a row, preventing duplicates
                //merging being a race condition with the database's inserting happening slower than its
                //next "findIngListItemWithSameName" call.
                newItem.setListId(listId);
                slaRepository.insertIngListItem(newItem).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        //if one was found, merge their quantities then persist the change
        else{
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateOrDeleteIfEmptyIngListItem(existingItemWithSameName);
        }
    }

    /**
     * Attempts to find an IngListItem in the database other than this one with a matching name
     * and checked flag value. Also checks for plurals, e.g. potato matches potatoes,
     * by attempting to match name without trailing "s" or "es", or by adding those suffixes,
     * if the original name is not found.
     * @param listId the id of the list to search within.
     * @param newItem the item to find a match for.
     * @return an existing item within the list with the same or the de/pluralised form, or null
     * if no match.
     */
    private IngListItem findItemWithMatchingName(long listId, IngListItem newItem){
        //attempt to find an existing item with the same name
        IngListItem existingItemWithSameName =
                slaRepository.findIngListItemWithSameName(listId, newItem.getId(), newItem.getName(), newItem.isChecked());

        //if none found, try some variations of plura/non plural for the name
        if(null == existingItemWithSameName){
            String name = newItem.getName();
            //try accounting for plural variation in name
            //remove "s" from the end if applicable
            if(name.endsWith("s")){
                existingItemWithSameName =
                        slaRepository.findIngListItemWithSameName(listId, newItem.getId(),
                                name.substring(0, newItem.getName().length() - 1),
                                newItem.isChecked());
                //if still no match, try removing an "es" suffix if applicable
                if(null == existingItemWithSameName){
                    if (name.endsWith("es")){
                        existingItemWithSameName =
                                slaRepository.findIngListItemWithSameName(listId, newItem.getId(),
                                        name.substring(0, newItem.getName().length() - 2),
                                        newItem.isChecked());
                    }
                }
                //if we found a matching item by de-pluralising the name, convert its name to the
                //plural form since we're about to merge newItem's qty into it and newItem is already
                //plural
                if (null != existingItemWithSameName){
                    existingItemWithSameName.setName(newItem.getName());
                }
            }
            //otherwise try turning the name into a plural
            else{
                existingItemWithSameName =
                        slaRepository.findIngListItemWithSameName(listId, newItem.getId(),
                                name + "s",
                                newItem.isChecked());

                //try adding an "es" to the end if that still hasn't worked
                if(null == existingItemWithSameName){
                    existingItemWithSameName =
                            slaRepository.findIngListItemWithSameName(listId, newItem.getId(),
                                    name + "es",
                                    newItem.isChecked());
                }
            }
        }
        return existingItemWithSameName;
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
        IngListItem existingItemWithSameName = findItemWithMatchingName(oldItem.getListId(), newItem);
        if (null != existingItemWithSameName){
            slaRepository.deleteIngListItem(oldItem);
            IngListItemUtils.mergeQuantities(existingItemWithSameName, newItem);
            slaRepository.updateOrDeleteIfEmptyIngListItem(existingItemWithSameName);
        }

        //if it wasn't changed to an existing item, simply update the original item in the db
        else{
            //copy identifying values over from oldItem to new item, then update it in the db
            //to overwrite all other values of oldItem
            newItem.setId(oldItem.getId());
            newItem.setListId(oldItem.getListId());
            newItem.setChecked(oldItem.isChecked());
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