package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.SlItem;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class SlItemUtils {
    private static DecimalFormat df = new DecimalFormat("#.##");

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

    /**
     * Merges the quantities of two of the same type of shopping list item.
     * newItem should not have anything set for qty2/unit2.
     * Compares the units of each item, and merges newItem's qty with the matching unit/qty in
     * originalItem.
     * If they have different names, then it simply returns item 1.
     * @param originalItem - the existing list item to be merged into.
     *                     May possibly have both qty1 and qty2 set.
     * @param newItem - the new item having only unit1/qty1 set, which is to be merged into originalItem.
     */
    public static void mergeQuantities(SlItem originalItem, SlItem newItem){
        //don't do anything if these ingredients are not in fact the same
        if(originalItem.equals(newItem)){
            //if unit matches original's unit 1, simply combine these quantities
            if(newItem.getUnit1().equals(originalItem.getUnit1())){
                double newQty = (IngredientUtils.qtyAsDouble(originalItem.getQty1())
                        + IngredientUtils.qtyAsDouble(newItem.getQty1()));
                originalItem.setQty1(df.format(newQty));
            }
            //if unit matches original's unit 2, simply combine these quantities
            else if(originalItem.getUnit2() != null
                    && newItem.getUnit1().equals(originalItem.getUnit2())){
                double newQty = (IngredientUtils.qtyAsDouble(originalItem.getQty2())
                        + IngredientUtils.qtyAsDouble(newItem.getQty1()));
                originalItem.setQty2(df.format(newQty));
            }

            //otherwise, we will have to do some conversion
            else{
                double newAmount = IngredientUtils.qtyAsDouble(newItem.getQty1());
                /* Every unit is either a weight unit, a volume unit, or "na" (whole items) */

                //if the new item has unit "na" but didn't match either unit of original,
                //then simply copy the new qty/unit into original's slot 2 as slot 2 hasn't been used yet.
                if(newItem.getUnit1().equals("na")){
                    originalItem.setUnit2(newItem.getUnit1());
                    originalItem.setQty2(newItem.getQty1());
                }

                //alternatively, if new qty is a weight, convert everything to g then combine
                else if (unitIsWeight(newItem.getUnit1())) {
                    //if slot 1 of original contains a weight unit
                    if(unitIsWeight(originalItem.getUnit1())){
                        double originalAmount = IngredientUtils.qtyAsDouble(originalItem.getQty1());
                        //convert to g if qty is in kg
                        if (originalItem.getUnit1().equals("kg")) {
                            originalAmount *= 1000;
                        }

                        //convert to g if qty is in kg
                        if (newItem.getUnit1().equals("kg")) {
                            newAmount *= 1000;
                        }

                        //convert to kg if over 1000g, and save the new combined value
                        double combinedAmount = originalAmount + newAmount;
                        if(combinedAmount >= 1000) {
                            originalItem.setUnit1("kg");
                            originalItem.setQty1(df.format(combinedAmount/1000));
                        }
                        else{
                            originalItem.setUnit1("g");
                            originalItem.setQty1(df.format(combinedAmount));
                        }
                    }
                    //otherwise if slot 2 is empty, put the new amount in there
                    else if (null == originalItem.getUnit2()){
                        originalItem.setUnit2(newItem.getUnit1());
                        originalItem.setQty2(newItem.getQty1());
                    }

                    //otherwise merge with slot 2, which must be a weight type
                    else{
                        double originalAmount = IngredientUtils.qtyAsDouble(originalItem.getQty2());
                        //convert to g if qty is in kg
                        if (originalItem.getUnit2().equals("kg")) {
                            originalAmount *= 1000;
                        }
                        //convert to g if qty is in kg
                        if (newItem.getUnit1().equals("kg")) {
                            newAmount *= 1000;
                        }

                        //convert to kg if over 1000g, and save the new combined value
                        double combinedAmount = originalAmount + newAmount;
                        if(combinedAmount >= 1000) {
                            originalItem.setUnit2("kg");
                            originalItem.setQty2(df.format(combinedAmount/1000));
                        }
                        else{
                            originalItem.setUnit2("g");
                            originalItem.setQty2(df.format(combinedAmount));
                        }
                    }
                }

                //otherwise newUnit must be a volume unit.
                //for volume measurements, convert everything to mL then combine
                else{
                    //if first slot is the volume unit, merge here
                    if(unitIsVolume(originalItem.getUnit1())){
                        double originalAmount = IngredientUtils.qtyAsDouble(originalItem.getQty1());
                        //convert original amount to mL
                        originalAmount = convertToMl(originalAmount, originalItem.getUnit1());
                        //convert new amount to mL
                        newAmount = convertToMl(newAmount, newItem.getUnit1());

                        double combinedAmount = originalAmount + newAmount;
                        //convert to L if over 1000ml
                        if(combinedAmount >= 1000){
                            originalItem.setUnit1("L");
                            originalItem.setQty1(df.format(combinedAmount/1000));
                        }
                        else{
                            originalItem.setUnit1("mL");
                            originalItem.setQty1(df.format(combinedAmount));
                        }
                    }
                    //otherwise if second slot is empty, simply copy new values in
                    else if (null == originalItem.getUnit2()){
                        originalItem.setUnit2(newItem.getUnit1());
                        originalItem.setQty2(newItem.getQty1());
                    }

                    //otherwise merge with second slot
                    else{
                        double originalAmount = IngredientUtils.qtyAsDouble(originalItem.getQty2());
                        //convert original amount to mL
                        originalAmount = convertToMl(originalAmount, originalItem.getUnit2());
                        //convert new amount to mL
                        newAmount = convertToMl(newAmount, newItem.getUnit1());

                        double combinedAmount = originalAmount + newAmount;
                        //convert to L if over 1000ml
                        if(combinedAmount >= 1000){
                            originalItem.setUnit2("L");
                            originalItem.setQty2(df.format(combinedAmount/1000));
                        }
                        else{
                            originalItem.setUnit2("mL");
                            originalItem.setQty2(df.format(combinedAmount));
                        }
                    }
                }
            }
        }
    }

    private static boolean unitIsWeight(String unit){
        List<String> weightUnits = Arrays.asList("g", "kg");
        return weightUnits.contains(unit);
    }

    private static boolean unitIsVolume(String unit){
        List<String> weightUnits = Arrays.asList("mL", "L", "cups", "cup", "tbsp", "tsp");
        return weightUnits.contains(unit);
    }

    private static double convertToMl(double qty, String unit){
        switch (unit){
            case "L":
                return qty * 1000;
            case "cups":
            case "cup":
                return qty * 250;
            case "tbsp":
                return qty * 15;
            case "tsp":
                return qty * 5;
            default:
                return qty;
        }
    }
}
