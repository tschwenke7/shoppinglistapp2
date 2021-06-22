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
import android.widget.ImageView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.helpers.KeyboardHider;

import org.jetbrains.annotations.NotNull;

public class MealPlanFragment extends Fragment implements MealPlanListAdapter.MealPlanClickListener {

    private MealPlanViewModel mealPlanViewModel;
    private RecipesViewModel recipesViewModel;
    private Callback callback;

    public static MealPlanFragment newInstance() {
        return new MealPlanFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        callback = (Callback) getActivity();//enables navigation of viewpager from within fragment
        return inflater.inflate(R.layout.fragment_meal_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //get viewModels
        mealPlanViewModel =
                new ViewModelProvider(getActivity()).get(MealPlanViewModel.class);
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        setupViews(view);

    }

    private void setupViews(View root){
        this.setHasOptionsMenu(true);

        //setup meal plan recyclerview
        RecyclerView mealPlanRecyclerView = root.findViewById(R.id.plan_recipes_recyclerview);
        final MealPlanListAdapter mealPlanListAdapter = new MealPlanListAdapter(this);
        mealPlanRecyclerView.setAdapter(mealPlanListAdapter);
        mealPlanRecyclerView.setLayoutManager(new LinearLayoutManager((this.getContext())));

        //loading spinner which shows while mealPlan recyclerview is loading
        View loadingSpinner = root.findViewById(R.id.meals_loading_spinner);

        //listen to meal plan list
        mealPlanViewModel.getMealPlans().observe(getViewLifecycleOwner(), (newList) -> {
            mealPlanListAdapter.setList(newList);

            //swap loading spinner for recyclerview once loaded
            mealPlanRecyclerView.setVisibility(View.VISIBLE);
            loadingSpinner.setVisibility(View.GONE);
        });

        //listen to add day button
        ((Button) root.findViewById(R.id.add_day_button)).setOnClickListener(
                button -> mealPlanViewModel.addDay()
        );

        /* listen to expand/hide section arrows */
        //'meals' clicked
        root.findViewById(R.id.layout_meals_title).setOnClickListener((view) -> {
            ImageView arrow = (ImageView) view.findViewById(R.id.plan_expand_arrow);
            View content = root.findViewById(R.id.layout_meals_content);
            //if visible, hide
            if(content.getVisibility() == View.VISIBLE){
                arrow.setImageResource(R.drawable.ic_hidden_arrow);
                content.setVisibility(View.GONE);
            }
            //if hidden, make visible
            else{
                arrow.setImageResource(R.drawable.ic_expanded_arrow);
                content.setVisibility(View.VISIBLE);
            }
        });

        //'ingredients needed' clicked
        root.findViewById(R.id.layout_ingredients_title).setOnClickListener((view) -> {
            ImageView arrow = (ImageView) view.findViewById(R.id.plan_ingredients_expand_arrow);
            View content = root.findViewById(R.id.plan_ingredients_recyclerview);
            //if visible, hide
            if(content.getVisibility() == View.VISIBLE){
                arrow.setImageResource(R.drawable.ic_hidden_arrow);
                content.setVisibility(View.GONE);
            }
            //if hidden, make visible
            else{
                arrow.setImageResource(R.drawable.ic_expanded_arrow);
                content.setVisibility(View.VISIBLE);
            }
        });

        //'ingredients needed' clicked
        root.findViewById(R.id.layout_suggestions_title).setOnClickListener((view) -> {
            ImageView arrow = (ImageView) view.findViewById(R.id.plan_suggestions_expand_arrow);
            View content = root.findViewById(R.id.suggested_recipes_recyclerview);
            //if visible, hide
            if(content.getVisibility() == View.VISIBLE){
                arrow.setImageResource(R.drawable.ic_hidden_arrow);
                content.setVisibility(View.GONE);
            }
            //if hidden, make visible
            else{
                arrow.setImageResource(R.drawable.ic_expanded_arrow);
                content.setVisibility(View.VISIBLE);
            }
        });
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

    @Override
    public void onTitleConfirmClicked(int position, String newTitle) {
        KeyboardHider.hideKeyboard(getActivity());

        //update title in database
        mealPlanViewModel.updateDayTitle(position, newTitle);
    }

    @Override
    public void onChooseRecipeClicked(int position) {
        //notify the viewmodel we are wanting to find a recipe for the specified mealplan
        recipesViewModel.setSelectingForMeal(mealPlanViewModel.getMealPlans().getValue().get(position));

        //navigate to recipes tab
        callback.setViewpagerTo(1);
    }

    @Override
    public void onRecipeClicked(int position) {

    }

    @Override
    public void onRemoveRecipeClicked(int position) {
        mealPlanViewModel.removeRecipe(position);
    }

    /** Navigation between viewpager fragments via activity */
    public interface Callback {
        void setViewpagerTo(int page);
    }
}