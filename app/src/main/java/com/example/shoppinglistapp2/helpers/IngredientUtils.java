package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.Ingredient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class IngredientUtils {

    private static final List<String> unitsOfMeasurement = Arrays.asList(
            "cup",
            "cups",

            "kg",
            "kilograms",
            "kilogram",

            "g",
            "gram",
            "grams",

            "ml",
            "millilitres",
            "millilitre",
            "milliliters",
            "milliliter",

            "l",
            "litre",
            "litres",
            "liter",
            "liters",

            "tsp",
            "teaspoons",
            "teaspoon",

            "tbsp",
            "tablespoon",
            "tablespoons"
    );

    public static final List<String> foreignUnitsOfMeasurement = Arrays.asList(
            "lb",
            "lbs",
            "pound",
            "pounds",
            "oz",
            "ounce",
            "ounces"
    );

    public static Ingredient toIngredient(String ingText){
        //the return will be two strings, the first being qty and the second the ingredient name
        String qty = "";
        String unit = null;
        String name = "";


        //normalise case, then split into individual words
        ingText = ingText.toLowerCase().trim();
        String[] words = ingText.split(" ");

        //if first word is "[num]x", interpret this as qty [num]
        if (words[0].matches("\\d+x$")){
            qty = words[0].substring(0,words[0].length()-1);

            //read the rest of ingText as the name of ingredient
            for (int i = 1; i < words.length; i++){
                name += words[i] + " ";
            }
        }
        //else if final word is "x[num]" interpret this as qty [num]
        else if (words[words.length-1].matches("^x\\d+")) {
            qty = words[words.length-1].substring(1);

            //read the rest of ingText as the name of ingredient
            for (int i = 0; i < words.length-1; i++){
                name += words[i] + " ";
            }
        }

        //else if the first word contains a number, read as qty
        // then check each subsequent line for either more of qty and/or a unit of measurement
        else if (words[0].matches(".*\\d.*")) {
            //keep reading as qty while the word either contains numbers or is a unit of measurement
            // or is a "unit of measurement" word
            int i = 0;
            do {
                //if the word is just a unit, record it as such
                if(unitsOfMeasurement.contains(words[i])){
                    unit = words[i];
                }

                //if word contains letters, separate out the unit from the qty
                else if (words[i].matches(".*[a-z].*")){
                    //find last occurrence of a digit to split qty/unit at
                    int splitIndex = -1;
                    for (int c = 0; c < words[i].length(); c++){
                        if (Character.isDigit(words[i].charAt(c))){
                            splitIndex = c;
                        }
                    }

                    qty += words[i].substring(0,splitIndex+1) + " ";
                    unit = words[i].substring(splitIndex+1);
                }

                //if it doesn't contain letters, assume it was solely a component of qty
                else{
                    qty += words[i] + " ";
                }

                i++;
            } while (words[i].matches(".*\\d.*") || unitsOfMeasurement.contains(words[i]));

            //read remainder of words as ingredient name
            for(int j = i; j < words.length; j++){
                name += words[j] + " ";
            }
        }

        //else just assume the quantity is 1
        else{
            qty = "1";

            //read all of ingText as the name of ingredient
            for (int i = 0; i < words.length; i++){
                name += words[i] + " ";
            }
        }

        //normalise the units to make them easier to combine quantities later
        Ingredient ingredient = new Ingredient(name.trim(),qty.trim(),unit);
        if(null != unit) {
            normaliseUnits(ingredient);
        }

        return ingredient;
    }

    /**
     * Converts any units written in different ways to one normalised form for consistency,
     * simplifying adding quantities later. e.g. "teaspoon," "teaspoons" or "tsp" all return "tsp".
     * Also converts any imperial units into equivalent metric units and adjusts qty accordingly
     * @param ingredient the Ingredient to be normalised
     * @return the standard way of writing that unit for this app
     */
    private static void normaliseUnits(Ingredient ingredient){
        String unit = ingredient.getUnit();
        //g
        if (
            "gram".equals(unit) ||
            "grams".equals(unit)
        ){ ingredient.setUnit("g"); }

        //kg
        else if (
            "kilogram".equals(unit) ||
            "kilograms".equals(unit)
        ){ ingredient.setUnit("kg"); }

        //L
        else if (
            "l".equals(unit) ||
            "liter".equals(unit) ||
            "liters".equals(unit) ||
            "litre".equals(unit) ||
            "litres".equals(unit)
        ){ ingredient.setUnit("L"); }

        //mL
        else if (
            "ml".equals(unit) ||
            "milliliter".equals(unit) ||
            "milliliters".equals(unit) ||
            "millilitre".equals(unit) ||
            "millilitres".equals(unit)
        ){ ingredient.setUnit("mL"); }

        //tsp
        else if (
            "teaspoon".equals(unit) ||
            "teaspoons".equals(unit)
        ){ ingredient.setUnit("tsp"); }
        //tbsp
        else if (
            "tablespoon".equals(unit) ||
            "tablespoon".equals(unit)
        ){ ingredient.setUnit("tbsp"); }
        else if (
            "cup".equals(unit)
        ){ ingredient.setUnit("cups"); }
        //todo - deal with imperial units here
//        else if (
//            "lb".equals(unit) ||
//            "lbs".equals(unit)
//        ){ ingredient.setUnit("kg"); }
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

        //compute the value of the fraction, and add to whole number part if applicable
        result += (numerator.divide(denominator)).doubleValue();

        return result;
    }

    public static boolean qtyHasFraction(String qty){
        return qty.contains("/");
    }
}
