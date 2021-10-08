package com.example.shoppinglistapp2.activities.mainContentFragments.mealplan;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.ActionMode;
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

import com.example.shoppinglistapp2.activities.mainContentFragments.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.activities.mainContentFragments.MainContentFragment;
import com.example.shoppinglistapp2.activities.mainContentFragments.SharedViewModel;
import com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.ShoppingListAdapter;
import com.example.shoppinglistapp2.databinding.FragmentMealPlanBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;
import com.example.shoppinglistapp2.db.tables.withextras.PopulatedRecipeWithScore;
import com.example.shoppinglistapp2.helpers.KeyboardHelper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executor;

public class MealPlanFragment extends Fragment implements MealPlanListAdapter.MealPlanClickListener, SuggestedRecipesListAdapter.ClickListener, ShoppingListAdapter.SlItemClickListener {

    private static final String TAG = "T_DBG_MP_FRAG";
    private final int NUM_SUGGESTIONS_TO_LOAD = 5;
    private SharedViewModel sharedViewModel;
    private MealPlanViewModel viewModel;
    private FragmentMealPlanBinding binding;

    private SuggestedRecipesListAdapter suggestionsAdapter;
    private MealPlanListAdapter mealsAdapter;
    private Executor uiExecutor;
    private ListeningExecutorService backgroundExecutor;

    private ActionMode actionMode = null;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();

    private int currentMPId;
    private int previousIngListSize;
    private ItemTouchHelper itemTouchHelper;

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
        mealsAdapter = new MealPlanListAdapter(this);
        binding.planRecipesRecyclerview.setAdapter(mealsAdapter);
        binding.planRecipesRecyclerview.setLayoutManager(new LinearLayoutManager((this.getContext())));
        //allow drag and drop reordering of elements
        itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
        itemTouchHelper.attachToRecyclerView(binding.planRecipesRecyclerview);

        //listen to meal plan list
        viewModel.getMeals().observe(getViewLifecycleOwner(), (newList) -> {
            if (newList.isEmpty()) {
                binding.noMealsPlaceholder.setVisibility(View.VISIBLE);
                binding.planRecipesRecyclerview.setVisibility(View.GONE);
                binding.mealsLoadingSpinner.setVisibility(View.GONE);
            }
            else {
                binding.noMealsPlaceholder.setVisibility(View.GONE);
                mealsAdapter.submitList(newList, () -> {
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

        //suggested recipe list
        suggestionsAdapter = new SuggestedRecipesListAdapter(this);
        binding.suggestedRecipesRecyclerview.setAdapter(suggestionsAdapter);
        binding.suggestedRecipesRecyclerview.setLayoutManager(new LinearLayoutManager((this.getContext())));

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
                planIngredientAdapter.submitList(newList, () -> {
                    binding.noPlanIngredientsPlaceholder.setVisibility(View.GONE);
                    binding.planIngredientsRecyclerview.setVisibility(View.VISIBLE);
                });
            }

            //if the list has added/removed ingredients, update the suggested recipes list
            if(newList.size() != previousIngListSize) {
                //show spinner
                binding.recipeSuggestionsLoadingSpinner.setVisibility(View.VISIBLE);
                binding.suggestedRecipesRecyclerview.setVisibility(View.GONE);
                binding.noSuggestionsPlaceholder.setVisibility(View.GONE);
                //update suggested recipes
                reloadSuggestedRecipes();
            }

            previousIngListSize = newList.size();
        });



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
                new AlertDialog.Builder(requireContext())
                .setTitle(R.string.export_ingredients_option)
                .setMessage(R.string.export_ingredients_warning)
                .setPositiveButton(R.string.export_ingredients_positive, (dialogInterface, i) -> {
                    exportToShoppingList();

                })
                //otherwise don't do anything
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private void reloadSuggestedRecipes() {
        //hide spinner
        Futures.addCallback(
            backgroundExecutor.submit(() -> viewModel.getSearchSuggestions(NUM_SUGGESTIONS_TO_LOAD)),
            new FutureCallback<List<PopulatedRecipeWithScore>>() {
                @Override
                public void onSuccess(@Nullable List<PopulatedRecipeWithScore> result) {
                    if(result == null || result.isEmpty()) {
                        //show placeholder text
                        binding.noSuggestionsPlaceholder.setVisibility(View.VISIBLE);
                        binding.noSuggestionsPlaceholder.setText(R.string.no_suggestions_placeholder);
                        binding.suggestedRecipesRecyclerview.setVisibility(View.GONE);
                    }
                    else{
                        //show suggestions
                        binding.noSuggestionsPlaceholder.setVisibility(View.GONE);
                        binding.suggestedRecipesRecyclerview.setVisibility(View.VISIBLE);
                        suggestionsAdapter.submitList(result);
                    }

                    //hide spinner
                    binding.recipeSuggestionsLoadingSpinner.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(Throwable t) {
                    //hide spinner
                    binding.recipeSuggestionsLoadingSpinner.setVisibility(View.GONE);
                    binding.noSuggestionsPlaceholder.setVisibility(View.VISIBLE);
                    binding.noSuggestionsPlaceholder.setText(R.string.error_getting_suggestions);
                    Log.e(TAG, "loading suggested recipes: ", t);
                }
            },
            uiExecutor);

    }

    private void exportToShoppingList() {
        //add copy of all items to the shopping list
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.exportToShoppingList()),
            new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean result) {
                    Toast.makeText(getContext(), requireContext().getString(R.string.export_ingredients_success),Toast.LENGTH_SHORT).show();
                    ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager))
                            .setCurrentItem(MainContentFragment.SHOPPING_LIST_VIEWPAGER_INDEX);
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
                new AlertDialog.Builder(requireContext())
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
                new AlertDialog.Builder(requireContext())
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
        KeyboardHelper.hideKeyboard(requireActivity());

        //update title in database
        viewModel.updateDayTitle(position, newTitle);
    }

    @Override
    public void onNotesConfirmClicked(int position, String newNotes) {
        KeyboardHelper.hideKeyboard(requireActivity());

        //update notes in database
        viewModel.updateNotes(position, newNotes);
    }

    @Override
    public void onDeleteNotesClicked(int position) {
        KeyboardHelper.hideKeyboard(requireActivity());

        //set notes to be empty
        viewModel.updateNotes(position, "");
    }

    @Override
    public void onChooseRecipeClicked(int position) {
        //notify the viewmodel we are wanting to find a recipe for the specified mealplan
        sharedViewModel.setSelectingForMeal(viewModel.getMeals().getValue().get(position).getMeal());

        //navigate to recipes tab
        ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager))
                .setCurrentItem(MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX);
    }

    @Override
    public void onRecipeClicked(int position) {
        //set id of recipe to navigate to, then change viewpager to recipe tab
        //with a value set, the recipe list will redirect to this recipe automatically
        sharedViewModel.setNavigateToRecipeId(viewModel.getMeals().getValue().get(position).getRecipe().getId());
        ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager)).setCurrentItem(MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX);
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
        KeyboardHelper.hideKeyboard(requireActivity());
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_meal_option)
            .setMessage(R.string.delete_meal_warning)
            .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                //delete all recipes and ingredients
                Futures.addCallback(backgroundExecutor.submit(() -> viewModel.deleteMeal(position)),
                    new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(@Nullable Object result) {}

                        @Override
                        public void onFailure(Throwable t) {
                            Toast.makeText(requireContext(), getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                        }
                    },
                    uiExecutor);
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
                    reloadSuggestedRecipes();
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

    @Override
    public void onSuggestionClicked(int recipeId) {
        //notify the viewmodel we are wanting to find a recipe for the specified mealplan
        sharedViewModel.setNavigateToRecipeId(recipeId);

        //navigate to recipes tab
        ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager))
                .setCurrentItem(MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX);
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    }

    private ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.START | ItemTouchHelper.END, 0
    ) {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

            if(viewHolder.getItemViewType() == 1) {
                return super.getMovementFlags(recyclerView,viewHolder);
            }
            return 0;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPos = viewHolder.getAdapterPosition();
            int toPos = target.getAdapterPosition();
            mealsAdapter.swap(fromPos, toPos);
//            Collections.swap(mealsAdapter.getCurrentList(), fromPos, toPos);
//            mealsAdapter.notifyItemMoved(fromPos,toPos);
            return false;
        }

        /**
         * Triggers when drag is stopped. Here we will update the db backing of this recyclerview
         * @param recyclerView
         * @param viewHolder
         */
        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            viewModel.updateMeals(mealsAdapter.getMealsToPersist());
            ((MealPlanListAdapter.ContentViewHolder) viewHolder).setSelected(false);
            super.clearView(recyclerView, viewHolder);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            switch (actionState) {
                case ItemTouchHelper.ACTION_STATE_DRAG:
                    ((MealPlanListAdapter.ContentViewHolder) viewHolder).setSelected(true);
                    break;
                case ItemTouchHelper.ACTION_STATE_IDLE:
                    break;
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

        @Override
        public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
            return current.getItemViewType() == target.getItemViewType();
        }
    };

    @Override
    public void startDragging(BaseRecyclerViewAdapter<MealWithRecipe>.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
}