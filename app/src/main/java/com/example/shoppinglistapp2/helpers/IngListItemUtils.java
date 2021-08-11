package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.IngListItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public class IngListItemUtils {
    private static DecimalFormat twodp = new DecimalFormat("#.##");
    private static DecimalFormat zerodp = new DecimalFormat("#");

    private static final List<String> unitsOfMeasurement = Arrays.asList(
            "cup",
//            "cups",

            "kg",
//            "kilograms",
            "kilogram",

            "g",
            "gram",
//            "grams",

            "ml",
//            "millilitres",
            "millilitre",
//            "milliliters",
            "milliliter",

            "l",
            "litre",
//            "litres",
            "liter",
//            "liters",

            "tsp",
//            "teaspoons",
            "teaspoon",

            "tbsp",
            "tablespoon",
//            "tablespoons",

            //foreign units
            "lb",
//            "lbs",
            "pound",
//            "pounds",

            "oz",
            "ounce"
//            "ounces"
    );

    public static final List<String> foreignUnitsOfMeasurement = Arrays.asList(
            "lb",
//            "lbs",
            "pound",
//            "pounds",
            "oz",
            "ounce"//,
//            "ounces"
    );

    /** Ingredients which really don't need to be on a shopping list ever. */
    public static final List<String> pointlessIngredients = Arrays.asList(
            "water",
            "hot water"
    );

    private static final List<String> wholeItemUnits = Arrays.asList(
            "x",
            "unit"
    );

    private static final List<String> massUnits = Arrays.asList(
            "lb",
            "pound",
            "oz",
            "ounce",
            "kg",
            "kilogram",
            "g",
            "gram"
//            "grams",
//            "kilograms",
//            "pounds",
//            "lbs",

    );

    private static final List<String> volumeUnits = Arrays.asList(
            "cup",
            "ml",
            "millilitre",
            "milliliter",
            "l",
            "litre",
            "liter",
            "tsp",
            "teaspoon",
            "tbsp",
            "tablespoon"
//            "tablespoons",
//            "teaspoons",
//            "liters",
//            "litres",
//            "milliliters",
//            "millilitres",
//            "cups",
    );

    private static enum UnitType {
        WHOLE_ITEM,
        MASS,
        VOLUME,
        OTHER
    }

    public static UnitType getUnitType(String unit) {
        unit = unit.toLowerCase();
        //remove trailing "s" for plural units
        if (unit.endsWith("s")) {
            unit = unit.substring(0,unit.length()-1);
        }

        if (wholeItemUnits.contains(unit)){
            return UnitType.WHOLE_ITEM;
        }
        if(massUnits.contains(unit)){
            return UnitType.MASS;
        }
        if(volumeUnits.contains(unit)){
            return UnitType.VOLUME;
        }
        return UnitType.OTHER;
    }

    public static IngListItem toIngListItem(String ingText) {
        IngListItem ingListItem = new IngListItem();
        StringBuilder name = new StringBuilder();
        StringBuilder amount = new StringBuilder();

        //expand any single-character fractions into <digit>/<digit>
        ingText = Normalizer.normalize(ingText, Normalizer.Form.NFKD);
        ingText = ingText.replaceAll("\u2044","/");

        //normalise case
        ingText = ingText.toLowerCase().trim();

        /* format of an ingListItem as text is zero or more "amount" pairs of qty and units (or no unit if
        whole items) separated by "+" characters, followed by the name of the item.
        [<qty>] [+ <qty> <unit>] [+ <qty> <unit>] <name>
         */

        //first, split multiple quantities if present by splitting on any "+" characters
        String[] amounts = ingText.split(" \\+");
        //the name of the item will be in the last "amount",
        //so lets first analyse any guaranteed "amount-only" components
        for (int i = 0; i < amounts.length - 2; i++){
            addAmount(ingListItem, amounts[i]);
        }

        /*now we need to break down the last component, which contains the name and potentially an amount */
        String[] words = amounts[amounts.length-1].split(" ");

        //if first word is "[num]x", interpret this as qty [num]
        if (words[0].matches("\\d+x$")){
            addAmount(ingListItem, words[0].substring(0,words[0].length()-1));

            //read the rest of ingText as the name of ingredient
            for (int i = 1; i < words.length; i++){
                name.append(words[i]).append(" ");
            }
        }

        //else if final word is "x[num]" interpret this as qty [num]
        else if (words[words.length-1].matches("^x\\d+")) {
            //extract the number part as whole unit quantity
            ingListItem.setWholeItemQty(Double.parseDouble(words[words.length-1].substring(1)));

            //read the rest of ingText as the name of ingredient
            for (int i = 0; i < words.length-1; i++){
                name.append(words[i]).append(" ");
            }
        }

        //else if the first word contains a number, an amount has been given and we need to separate
        //it out from the name of the item.
        // Check each subsequent word for either more of qty number or a unit of measurement
        else if (words[0].matches(".*\\d.*")) {
            //keep reading as amount while the word either contains numbers
            //or is a "unit of measurement" word
            int i = 0;
            do {
                amount.append(words[i]).append(" ");
                i++;
            } while (words[i].matches(".*\\d.*") || unitsOfMeasurement.contains(words[i]));

            //read remainder of words as ingredient name
            for(int j = i; j < words.length; j++){
                name.append(words[j]).append(" ");
            }

            //add the amount recorded to this ingredient
            String amountString = amount.toString().trim();
            if(!amountString.isEmpty()){
                addAmount(ingListItem, amountString);
            }
        }

        //else just assume the quantity is 1 if there are no numbers
        else{
            ingListItem.setWholeItemQty(1);

            //read all of ingText as the name of ingredient
            for (int i = 0; i < words.length; i++){
                name.append(words[i]).append(" ");
            }
        }

        ingListItem.setName(name.toString().trim());
        return ingListItem;
    }

    /**
     * Takes a string representation of an ingredient amount, converts it to qty/unit and sets
     * the relevant properties of IngListItem accordingly.
     * @param ingListItem - the ingListItem to add this amount to
     * @param amount the "amount" string saying the amount to be set. Expected formats include:
     *               <number>[ ][unit]
     *               Where <number> = integer, decimal, fraction (in format integer/integer)
     *               or mixed numeral (in format <integer>_<integer>/<integer>)
     */
    private static void addAmount(IngListItem ingListItem, String amount) {
        /* Separate the qty and unit (if applicable) from the amount String */

        //find last occurrence of a digit to split qty/unit at
        int splitIndex = -1;
        for (int i = 0; i < amount.length(); i++){
            if (Character.isDigit(amount.charAt(i))){
                splitIndex = i;
            }
        }

        String qtyStr = (amount.substring(0, splitIndex + 1) + " ").trim();
        String unit = amount.substring(splitIndex+1).trim().toLowerCase();



        //convert the amount string into a "double" amount, handling fractions/mixed numerals
        double qty = qtyAsDouble(qtyStr);

        //determine which type of amount this is by checking the unit
        switch (getUnitType(unit)){
            case WHOLE_ITEM:
                ingListItem.setWholeItemQty(qty);
                break;
            case VOLUME:
                ingListItem.setVolumeQty(qty);
                ingListItem.setVolumeUnit(unit);
                break;
            case MASS:
                ingListItem.setMassQty(qty);
                ingListItem.setMassUnit(unit);
        }
    }

    /**
     * Converts a qty string into a double value. This can handle fractions or decimals, but always
     * outputs to a double.
     * @param qty - the qty string
     * @return the double value equivalent
     */
    public static double qtyAsDouble(String qty){
        //if there's no fraction bar, simply parse as a double
        if(!qty.contains("/")){
            return Double.parseDouble(qty);
        }

        double result = 0;
        BigDecimal numerator;
        BigDecimal denominator;

        //otherwise, split either side of fraction bar
        String[] components = qty.split("/");
        //denominator should always be second part
        denominator = BigDecimal.valueOf(Double.parseDouble(components[1].trim()));

        //check for mixed numerals
        components[0] = components[0].trim();
        String[] wholeNumAndNumerator = components[0].split(" ");

        //if it is a mixed numeral
        if(wholeNumAndNumerator.length == 2){
            //add whole number part just as number
            result = Double.parseDouble(wholeNumAndNumerator[0].trim());
            //record numerator
            numerator = new BigDecimal(wholeNumAndNumerator[1].trim());
        }
        //if just a standard fraction, the first component is the numerator
        else{
            numerator = new BigDecimal(components[0]);
        }

        //compute the value of the fraction, and add/subtract to whole number part if applicable
        //subtract if the whole number part was negative, otherwise add
        if (result < 0){
            result -= (numerator.divide(denominator, 2, RoundingMode.HALF_UP)).doubleValue();
        }
        else{
            result += (numerator.divide(denominator, 2, RoundingMode.HALF_UP)).doubleValue();
        }

        return result;
    }

    /**
     * Converts any units written in different ways to one normalised form for consistency,
     * simplifying adding quantities later. e.g. "teaspoon," "teaspoons" or "tsp" all become "tsp".
     * Also converts any imperial units into equivalent metric units and adjusts qty accordingly
     * @param ingredient the Ingredient to be normalised
     */
    private static void normaliseUnits(IngListItem ingredient){
        //Normalise volume unit */
        String volumeUnit = ingredient.getVolumeUnit().toLowerCase();

        switch (volumeUnit) {
            //L
            case "l":
            case "ls":
            case "liter":
            case "liters":
            case "litre":
            case "litres":
                ingredient.setVolumeUnit("L");
                break;

            //mL
            case "ml":
            case "milliliter":
            case "milliliters":
            case "millilitre":
            case "millilitres":
            case "mls":
                ingredient.setVolumeUnit("mL");
                break;

            //tsp
            case "teaspoon":
            case "teaspoons":
            case "tsps":
                ingredient.setVolumeUnit("tsp");
                break;

            //tbsp
            case "tablespoon":
            case "tablespoons":
            case "tbsps":
                ingredient.setVolumeUnit("tbsp");
                break;

            //cups
            case "cup":
                ingredient.setVolumeUnit("cups");
                break;
        }

        /* Normalise mass unit */
        String massUnit = ingredient.getMassUnit().toLowerCase();
        double grams;

        switch (massUnit) {
            //g
            case "gram":
            case "grams":
                ingredient.setMassUnit("g");
                break;

            //kg
            case "kilogram":
            case "kilograms":
            case "kgs":
                ingredient.setMassUnit("kg");
                break;

            /* convert any imperial units to an appropriate metric one */
            //convert pounds to g/kg
            case "lb":
            case "lbs":
            case "pound":
            case "pounds":
                //~454.592 grams per lb
                grams = ingredient.getMassQty() * 453.592;
                //write as kg if >= 1000g
                if (grams >= 1000) {
                    //round to two dp
                    ingredient.setMassQty(Double.parseDouble(twodp.format(grams / 1000)));
                    ingredient.setMassUnit("kg");
                } else {
                    ingredient.setMassQty(Double.parseDouble(zerodp.format(grams)));
                    ingredient.setMassUnit("g");
                }

                break;
            case "oz":
            case "ounce":
            case "ounces":
                //~28.3495g per oz.
                grams = ingredient.getMassQty() * 28.3495;
                if (grams >= 1000) {
                    ingredient.setMassQty(Double.parseDouble(twodp.format(grams / 1000)));
                    ingredient.setMassUnit("kg");
                } else {
                    ingredient.setMassQty(Double.parseDouble(zerodp.format(grams)));
                    ingredient.setMassUnit("g");
                }
                break;
        }
    }
}
