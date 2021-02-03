package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.db.tables.Ingredient;

import java.util.Arrays;
import java.util.List;

public class IngredientUtil {

    private static final List<String> unitsOfMeasurement = Arrays.asList(
            "cup",
            "cups",
            "kg",
            "g",
            "ml",
            "l",
            "tsp",
            "tbsp"
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
                    unit = words[i].substring(splitIndex);
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

        //capitalise Litres
        if ("l".equals(unit)){
            unit = "L";
        }

        return new Ingredient(qty.trim(),unit,name.trim());
    }
}
