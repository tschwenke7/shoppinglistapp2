package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.favourites;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.IngList;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.example.shoppinglistapp2.helpers.InvalidIngredientStringException;

import java.util.List;

public class FavouritesViewModel extends AndroidViewModel {
    private final LiveData<List<IngListItem>> favourites;
    private final SlaRepository slaRepository;

    public FavouritesViewModel(@NonNull Application application) {
        super(application);
        slaRepository = new SlaRepository(application);
        favourites = slaRepository.getFavouritesItems();
    }

    public LiveData<List<IngListItem>> getFavourites() {
        return favourites;
    }

    public void addItems(String inputText) throws InvalidIngredientStringException {
        //split input in case of multiple lines
        String[] items = inputText.split("(\\r\\n|\\r|\\n)");

        //convert each line to an item
        //and either add it or merge it with an existing item of same name
        for (String item : items){
            slaRepository.insertOrMergeItem(IngListItemUtils.FAVOURITES_LIST_ID, IngListItemUtils.toIngListItem(item.trim()));
        }

    }

    public void editItem(IngListItem oldItem, String newItemString) throws InvalidIngredientStringException {
        slaRepository.editItem(oldItem, newItemString);
    }

    public void deleteItem(IngListItem item) {
        slaRepository.deleteIngListItem(item);
    }

    public void sendToShoppingList(IngListItem item) {
        //copy item and add it to shopping list
        IngListItem addItem = item.deepCopy();
        slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, addItem);

        //mark off item in favourites list as added
        item.setChecked(true);
        slaRepository.updateIngListItem(item);
    }

    public void addAllUnchecked() {
        for (IngListItem item : favourites.getValue()) {
            if (!item.isChecked()) {
                slaRepository.insertOrMergeItem(IngListItemUtils.SHOPPING_LIST_ID, item.deepCopy());
            }
        }
    }
}
