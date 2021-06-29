package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;

public class RecipeListFragment extends Fragment implements RecipeListAdapter.OnRecipeClickListener, AdapterView.OnItemSelectedListener {

    private View root;
    private RecipesViewModel recipesViewModel;
    private ActionMode actionMode;
    private ActionMode.Callback multiSelectActionModeCallback = new ActionModeCallback(1);
    private ActionMode.Callback chooseMealPlanItemActionModeCallback = new ActionModeCallback(2);
    private RecipeListAdapter adapter;
    private boolean advancedSearchVisible;
    private Callback callback;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        root = inflater.inflate(R.layout.fragment_recipe_list, container, false);

        callback = (Callback) getActivity();

        //this will delete ALL recipes and load recipetineats websites from the spreadsheet in res/raw/<name>.csv
//        recipesViewModel.loadFromBackup(this);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(root);
    }

    private void setupViews(View root){
        //setup action bar
        this.setHasOptionsMenu(true);

        //setup recipe list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_recyclerview);
        adapter = new RecipeListAdapter(this);
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update recipe list if it changes
        recipesViewModel.getAllRecipes().observe(getViewLifecycleOwner(),
                recipes -> adapter.setRecipes(recipes));

        //populate advanced search spinners
        Spinner searchCriteriaSpinner = root.findViewById(R.id.search_criteria_spinner);
        ArrayAdapter<CharSequence> scAdapter = ArrayAdapter.createFromResource(
                this.getContext(), R.array.search_criteria_options,android.R.layout.simple_spinner_item);
        scAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchCriteriaSpinner.setAdapter(scAdapter);
        searchCriteriaSpinner.setOnItemSelectedListener(this);

        Spinner orderBySpinner = root.findViewById(R.id.order_by_spinner);
        ArrayAdapter<CharSequence> obAdapter = ArrayAdapter.createFromResource(
                this.getContext(), R.array.order_by_options,android.R.layout.simple_spinner_item);
        obAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orderBySpinner.setAdapter(obAdapter);
        orderBySpinner.setOnItemSelectedListener(this);

        //setup advanced search show/hide prompt
        ((TextView) root.findViewById(R.id.advanced_search_prompt)).setOnClickListener((view -> toggleAdvancedSearch()));

        //setup search bar
        SearchView searchView = (SearchView) root.findViewById(R.id.search_bar);
        //hide default underline style of searchview
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        searchPlate.setBackgroundColor(Color.TRANSPARENT);
        //have it listen and update results in realtime as the user types
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //end mutli-select if user changes search, as list weill change
                if(actionMode != null && recipesViewModel.getSelectingForMeal() == null){
                    actionMode.finish();
                }
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    /**
     * Flips the visibility of the advanced search criteria viewgroup
     * to the inverse of its current state.
     */
    private void toggleAdvancedSearch(){
        ViewGroup advancedSearch = root.findViewById(R.id.viewgroup_advanced_search);
        TextView advancedSearchPrompt = root.findViewById(R.id.advanced_search_prompt);

        //if it was visible
        if (advancedSearchVisible){
            //hide advanced search and change prompt back to 'show'
            advancedSearch.setVisibility(View.GONE);
            advancedSearchPrompt.setText(R.string.show_advanced_search_prompt);
        }
        //if it was hidden
        else{
            //show advanced search and change prompt to 'hide'
            advancedSearch.setVisibility(View.VISIBLE);
            advancedSearchPrompt.setText(R.string.hide_advanced_search_prompt);
        }

        advancedSearchVisible = !advancedSearchVisible;
    }

    //hide back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();
        //check if we need to redirect to a recipe
        Integer recipeId = recipesViewModel.getNavigateToRecipeId();
        if(null != recipeId){
            //navigate to view recipe, passing id of clicked recipe along
            RecipeListFragmentDirections.ActionRecipeListToViewRecipe action = RecipeListFragmentDirections.actionRecipeListToViewRecipe();
            action.setRecipeId(recipeId);
            //clear value, so we don't redirect again next time
            recipesViewModel.setNavigateToRecipeId(null);
            Navigation.findNavController(root).navigate(action);
        }

        //hide back button
        MainActivity activity = (MainActivity) getParentFragment().getActivity();
        if (activity != null) {
            activity.hideUpButton();
        }

        //set title of page
        ((AppCompatActivity) getParentFragment().getActivity()).getSupportActionBar().setTitle(R.string.title_recipes);

        //if we've arrived at this page to select a recipe for a meal plan,
        if(recipesViewModel.getSelectingForMeal() != null){
            //activate the appropriate action mode
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(chooseMealPlanItemActionModeCallback);

            //change title accordingly
            actionMode.setTitle(String.format("Choose a recipe for %s", recipesViewModel.getSelectingForMeal().getDayTitle()));
            actionMode.invalidate();
        }
    }

    /** Merges extra menu items into the default activity action bar, according to provided menu xml */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        inflater.inflate(R.menu.recipe_list_action_bar, menu);
    }

    /** Handle onClick for the custom action bar menu items for this fragment */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_recipe:  {
                //navigate to recipe creation hub
                Navigation.findNavController(root).navigate(R.id.action_recipe_list_to_create_recipe);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRecipeClick(int position) {
        Log.d("TOM_TEST", "onRecipeClick triggered for item " + position);

        //if we are currently in select for mealplan mode, click should instead save this recipe as a mealplan
        if(recipesViewModel.getSelectingForMeal() != null){
            //update db with this recipe in the specified meal plan slot
            recipesViewModel.saveToMealPlan(position);

            //navigate back to meal plan tab
            actionMode.finish();
            callback.setViewpagerTo(0);
        }
        //otherwise, the click should send the user to view that recipe
        else{
            //navigate to view recipe, passing id of clicked recipe along
            RecipeListFragmentDirections.ActionRecipeListToViewRecipe action = RecipeListFragmentDirections.actionRecipeListToViewRecipe();
            action.setRecipeId(recipesViewModel.getRecipeIdAtPosition(position));
            Navigation.findNavController(root).navigate(action);
        }
    }

    @Override
    public boolean onRecipeLongPress(View view, int position) {
        if (actionMode == null){
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(multiSelectActionModeCallback);
        }

        //check if all items have been deselected to close actionMode
        if (adapter.getSelectedItemCount() == 0){
            actionMode.finish();
        }
        //otherwise update the heading
        else {
            //change the title to say how many recipes are selected
            actionMode.setTitle(String.format("%d recipe/s selected",adapter.getSelectedItemCount()));
            actionMode.invalidate();
        }

        return true;
    }

    /**
     * Respond to inputs to the advanced search spinners
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()){
            //when an option is selected in the "search by" spinner
            case R.id.search_criteria_spinner:
                //respond to option selection here
                adapter.setSearchCriteria(pos);
                //show appropriate hint
                TextView hintTextView = root.findViewById(R.id.search_hint);
                switch (pos){
                    case 0:
                        hintTextView.setVisibility(View.GONE);
                        break;
                    case 1:
                        hintTextView.setText(R.string.ingredient_search_hint);
                        hintTextView.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        hintTextView.setText(R.string.tag_search_hint);
                        hintTextView.setVisibility(View.VISIBLE);
                        break;
                }
                break;

            //when an option is selected in the "order by" spinner
            case R.id.order_by_spinner:
                adapter.sort(pos);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        //neither spinner has a "nothing" option
    }

    /** Creates and handles a contextual action bar for when one or more recipes are selected
     * Can be for one of the following options, differentiated in constructor by actionCode:
     * 1 - multi select for deletion or bulk adding ingredients to shopping list
     * 2 - select a recipe to add to meal plan slot */
    private class ActionModeCallback implements ActionMode.Callback{
        /** 1 - multi select for deletion or bulk adding ingredients to shopping list
         *  2 - select a recipe to add to meal plan slot */
        private int actionCode;

        public int getActionCode() {
            return actionCode;
        }

        public ActionModeCallback(int actionCode) {
            super();
            this.actionCode = actionCode;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            switch (actionCode){
                case 1: actionMode.getMenuInflater().inflate(R.menu.recipe_selected_action_bar, menu);
                    break;
                case 2: actionMode.getMenuInflater().inflate(R.menu.choose_meal_plan_item_menu, menu);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()){
                case R.id.action_add_all_to_list:

                    //TODO - add all ingredients from these recipes to the shopping list
                    actionMode.finish();
                    return true;

                //Handle clicking of delete button
                case R.id.action_delete_recipe:
                    //prompt for confirmation first
                    new AlertDialog.Builder(root.getContext())
                            .setTitle(R.string.delete_recipes_warning_title)
                            .setMessage(String.format("%s %d %s",
                                    root.getContext().getString(R.string.delete_warning_prompt1),
                                    adapter.getSelectedItemCount(),
                                    root.getContext().getString(R.string.delete_warning_prompt2)))
                            .setPositiveButton(R.string.delete_warning_yes_button, new DialogInterface.OnClickListener() {
                                //actually delete selected recipes if confirmed
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(root.getContext(), String.format("Deleted %d recipes", adapter.getSelectedItemCount()), Toast.LENGTH_LONG).show();
                                    recipesViewModel.deleteRecipes(adapter.getSelectedItems());
                                    actionMode.finish();//remove action bar
                                }
                            })
                            //otherwise don't do anything
                            .setNegativeButton(R.string.delete_warning_cancel_button, null)
                            .show();

                    return true;

                //Case where user was choosing a recipe to add to meal plan, but cancels
                case R.id.action_cancel_selection:
                    //end action mode
                    actionMode.finish();

                    //navigate back to meal plan tab
                    callback.setViewpagerTo(0);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelections();
            recipesViewModel.clearSelectingForMeal();
            actionMode = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //close action bar if user navigates away
        if(null != actionMode){
            actionMode.finish();
        }

    }

    /** Navigation between viewpager fragments via activity */
    public interface Callback {
        void setViewpagerTo(int page);
    }
}