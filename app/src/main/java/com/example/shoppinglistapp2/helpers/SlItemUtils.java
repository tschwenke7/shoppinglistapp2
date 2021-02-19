package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.SlItem;

public class SlItemUtils {

    public static SlItem toSlItem(String text){
        //delegate to IngredientUtils to interpret the user input as an ingredient.
        //Ingredient is quite similar to SlItem, so this does most of the work for us.
        Ingredient ingredient = IngredientUtils.toIngredient(text);
        return toSlItem(ingredient);
    }

    public static SlItem toSlItem(Ingredient ingredient){
        SlItem slItem = new SlItem();

        //copy name
        slItem.setName(ingredient.getName());

        //copy quantity/unit into first slot
        slItem.setQty1(ingredient.getQty());

        //if this is "Generic unit" (e.g. "3" potatoes) then put "na" as unit
        if(null == ingredient.getUnit()){
            slItem.setUnit1("na");
        }
        else {
            slItem.setUnit1(ingredient.getUnit());
        }

        //checked will always start as false
        slItem.setChecked(false);

        return slItem;
    }
}
