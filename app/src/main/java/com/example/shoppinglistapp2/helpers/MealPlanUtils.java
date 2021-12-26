package com.example.shoppinglistapp2.helpers;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;

public class MealPlanUtils {
    private static final String[] daysOfTheWeek = new String[] {
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday",
            "Monday" //repeat first day so we can consistently tell what day comes after a given day
    };

    private static final String[] daysOfTheWeekLowerCase = new String[] {
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday",
            "sunday",
            "monday" //repeat first day so we can consistently tell what day comes after a given day
    };

    private static final String[] meals = new String[] {
            "lunch",
            "dinner"
    };

    /**
     * Checks if previous title made mention of a day name, and if so,
     * returns the next day of the week. Otherwise it returns a default placeholder name.
     * @param previousTitle - the title of the previous meal.
     * @return a suggested title using the next day of the week, or the default title.
     */
    public static String suggestNextMealTitle(String previousTitle) {
        //compare previous title to each day of the week
        if (previousTitle != null) {
            for(int i = 0; i < daysOfTheWeek.length - 1; i++){ //-1 so we don't check the second "monday"
                //if there's a match, return the NEXT day of the week
                if(previousTitle.toLowerCase().contains(daysOfTheWeekLowerCase[i])){
                    return daysOfTheWeek[i+1];
                }
            }
        }
        //if there was no match or previous was null, return the default title
        return App.getRes().getString(R.string.default_meal_title);
    }
}
