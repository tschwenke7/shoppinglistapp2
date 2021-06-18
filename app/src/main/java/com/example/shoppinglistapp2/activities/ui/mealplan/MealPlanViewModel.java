package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.shoppinglistapp2.db.SlaRepository;
import com.example.shoppinglistapp2.db.tables.MealPlan;

import java.util.List;

public class MealPlanViewModel extends AndroidViewModel {
    private LiveData<List<MealPlan>> allMealPlans;
    private SlaRepository slaRepository;

    public MealPlanViewModel(@NonNull Application application){
        super(application);
        slaRepository = new SlaRepository(application);

        allMealPlans = slaRepository.getAllMealPlans(1);
        List<MealPlan> test = allMealPlans.getValue();
        Log.d("TOM_TEST", String.valueOf(allMealPlans == null));
    }

    public LiveData<List<MealPlan>> getMealPlans(){
        return allMealPlans;
    }

    public void addDay(){
        MealPlan mealPlan = new MealPlan();
        mealPlan.setDayId(allMealPlans.getValue().size());
        mealPlan.setDayTitle("Title");
        slaRepository.insertMealPlan(mealPlan);
    }
}