package com.example.shoppinglistapp2.activities.ui.mealplan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListViewModel;

import org.jetbrains.annotations.NotNull;

public class MealPlanFragment extends Fragment {

    private MealPlanViewModel mealPlanViewModel;

    public static MealPlanFragment newInstance() {
        return new MealPlanFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //get viewModel
        mealPlanViewModel =
                new ViewModelProvider(getActivity()).get(MealPlanViewModel.class);

        this.setHasOptionsMenu(true);

        //setup meal plan recyclerview
        RecyclerView mealPlanRecyclerView = getView().findViewById(R.id.plan_recipes_recyclerview);
        final MealPlanListAdapter mealPlanListAdapter = new MealPlanListAdapter();
        mealPlanRecyclerView.setAdapter(mealPlanListAdapter);
        mealPlanRecyclerView.setLayoutManager(new LinearLayoutManager((this.getContext())));

        //listen to meal plan list
        mealPlanViewModel.getMealPlans().observe(getViewLifecycleOwner(), mealPlanListAdapter::setList);

        //listen to add day button
        ((Button) getView().findViewById(R.id.add_day_button)).setOnClickListener(
                button -> mealPlanViewModel.addDay()
        );
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        menu.clear();
        inflater.inflate(R.menu.meal_plan_action_bar, menu);
    }

    @Override
    public void onResume(){
        super.onResume();

        //hide back button
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.hideUpButton();

        //set title
        ((AppCompatActivity) mainActivity).getSupportActionBar().setTitle(R.string.title_meal_plan);
    }
}