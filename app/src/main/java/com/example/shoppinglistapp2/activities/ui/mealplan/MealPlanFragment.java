package com.example.shoppinglistapp2.activities.ui.mealplan;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;

import com.example.shoppinglistapp2.activities.ui.SharedViewModel;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListAdapter;
import com.example.shoppinglistapp2.databinding.FragmentMealPlanBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.KeyboardHider;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class MealPlanFragment extends Fragment implements MealPlanListAdapter.MealPlanClickListener, ShoppingListAdapter.SlItemClickListener {

    private static final String TAG = "T_DBG_MP_FRAG";
    private SharedViewModel sharedViewModel;
    private MealPlanViewModel viewModel;
    int currentMPId;
    private FragmentMealPlanBinding binding;

    private Executor uiExecutor;
    private ListeningExecutorService backgroundExecutor;

    private Callback callback;

    public static MealPlanFragment newInstance() {
        return new MealPlanFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMealPlanBinding.inflate(inflater, container, false);

        //get viewModels
        viewModel =
                new ViewModelProvider(requireActivity()).get(MealPlanViewModel.class);
        currentMPId = -1; //todo change this once multiple meal plans possible
        viewModel.setMealPlan(currentMPId);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        callback = (Callback) requireActivity();//enables navigation of viewpager from within fragment

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(requireContext());

        setupViews();
    }

    private void setupViews(){
        this.setHasOptionsMenu(true);

        //setup meal plan recyclerview
        final MealPlanListAdapter mealPlanListAdapter = new MealPlanListAdapter(this);
        binding.planRecipesRecyclerview.setAdapter(mealPlanListAdapter);
        binding.planRecipesRecyclerview.setLayoutManager(new LinearLayoutManager((this.getContext())));

        //listen to meal plan list
        viewModel.getMeals().observe(getViewLifecycleOwner(), (newList) -> {
            if (newList.isEmpty()) {
                binding.noMealsPlaceholder.setVisibility(View.VISIBLE);
                binding.planRecipesRecyclerview.setVisibility(View.GONE);
                binding.mealsLoadingSpinner.setVisibility(View.GONE);
            }
            else {
                binding.noMealsPlaceholder.setVisibility(View.GONE);
                mealPlanListAdapter.submitList(newList, () -> {
                    //swap loading spinner for recyclerview once loaded
                    binding.planRecipesRecyclerview.setVisibility(View.VISIBLE);
                    binding.mealsLoadingSpinner.setVisibility(View.GONE);
                });
            }
        });

        //listen to add day button
        binding.addDayButton.setOnClickListener(
                button -> viewModel.addMeal()
        );

        //setup ingredients recyclerview
        final ShoppingListAdapter planIngredientAdapter = new ShoppingListAdapter(this);
        binding.planIngredientsRecyclerview.setAdapter(planIngredientAdapter);
        binding.planIngredientsRecyclerview.setLayoutManager(new LinearLayoutManager((this.getContext())));

        //listen to meal plan ingredient list
        viewModel.getMealPlanIngredients().observe(getViewLifecycleOwner(), (newList) -> {
            if(newList.isEmpty()) {
                binding.noPlanIngredientsPlaceholder.setVisibility(View.VISIBLE);
                binding.planIngredientsRecyclerview.setVisibility(View.GONE);
            }
            else{
                binding.noPlanIngredientsPlaceholder.setVisibility(View.GONE);
                binding.planIngredientsRecyclerview.setVisibility(View.VISIBLE);
                planIngredientAdapter.submitList(newList);
            }

        });

        //suggested recipe list
        binding.noSuggestionsPlaceholder.setVisibility(View.VISIBLE);
        binding.suggestedRecipesRecyclerview.setVisibility(View.GONE);

        /* listen to expand/hide section arrows */
        //'meals' clicked
        binding.layoutMealsTitle.setOnClickListener((view) -> {
            expandOrCollapseSection(binding.layoutMealsContent, binding.planExpandArrow);
        });

        //'ingredients needed' clicked
        binding.layoutIngredientsTitle.setOnClickListener((view) -> {
            expandOrCollapseSection(binding.layoutIngredientsContent, binding.planIngredientsExpandArrow);
        });

        //'suggested recipes' clicked
        binding.layoutSuggestionsTitle.setOnClickListener((view) -> {
            expandOrCollapseSection(binding.layoutSuggestionsContent, binding.planSuggestionsExpandArrow);
        });

        //listen to 'export ingredients to shopping list' icon
        binding.exportIngredientsIcon.setOnClickListener((view) ->
                new AlertDialog.Builder(getContext())
                .setTitle(R.string.export_ingredients_option)
                .setMessage(R.string.export_ingredients_warning)
                .setPositiveButton(R.string.export_ingredients_positive, (dialogInterface, i) -> {
                    exportToShoppingList();

                })
                //otherwise don't do anything
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private void exportToShoppingList() {
        //add copy of all items to the shopping list
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.exportToShoppingList()),
            new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean result) {
                    Toast.makeText(getContext(), getContext().getString(R.string.export_ingredients_success),Toast.LENGTH_SHORT).show();
                    callback.setViewpagerTo(MainActivity.SHOPPING_LIST_VIEWPAGER_INDEX);
                }

                @Override
                public void onFailure(Throwable t) {
                    Toast.makeText(requireContext(), R.string.error_sending_ingredients_to_shopping_list, Toast.LENGTH_LONG).show();
                }
            },
            uiExecutor
        );
    }

    private void expandOrCollapseSection(View content, ImageView arrow){
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
                            //delete all recipes, notes and ingredients
                            clearAllMeals();
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
                            backgroundExecutor.submit(() -> viewModel.resetMealPlan());
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

    private void clearAllMeals() {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.clearAllMeals()),
                new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(@Nullable Object result) {

                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(requireContext(), R.string.error_clearing_meals, Toast.LENGTH_LONG).show();
                    }
                },
                uiExecutor);
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
        viewModel.updateDayTitle(position, newTitle);
    }

    @Override
    public void onNotesConfirmClicked(int position, String newNotes) {
        KeyboardHider.hideKeyboard(requireActivity());

        //update notes in database
        viewModel.updateNotes(position, newNotes);
    }

    @Override
    public void onDeleteNotesClicked(int position) {
        KeyboardHider.hideKeyboard(requireActivity());

        //set notes to be empty
        viewModel.updateNotes(position, "");
    }

    @Override
    public void onChooseRecipeClicked(int position) {
        //notify the viewmodel we are wanting to find a recipe for the specified mealplan
        sharedViewModel.setSelectingForMeal(viewModel.getMeals().getValue().get(position).getMeal());

        //navigate to recipes tab
        callback.setViewpagerTo(MainActivity.RECIPE_LIST_VIEWPAGER_INDEX);
    }

    @Override
    public void onRecipeClicked(int position) {
        //set id of recipe to navigate to, then change viewpager to recipe tab
        //with a value set, the recipe list will redirect to this recipe automatically
        sharedViewModel.setNavigateToRecipeId(viewModel.getMeals().getValue().get(position).getRecipe().getId());
        callback.setViewpagerTo(MainActivity.RECIPE_LIST_VIEWPAGER_INDEX);
    }

    @Override
    public void onRemoveRecipeClicked(int position) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.removeRecipeFromMealAtPos(position)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {}

                @Override
                public void onFailure(Throwable t) {
                    Toast.makeText(requireContext(), R.string.error_removing_recipe_from_meal, Toast.LENGTH_LONG).show();
                }
            },
            uiExecutor
        );
    }

    @Override
    public void onDeleteMealClicked(int position) {
        KeyboardHider.hideKeyboard(requireActivity());
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_meal_option)
                .setMessage(R.string.delete_meal_warning)
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                    //delete all recipes and ingredients
                    viewModel.deleteMeal(position);
                })
                //otherwise don't do anything
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onSlItemClick(int position) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.toggleChecked(position)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {

                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "while crossing off item: ", t);
                    Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                }
            },
            uiExecutor);
    }

    @Override
    public void onSlItemEditConfirm(IngListItem oldItem, String newItem) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.editIngListItem(oldItem,newItem)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {

                }

                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof InterruptedException){
                        Log.e(TAG, "adding items to shoppping list: ", t);
                        Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                    }
                    else{
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.error_title)
                                .setMessage(R.string.error_could_not_add_items)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                }
            },
            uiExecutor);
    }

    /** Navigation between viewpager fragments via activity */
    public interface Callback {
        void setViewpagerTo(int page);
    }
}