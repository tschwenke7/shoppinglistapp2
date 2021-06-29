package com.example.shoppinglistapp2.activities.ui.mealplan;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListFragmentDirections;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListAdapter;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListViewModel;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.helpers.KeyboardHider;

import org.jetbrains.annotations.NotNull;

public class MealPlanFragment extends Fragment implements MealPlanListAdapter.MealPlanClickListener, ShoppingListAdapter.SlItemClickListener {

    private MealPlanViewModel mealPlanViewModel;
    private RecipesViewModel recipesViewModel;
    private ShoppingListViewModel shoppingListViewModel;
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
        shoppingListViewModel =
                new ViewModelProvider(getActivity()).get(ShoppingListViewModel.class);

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

        //setup ingredients recyclerview
        //setup meal plan recyclerview
        RecyclerView planIngredientsRecyclerView = root.findViewById(R.id.plan_ingredients_recyclerview);
        final ShoppingListAdapter planIngredientAdapter = new ShoppingListAdapter(this);
        planIngredientsRecyclerView.setAdapter(planIngredientAdapter);
        planIngredientsRecyclerView.setLayoutManager(new LinearLayoutManager((this.getContext())));

        //listen to meal plan ingredient list
        mealPlanViewModel.getAllMealPlanSlItems().observe(getViewLifecycleOwner(), (newList) -> {
            planIngredientAdapter.setItems(newList);
        });

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
            View content = planIngredientsRecyclerView;
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

        //'suggested recipes' clicked
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

        //listen to 'export ingredients to shopping list' icon
        root.findViewById(R.id.export_ingredients_icon).setOnClickListener((view) ->
                new AlertDialog.Builder(getContext())
                .setTitle(R.string.export_ingredients_option)
                .setMessage(R.string.export_ingredients_warning)
                .setPositiveButton(R.string.export_ingredients_positive, (dialogInterface, i) -> {
                    //add copy of all items to the shopping list
                    shoppingListViewModel.addItemsToShoppingList(mealPlanViewModel.getAllUncheckedMealPlanSlItems());
                    Toast.makeText(getContext(), getContext().getString(R.string.export_ingredients_success),Toast.LENGTH_SHORT).show();
                    callback.setViewpagerTo(2);
                })
                //otherwise don't do anything
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        super.onCreateOptionsMenu(menu,inflater);
//        menu.clear();
//        inflater.inflate(R.menu.meal_plan_action_bar, menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.remove_all_recipes:
                //prompt for confirmation first
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.remove_recipes_option)
                        .setMessage(R.string.remove_recipes_warning)
                        .setPositiveButton(R.string.remove_recipes_positive_button, (dialogInterface, i) -> {
                            //delete all recipes and ingredients
                            mealPlanViewModel.removeAllRecipes();
                        })
                        //otherwise don't do anything
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            case R.id.clear_meal_plans:
                //prompt for confirmation first
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.clear_meal_plans_option)
                        .setMessage(R.string.clear_meal_plans_warning)
                        .setPositiveButton(R.string.clear_meal_plans_positive_button, (dialogInterface, i) -> {
                            //delete everything
                            mealPlanViewModel.resetMealPlan();
                        })
                        //otherwise don't do anything
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
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
        KeyboardHider.hideKeyboard(requireActivity());

        //update title in database
        mealPlanViewModel.updateDayTitle(position, newTitle);
    }

    @Override
    public void onNotesConfirmClicked(int position, String newNotes) {
        KeyboardHider.hideKeyboard(requireActivity());

        //update notes in database
        mealPlanViewModel.updateNotes(position, newNotes);
    }

    @Override
    public void onDeleteNotesClicked(int position) {
        KeyboardHider.hideKeyboard(requireActivity());

        //set notes to be empty
        mealPlanViewModel.updateNotes(position, "");
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
        //set id of recipe to navigate to, then change viewpager to recipe tab
        //with a value set, the recipe list will redirect to this recipe automatically
        recipesViewModel.setNavigateToRecipeId(mealPlanViewModel.getMealPlans().getValue().get(position).getRecipeId());
        callback.setViewpagerTo(1);
    }

    @Override
    public void onRemoveRecipeClicked(int position) {
        MealPlan mealPlan = mealPlanViewModel.getMealPlans().getValue().get(position);
        //remove the recipe from the meal plan slot
        mealPlanViewModel.removeRecipe(mealPlan);
        //remove all ingredients from this recipe from the "ingredients needed" list
        recipesViewModel.removeIngredientsFromList(mealPlan.getRecipe().getIngredients());
    }

    @Override
    public void onDeleteMealClicked(int position) {
        KeyboardHider.hideKeyboard(requireActivity());
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_meal_option)
                .setMessage(R.string.delete_meal_warning)
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                    //delete all recipes and ingredients
                    mealPlanViewModel.deleteMealPlan(position);
                })
                //otherwise don't do anything
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onSlItemClick(int position) {
        recipesViewModel.toggleChecked(mealPlanViewModel.getAllMealPlanSlItems().getValue().get(position));
    }

    /** Navigation between viewpager fragments via activity */
    public interface Callback {
        void setViewpagerTo(int page);
    }
}