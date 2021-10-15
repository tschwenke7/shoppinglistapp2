package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.mainContentFragments.recipes.creator.InvalidRecipeUrlExeception;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;

import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecipeWebsiteUtils {

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
        if (url.contains("hellofresh.com.au") || url.contains("hellofresh.co.nz")){
            return Domain.HELLO_FRESH;
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
    public static RecipeWithTagsAndIngredients getRecipeFromWebsite(String url) throws InvalidRecipeUrlExeception {
        //validate url before attempting to convert
        //throws InvalidRecipeUrlException if not valid
        validateUrl(url);

        //use appropriate prefiller for url's domain
        try {
            switch (getDomain(url)){
                case RECIPE_TIN_EATS:
                    return convertRecipeTinEats(url);
                case HELLO_FRESH:
                    return convertHelloFresh(url);
                default:
                    return null;
            }
        }
        //if the url was unable to be loaded, catch that here and return null
        catch (IOException ioException) {
            ioException.printStackTrace();
            return null;
        }
    }

    /**
     * Things to tell the user about:
     *  - If there is more than one quantity listed for an ingredient, the smaller one will be chosen.
     *
     * @param url
     * @return
     * @throws IOException
     */
    private static RecipeWithTagsAndIngredients convertRecipeTinEats(String url) throws IOException {
        //open webpage
        Document doc = Jsoup.connect(url).get();

        RecipeWithTagsAndIngredients populatedRecipe = new RecipeWithTagsAndIngredients();

        Recipe recipe = new Recipe();
        recipe.setServes(1);//default value for serves should be 1

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
        List<Tag> tags = new ArrayList<>();
        Elements courseElements = doc.getElementsByClass("wprm-recipe-course");
        if(courseElements.size() > 0){
            String[] courses = courseElements.text().split(",");
            for (String course : courses) {
                tags.add(new Tag(course.trim()));
            }
        }

        Elements cuisineElements = doc.getElementsByClass("wprm-recipe-cuisine");
        if(cuisineElements.size() > 0){
            String[] cuisines = cuisineElements.text().split(",");
            for (String cuisine : cuisines) {
                tags.add(new Tag(cuisine.trim()));
            }
        }
        tags.add(new Tag("RecipeTin Eats"));

        /* get ingredients */
        ArrayList<IngListItem> ingredients = new ArrayList<>();
        Elements ingredientsRows = doc.getElementsByClass("wprm-recipe-ingredient");

        for(Element ingredientRow: ingredientsRows){
            //read ingredient amount
            //sometimes the unit span is not present, and instead it's combined with qty
            //therefore, we will just combine them and let IngredientsUtil separate the units
            String ingAmount = ingredientRow.getElementsByClass("wprm-recipe-ingredient-amount").text().trim()
                    + " " //separate with a space
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
                for(String unit : IngListItemUtils.foreignUnitsOfMeasurement){
                    if(ingAmountComponents[0].contains(unit)){
                        //if it does, we will take the second part instead.
                        ingAmount = ingAmountComponents[1].trim();
                        ingAmount = ingAmount.replace(")", "");//get rid of the close bracket
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
                        for(String unit : IngListItemUtils.foreignUnitsOfMeasurement){
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
                    for(String unit : IngListItemUtils.foreignUnitsOfMeasurement){
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
                    //concatenate the resulting amount/name and delegate to IngListItemUtils to convert
                    //it to an actual Ingredient object
                    String ingText = String.format("%s %s", ingAmount, name);
                    //replace any instances of "+" symbol with "plus" to avoid breaking the toIngListItem
                    addIngredientToList(ingredients, ingText);
                }
            }
            //otherwise just add the ingredient
            else{
                //concatenate the resulting amount/name and delegate to IngredientsUtil to convert
                //it to an actual Ingredient object
                String ingText = String.format("%s %s", ingAmount, ingName);
                addIngredientToList(ingredients, ingText);
            }
        }

        //combine all components into populated recipe to return
        populatedRecipe.setRecipe(recipe);
        populatedRecipe.setTags(tags);
        populatedRecipe.setIngredients(ingredients);

        return populatedRecipe;
    }

    /**
     * Shared business logic for final checks before adding an ingredient to the recipe.
     * - Replaces "+" with "plus" to avoid confusing toIngListItem, as "+" usually separates
     * multiple incompatible qtys of an item.
     * - Doesn't add the ingredient if it's part of the "pointless ingredients" list
     * - If ingText cannot be cleanly converted to an IngListItem, appends [edit me] to the front.
     * This will convert successfully, but inform the user to edit the item themselves.
     * @param ingredients
     * @param ingText
     */
    private static void addIngredientToList(List<IngListItem> ingredients, String ingText) {
        //replace any instances of "+" symbol with "plus" to avoid breaking the toIngListItem
        ingText = ingText.replaceAll("\\+", "plus");

        try{
            IngListItem ingredient = IngListItemUtils.toIngListItem(ingText);
            if (!IngListItemUtils.pointlessIngredients.contains(ingredient.getName())) {
                ingredients.add(ingredient);
            }
        }
        //if the item was invalid, add a "[EDIT_ME] to the front so it will convert, but the user will
        //know to edit it.
        catch (Exception e){
            IngListItem ingredient = IngListItemUtils.toIngListItem("[edit me] " + ingText);
            ingredients.add(ingredient);
        }
    }

    private static RecipeWithTagsAndIngredients convertHelloFresh(String url) throws IOException {
        //open webpage
        Document doc = Jsoup.connect(url).get();

        RecipeWithTagsAndIngredients populatedRecipe = new RecipeWithTagsAndIngredients();

        Recipe recipe = new Recipe();
        recipe.setServes(1);//default value for serves should be 1

        /* get url */
        recipe.setUrl(url);

        /* get recipe name - from webpage title, minus everything after the "|" and the word "Recipe" */
        String title = doc.title().split("\\|",2)[0].trim();
        int lastIndex = title.lastIndexOf("Recipe");
        if(lastIndex != -1){
            title = title.substring(0,title.lastIndexOf("Recipe"));
        }

        recipe.setName(title);

        /* get ingredients */
        List<IngListItem> ingredients = new ArrayList<>();
        //get from main ingredient list
        Elements ingredientsElements = doc.selectFirst("div.fela-_g6xips").child(0).children();
        //get from "Not included in your delivery" section
        Elements extraIngredientsElements = doc.selectFirst("div.fela-_20uev4").child(0).children();
        ingredientsElements.addAll(extraIngredientsElements);

        for (Element ingElement : ingredientsElements) {
            Element ingDetails = ingElement.child(1);

            //remove redundant "unit" unit of measurement if present
            String amount = ingDetails.child(0).text().trim();
            amount = amount.replaceFirst(" unit","");

            addIngredientToList(ingredients, amount + " " + ingDetails.child(1).text().trim());
        }


        /* get tags */
        List<Tag> tags = new ArrayList<>();
        try {
            Element tagHeadingSpan = doc.selectFirst("span[data-translation-id=recipe-detail.tags]");
            Element tagsContainerSpan = selectNthElementAfter(tagHeadingSpan.parent(), "span", 1);
            for(String tagName: tagsContainerSpan.text().split("•")){
                tags.add(new Tag(tagName));
            }
        }
        catch (Exception e){
            //if there was no tags section, we'll just leave it at "HelloFresh"
        }
        tags.add(new Tag("HelloFresh"));

        /* get prep time */
        //prep time located by going to next span after prep time heading, identified by the attribute
        //'data-translation-id=recipe-detail.preparation-time'
        Element prepTimeHeading = doc.selectFirst("span[data-translation-id=recipe-detail.preparation-time]");
        String prepTimeString = selectNthElementAfter(prepTimeHeading.parent(), "span", 1).text();
        //read the first part as the number of minutes, separating it from the word "minutes"
        try{
            recipe.setPrepTime(Integer.parseInt(prepTimeString.split(" ")[0]));
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }

        /* Set servings */
        //currently it is impossible to change the number of serves by clicking a button using JSoup
        recipe.setServes(2);

        //combine all components into populated recipe to return
        populatedRecipe.setRecipe(recipe);
        populatedRecipe.setTags(tags);
        populatedRecipe.setIngredients(ingredients);

        return populatedRecipe;
    }

    /**
     * Selects the nth sibling element after the given element in the DOM matching the given query.
     * Useful for finding sibling elements of an element.
     * @param origin the element to start looking down the DOM from
     * @param query a selector to match, e.g. the tag name
     * @param count number of matches to scroll through (1 = first match)
     * @return
     */
    private static Element selectNthElementAfter(Element origin, String query, int count) {
        Element currentElement = origin;
        Evaluator evaluator = QueryParser.parse(query);
        while ((currentElement = currentElement.nextElementSibling()) != null) {
            int val = 0;
            if (currentElement.is(evaluator)) {
                if (--count == 0)
                    return currentElement;
                val++;
            }
            Elements elems = currentElement.select(query);
            if (elems.size() > val) {
                int childCount = elems.size() - val;
                int diff = count - childCount;

                if (diff == 0) {
                    return elems.last();
                }
                if (diff > 0) {
                    count -= childCount;
                }
                if (diff < 0) {
                    return elems.get(childCount + diff);
                }
            }
        }
        if (origin.parent() != null) {
            return selectNthElementAfter(origin.parent(), query, count);
        }
        return null;
    }


}
