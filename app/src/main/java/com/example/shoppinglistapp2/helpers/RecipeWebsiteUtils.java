package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.recipes.creator.InvalidRecipeUrlExeception;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;

import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecipeWebsiteUtils {
    private static List<String> supportedWebsites = Arrays.asList(
            "recipetineats.com"
    );

    /**
     * Enum of supported domains for recipe website conversion
     */
    public static enum Domain {
        RECIPE_TIN_EATS,
        NOT_SUPPORTED
    }

    /**
     * Gets the domain of the recipe url provided so we can use the appropriate converter
     * @param url
     * @return
     */
    public static Domain getDomain(String url){
        //Check if Recipe Tin Eats
        if(url.contains("recipetineats.com")){
            return Domain.RECIPE_TIN_EATS;
        }
        //otherwise indicate that this domain is not supported
        else{
            return Domain.NOT_SUPPORTED;
        }
    }

    public static boolean validateUrl(String url) throws InvalidRecipeUrlExeception{
        //check that something was provided
        if (url.isEmpty()) {
            throw new InvalidRecipeUrlExeception(App.getRes().getString(R.string.recipe_url_empty));
        }
        //check that it is a valid url
        UrlValidator urlValidator = new UrlValidator();
        if (!urlValidator.isValid(url)){
            throw new InvalidRecipeUrlExeception(App.getRes().getString(R.string.recipe_url_invalid));
        }
        //check that it's one of the currently supported URLs
        if (getDomain(url) == Domain.NOT_SUPPORTED){
            throw new InvalidRecipeUrlExeception(App.getRes().getString(R.string.recipe_url_unsupported));
        }

        return true;
    }

    /**
     * Visits the given url and scrapes the page to extract a Recipe object.
     * If the website is not valid or supported, or a Recipe otherwise cannot be read from it,
     * returns null.
     * @param url - the website for a recipe
     * @return - A Recipe object populated with the website's data.
     */
    public static Recipe getRecipeFromWebsite(String url) throws InvalidRecipeUrlExeception {
        //validate url before attempting to convert
        //throws InvalidRecipeUrlException if not valid
        validateUrl(url);

        //use appropriate prefiller for url's domain
        switch (getDomain(url)){
            case RECIPE_TIN_EATS:
                return convertRecipeTinEats(url);
            default:
                return null;
        }
    }

    private static Recipe convertRecipeTinEats(String url){
        try {
            //open webpage
            Document doc = Jsoup.connect(url).get();

            Recipe recipe = new Recipe();

            /* get url */
            recipe.setUrl(url);

            /* get recipe name - from webpage title, minus everything after the "|" */
            recipe.setName(doc.title().split("\\|",2)[0].trim());

            /* get prep time */
            int prepTime = 0;

            //add hours if present
            Elements prepTimeHours = doc.getElementsByClass("wprm-recipe-prep_time-hours");
            if(prepTimeHours.size() > 0){
                prepTime += 60 * Integer.parseInt(prepTimeHours.get(0).text());
            }
            //add minutes if present
            Elements prepTimeMinutes = doc.getElementsByClass("wprm-recipe-prep_time-minutes");
            if(prepTimeMinutes.size() > 0){
                prepTime += Integer.parseInt(prepTimeMinutes.get(0).text());
            }

            /* get cook time */
            int cookTime = 0;

            recipe.setPrepTime(prepTime);
            //add hours if present
            Elements cookTimeHours = doc.getElementsByClass("wprm-recipe-cook_time-hours");
            if(cookTimeHours.size() > 0){
                cookTime += 60 * Integer.parseInt(cookTimeHours.get(0).text());
            }
            //add minutes if present
            Elements cookTimeMinutes = doc.getElementsByClass("wprm-recipe-cook_time-minutes");
            if(cookTimeMinutes.size() > 0){
                cookTime += Integer.parseInt(cookTimeMinutes.get(0).text());
            }

            recipe.setCookTime(cookTime);

            /* Get number of serves */
            Elements serves = doc.getElementsByClass("wprm-recipe-servings");
            if(serves.size() > 0){
                try{
                    recipe.setServes(Integer.parseInt(serves.get(0).text()));
                }
                catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }

            /* Read "course" and "cuisine" as tags if present */
            List<String> tags = new ArrayList<>();

            Elements course = doc.getElementsByClass("wprm-recipe-course");
            if(course.size() > 0){
                String[] courses = course.text().split(",");
                for(int i = 0; i < courses.length; i++){
                    tags.add(courses[i].trim());
                }
            }

            Elements cuisine = doc.getElementsByClass("wprm-recipe-cuisine");
            if(cuisine.size() > 0){
                String[] cuisines = cuisine.text().split(",");
                for(int i = 0; i < cuisines.length; i++){
                    tags.add(cuisines[i].trim());
                }
            }

            recipe.setTags(tags);

            /* get ingredients */
            ArrayList<Ingredient> ingredients = new ArrayList<>();
            Elements ingredientsRows = doc.getElementsByClass("wprm-recipe-ingredient");

            for(Element ingredientRow: ingredientsRows){
                //read ingredient amount
                //sometimes the unit span is not present, and instead it's combined with qty
                //therefore, we will just combine them and let IngredientsUtil separate the units
                String ingAmount = ingredientRow.getElementsByClass("wprm-recipe-ingredient-amount").text().trim()
                        + ingredientRow.getElementsByClass("wprm-recipe-ingredient-unit").text().trim();

                String ingName = ingredientRow.getElementsByClass("wprm-recipe-ingredient-name").text().trim();


                //if there's two amount/units given, we need to separate them and only keep one (preferably metric)
                //sometimes the second amount follows in brackets
                if(ingAmount.contains("(")){
                    //split the amount on the bracket
                    String[] ingAmountComponents = ingAmount.split("\\(");
                    ingAmount = ingAmountComponents[0].trim();

                    //check if the first part uses an imperial measurement
                    boolean firstGood = true;
                    for(String unit : IngredientUtils.foreignUnitsOfMeasurement){
                        if(ingAmountComponents[0].contains(unit)){
                            //if it does, we will take the second part instead.
                            ingAmount = ingAmountComponents[1].trim();
                            ingAmount.replace(")", "");//get rid of the close bracket
                        }
                    }
                }

                //sometimes the alternative amounts are separated instead by a "/"
                else if(ingAmount.contains("/")){
                    //if the characters either side of the "/" are digits, then this is a fraction bar instead
                    if(Character.isDigit(ingAmount.charAt(ingAmount.indexOf("/") + 1))
                            && Character.isDigit(ingAmount.charAt(ingAmount.indexOf("/") - 1))){
                        //check further in string for potential second "/" which IS a separator
                        if(ingAmount.indexOf("/", ingAmount.indexOf("/") + 1) != -1) {
                            //split string on second "/"
                            int splitIndex = ingAmount.indexOf("/", ingAmount.indexOf("/") + 1);
                            String[] ingAmountComponents = new String[]{
                                    ingAmount.substring(0, splitIndex),
                                    ingAmount.substring(splitIndex + 1)
                            };

                            //take the alternative which doesn't use imperial measurement
                            ingAmount = ingAmountComponents[0].trim();
                            for(String unit : IngredientUtils.foreignUnitsOfMeasurement){
                                if(ingAmountComponents[0].contains(unit)){
                                    ingAmount = ingAmountComponents[1].trim();
                                }
                            }
                        }
                    }
                    //otherwise simply split the amount either side of the first "/"
                    else{
                        int splitIndex = ingAmount.indexOf("/");
                        String[] ingAmountComponents = new String[]{
                                ingAmount.substring(0, splitIndex),
                                ingAmount.substring(splitIndex + 1)
                        };
                        //take the part which doesn't use imperial measurements
                        ingAmount = ingAmountComponents[0].trim();
                        for(String unit : IngredientUtils.foreignUnitsOfMeasurement){
                            if(ingAmountComponents[0].contains(unit)){
                                ingAmount = ingAmountComponents[1].trim();
                            }
                        }
                    }
                }

                //sometimes, the name of the ingredient starts with "EACH" and then actually lists
                //multiple ingredients. In this case, create a separate ingredient for each w/ same amount
                if(ingName.contains("each")){
                    ingName = ingName.replaceFirst("each", "");//remove the "each"
                    String[] names = ingName.split("and");//split before and after "and"
                    String[] names2 = names[0].split(",");//split on each comma before and
                    //add each separated name to a list
                    List<String> namesList = new ArrayList<>();
                    namesList.addAll(Arrays.asList(names2));
                    namesList.add(names[1]);

                    //add an ingredient for name (amount stays the same for each)
                    for (String name: namesList){
                        //concatenate the resulting amount/name and delegate to IngredientsUtil to convert
                        //it to an actual Ingredient object
                        String ingText = String.format("%s %s", ingAmount, name);
                        ingredients.add(IngredientUtils.toIngredient(ingText));
                    }
                }
                //otherwise just add the ingredient
                else{
                    //concatenate the resulting amount/name and delegate to IngredientsUtil to convert
                    //it to an actual Ingredient object
                    String ingText = String.format("%s %s", ingAmount, ingName);
                    ingredients.add(IngredientUtils.toIngredient(ingText));
                }
            }

            recipe.setIngredients(ingredients);

            return recipe;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
