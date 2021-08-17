package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.content.DialogInterface;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.SharedViewModel;
import com.example.shoppinglistapp2.activities.ui.ViewPagerNavigationCallback;
import com.example.shoppinglistapp2.databinding.FragmentRecipeListBinding;
import com.example.shoppinglistapp2.helpers.KeyboardHider;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

public class RecipeListFragment extends Fragment implements RecipeListAdapter.OnRecipeClickListener, AdapterView.OnItemSelectedListener {

    private RecipeListViewModel viewModel;
    private SharedViewModel sharedViewModel;
    private ActionMode actionMode;
    private ActionMode.Callback multiSelectActionModeCallback = new ActionModeCallback(1);
    private ActionMode.Callback chooseMealPlanItemActionModeCallback = new ActionModeCallback(2);
    private RecipeListAdapter adapter;
    private ViewPagerNavigationCallback callback;
    private ListeningExecutorService backgroundExecutor;

    private FragmentRecipeListBinding binding;

    private boolean advancedSearchVisible = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel =
                new ViewModelProvider(requireActivity()).get(RecipeListViewModel.class);
        sharedViewModel =
                new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentRecipeListBinding.inflate(inflater, container, false);

        callback = (ViewPagerNavigationCallback) getActivity();

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;

        //this will delete ALL recipes and load recipetineats websites from the spreadsheet in res/raw/<name>.csv
//        recipesViewModel.loadFromBackup(this);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(savedInstanceState);
    }

    private void setupViews(Bundle savedInstanceState){
        //setup action bar
        this.setHasOptionsMenu(true);

        if(advancedSearchVisible) {
            binding.viewgroupAdvancedSearch.setVisibility(View.VISIBLE);
        }

        //setup recipe list recyclerview
        adapter = new RecipeListAdapter(backgroundExecutor,this);
        binding.recipeRecyclerview.setAdapter(adapter);
        binding.recipeRecyclerview.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update recipe list if it changes
        viewModel.getAllRecipes().observe(getViewLifecycleOwner(),
            recipes -> {
            //show or hide placeholder text for when there are no recipes
                if(recipes.size() == 0){
                    binding.textviewNoRecipes.setVisibility(View.VISIBLE);
                }
                else {
                    binding.textviewNoRecipes.setVisibility(View.GONE);

                    //display loading spinner
//                    binding.progressBarRecipeList.setVisibility(View.VISIBLE);
//                    binding.recipeRecyclerview.setVisibility(View.GONE);
                }

                //restore state of adapter in case of fragment reload
                adapter.setSearchCriteria(binding.searchCriteriaSpinner.getSelectedItemPosition());
                adapter.setOrderByCriteria(binding.orderBySpinner.getSelectedItemPosition());
                adapter.setLatestConstraint(binding.searchBar.getText().toString());

                //submit list
                adapter.updateList(recipes, () -> {
                    binding.progressBarRecipeList.setVisibility(View.GONE);
                    binding.recipeRecyclerview.setVisibility(View.VISIBLE);
                });
            });

        //populate advanced search spinners
        ArrayAdapter<CharSequence> scAdapter = ArrayAdapter.createFromResource(
                this.getContext(), R.array.search_criteria_options,android.R.layout.simple_spinner_item);
        scAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.searchCriteriaSpinner.setAdapter(scAdapter);
        binding.searchCriteriaSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> obAdapter = ArrayAdapter.createFromResource(
                this.getContext(), R.array.order_by_options,android.R.layout.simple_spinner_item);
        obAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.orderBySpinner.setAdapter(obAdapter);
        binding.orderBySpinner.setOnItemSelectedListener(this);

        //setup advanced search show/hide prompt
        binding.advancedSearchPrompt.setOnClickListener((view -> toggleAdvancedSearch()));

        //setup search bar
        //configure searchbar to not allow newline character entries, but still allow wrapping
        //over multiple lines
        binding.searchBar.setSingleLine(true);
        binding.searchBar.setHorizontallyScrolling(false);
        binding.searchBar.setMaxLines(20);

        //setup clear search button
        binding.clearSearchButton.setOnClickListener((v) -> binding.searchBar.setText(""));

        //have it listen and update results in realtime as the user types
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence newText, int start, int before, int count) {
                //end mutli-select if user changes search, as list will change
                if(actionMode != null && sharedViewModel.getSelectingForMeal() == null){
                    actionMode.finish();
                }
                adapter.filter(newText);

                //show clear search button if there's any text in the search bar
                if (newText.length() == 0){
                    binding.clearSearchButton.setVisibility(View.GONE);
                }
                else{
                    binding.clearSearchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        //hide keyboard when enter key pressed when using searchbar, so user can see the results
        binding.searchBar.setOnEditorActionListener((v, actionId, event) -> {
            KeyboardHider.hideKeyboard(requireActivity());
            v.clearFocus();
            return false;
        });

//        //setup autocomplete on the searchbar
//        ArrayAdapter<String> searchBarAdapter = new ArrayAdapter<>(getContext(),
//                android.R.layout.simple_dropdown_item_1line, outfitViewModel.getAllDistinctClothingItems());
//        searchBar.setAdapter(searchBarAdapter);
//        //configure autocomplete to consider comma separated phrases as separate tokens
//        searchBar.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    }

    /**
     * Flips the visibility of the advanced search criteria viewgroup
     * to the inverse of its current state.
     */
    private void toggleAdvancedSearch(){
        //if it was visible
        if (advancedSearchVisible){
            //hide advanced search and change prompt back to 'show'
            binding.viewgroupAdvancedSearch.setVisibility(View.GONE);
            binding.advancedSearchPrompt.setText(R.string.show_advanced_search_prompt);
        }
        //if it was hidden
        else{
            //show advanced search and change prompt to 'hide'
            binding.viewgroupAdvancedSearch.setVisibility(View.VISIBLE);
            binding.advancedSearchPrompt.setText(R.string.hide_advanced_search_prompt);
        }
        advancedSearchVisible = !advancedSearchVisible;
    }

    //hide back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();
        //check if we need to redirect to a recipe
        Integer recipeId = sharedViewModel.getNavigateToRecipeId();
        if(null != recipeId){
            //navigate to view recipe, passing id of clicked recipe along
            RecipeListFragmentDirections.ActionRecipeListToViewRecipe action = RecipeListFragmentDirections.actionRecipeListToViewRecipe();
            action.setRecipeId(recipeId);
            //clear value, so we don't redirect again next time
            sharedViewModel.setNavigateToRecipeId(null);
            Navigation.findNavController(requireView()).navigate(action);
        }

        //hide back button
        MainActivity activity = (MainActivity) getParentFragment().requireActivity();
        if (activity != null) {
            activity.hideUpButton();
        }

        //set title of page
        ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar().setTitle(R.string.title_recipes);

        //if we've arrived at this page to select a recipe for a meal plan,
        if(sharedViewModel.getSelectingForMeal() != null){
            //activate the appropriate action mode
            actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(chooseMealPlanItemActionModeCallback);

            //change title accordingly
            actionMode.setTitle(String.format("Choose a recipe for %s", sharedViewModel.getSelectingForMeal().getDayTitle()));
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
                Navigation.findNavController(requireView()).navigate(R.id.action_recipe_list_to_create_recipe);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRecipeClick(int recipeId) {
        //if we are currently in select for mealplan mode, click should instead save this recipe as a mealplan
        if(sharedViewModel.getSelectingForMeal() != null){
            //update db with this recipe in the specified meal plan slot
            Futures.addCallback(backgroundExecutor.submit(() -> sharedViewModel.saveToMealPlan(recipeId)),
                    new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(@Nullable Object result) {

                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Toast.makeText(requireContext(), R.string.error_adding_recipe_to_meal, Toast.LENGTH_LONG).show();
                            t.printStackTrace();
                        }
                    },
                    ContextCompat.getMainExecutor(requireContext()));

            //navigate back to meal plan tab
            actionMode.finish();
            callback.setViewpagerTo(0);
        }
        //otherwise, the click should send the user to view that recipe
        else{
            //navigate to view recipe, passing id of clicked recipe along
            RecipeListFragmentDirections.ActionRecipeListToViewRecipe action = RecipeListFragmentDirections.actionRecipeListToViewRecipe();
            action.setRecipeId(recipeId);
            Navigation.findNavController(requireView()).navigate(action);
        }
    }

    @Override
    public boolean onRecipeLongPress(View view, int position) {
        if (actionMode == null){
            actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(multiSelectActionModeCallback);
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
                adapter.refilter();

                //show appropriate hint
                switch (pos){
                    case 0:
                        binding.searchHint.setVisibility(View.GONE);
                        binding.searchBar.setHint(R.string.searchbar_name_hint);
                        break;
                    case 1:
                        binding.searchHint.setText(R.string.ingredient_search_hint);
                        binding.searchHint.setVisibility(View.VISIBLE);
                        binding.searchBar.setHint(R.string.searchbar_ingredient_hint);
                        break;
                    case 2:
                        binding.searchHint.setText(R.string.tag_search_hint);
                        binding.searchBar.setHint(R.string.searchbar_tag_hint);
                        binding.searchHint.setVisibility(View.VISIBLE);
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
        private final int actionCode;

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
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.delete_recipes_warning_title)
                            .setMessage(String.format("%s %d %s", getString(R.string.delete_warning_prompt1),
                                    adapter.getSelectedItemCount(),
                                    getString(R.string.delete_warning_prompt2)))
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                //actually delete selected recipes if confirmed
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(requireContext(), String.format("Deleted %d recipes", adapter.getSelectedItemCount()), Toast.LENGTH_LONG).show();
                                    Futures.addCallback(viewModel.deleteRecipes(adapter.getSelectedItems()),
                                        new FutureCallback<Integer>() {
                                            @Override
                                            public void onSuccess(@Nullable Integer result) {
                                                actionMode.finish();//remove action bar
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                                Toast.makeText(requireContext(), R.string.error_deleting_recipes, Toast.LENGTH_LONG).show();
                                                t.printStackTrace();
                                            }
                                        },
                                        ContextCompat.getMainExecutor(requireContext()));
                                }
                            })
                            //otherwise don't do anything
                            .setNegativeButton(R.string.cancel, null)
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
            sharedViewModel.clearSelectingForMeal();
            actionMode = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        //close action bar if user navigates away
        if(null != actionMode){
            actionMode.finish();
        }

    }
}