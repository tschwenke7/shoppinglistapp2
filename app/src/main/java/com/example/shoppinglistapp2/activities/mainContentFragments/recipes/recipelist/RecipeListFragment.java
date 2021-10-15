package com.example.shoppinglistapp2.activities.mainContentFragments.recipes.recipelist;

import android.content.DialogInterface;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import androidx.viewpager.widget.ViewPager;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ContentFragment;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.mainContentFragments.MainContentFragment;
import com.example.shoppinglistapp2.activities.mainContentFragments.SharedViewModel;
import com.example.shoppinglistapp2.databinding.FragmentRecipeListBinding;
import com.example.shoppinglistapp2.helpers.KeyboardHelper;
import com.example.shoppinglistapp2.helpers.RecipeSharer;
import com.google.common.base.CharMatcher;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.List;
import java.util.concurrent.Executor;

public class RecipeListFragment extends ContentFragment implements RecipeListAdapter.OnRecipeClickListener, AdapterView.OnItemSelectedListener {
    private final String TAG = "TDB_RCP_LIST_FRAG";

    private RecipeListViewModel viewModel;
    private SharedViewModel sharedViewModel;
    private ActionMode actionMode;
    private ActionMode.Callback multiSelectActionModeCallback = new ActionModeCallback(1);
    private ActionMode.Callback chooseMealPlanItemActionModeCallback = new ActionModeCallback(2);
    private RecipeListAdapter adapter;


    private FragmentRecipeListBinding binding;
    private int numSearchbarTokens;

    private boolean advancedSearchVisible = false;
    public enum SearchCriteria {
        NAME,
        INGREDIENT,
        TAG
    }

    private SearchCriteria searchCriteria;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel =
                new ViewModelProvider(requireActivity()).get(RecipeListViewModel.class);
        sharedViewModel =
                new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentRecipeListBinding.inflate(inflater, container, false);


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
        setHasMenu(true);
        setPageTitle(getString(R.string.title_recipes));
        setShowUpButton(false);

        if(advancedSearchVisible) {
            binding.viewgroupAdvancedSearch.setVisibility(View.VISIBLE);
        }
        //provide default starting state for searchCriteria if not set
        if(searchCriteria == null){
            searchCriteria = SearchCriteria.NAME;
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
                adapter.setSearchCriteria(searchCriteria);
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
        binding.clearSearchButton.setOnClickListener((v) -> {
            binding.searchBar.setText("");
            scrollToTop();
        });

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
                adapter.filter(newText, () -> scrollToTop());

                //show clear search button if there's any text in the search bar
                if (newText.length() == 0){
                    binding.clearSearchButton.setVisibility(View.GONE);
                }
                else{
                    binding.clearSearchButton.setVisibility(View.VISIBLE);
                }

                /* If searching by tag or ingredient, we want autocomplete to suggest only tokens
                * which can be found in the recipes matching existing tokens.
                * If a comma was added or removed, then the tokens will have changed, which will
                * affect the list of possible matching tokens and require an autocomplete update */
                int currentCommaCount = CharMatcher.is(',').countIn(newText);
                if((searchCriteria == SearchCriteria.TAG ||
                    searchCriteria == SearchCriteria.INGREDIENT) &&
                    numSearchbarTokens != currentCommaCount) {

                    numSearchbarTokens = currentCommaCount;
                    //update the autocomplete suggestions accordingly
                    setAutoCompleteSuggestions();
                }
                numSearchbarTokens = currentCommaCount;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        //hide keyboard when enter key pressed when using searchbar, so user can see the results
        binding.searchBar.setOnEditorActionListener((v, actionId, event) -> {
            KeyboardHelper.hideKeyboard(requireActivity());
            v.clearFocus();
            return false;
        });

        //configure autocomplete to consider comma separated phrases as separate tokens
        setAutoCompleteSuggestions();

        //make back button go back to meal plan fragment if the "choose a recipe" actionmode was on
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(actionMode != null && sharedViewModel.getSelectingForMeal() != null)
                    //navigate back to meal plan tab
                    ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager))
                            .setCurrentItem(MainContentFragment.MEAL_PLAN_VIEWPAGER_INDEX);
            }
        };

    }

    /** Retrieves and sets the autocomplete suggestions for the search bar based on the
     * current values of this.searchCriteria.
     */
    private void setAutoCompleteSuggestions(){
        ListenableFuture<List<String>> suggestions;

        switch (searchCriteria) {
            case INGREDIENT:
                //if no tokens have been completed, use full list of ingredients as suggestions
                if (numSearchbarTokens == 0){
                    suggestions = viewModel.getDistinctIngredientNames();
                }
                //if 1 or more tokens already exists, fetch autocomplete suggestions for ingredients
                //in the filtered list of recipes only
                else{
                    suggestions = backgroundExecutor.submit(() -> adapter.getOtherMatchingIngredients());
                }

                binding.searchBar.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
                break;

            case TAG:
                //if no tokens have been completed, use full list of tags as suggestions
                if(numSearchbarTokens == 0){
                    suggestions = viewModel.getDistinctTagNames();
                }
                //if 1 or more tokens already exists, fetch autocomplete suggestions for tags
                //in the filtered list of recipes only
                else{
                    suggestions = backgroundExecutor.submit(() -> adapter.getOtherMatchingTags());
                }

                binding.searchBar.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
                break;
            case NAME:
            default:
                suggestions = viewModel.getAllRecipeNames();
                binding.searchBar.setTokenizer(null);
                break;
        }

        Futures.addCallback(suggestions,
            new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(@Nullable List<String> result) {
                    ArrayAdapter<String> searchBarAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_dropdown_item_1line, result);
                    binding.searchBar.setAdapter(searchBarAdapter);
                }

                @Override
                public void onFailure(Throwable t) {
//                    Toast.makeText(requireContext(), R.string.error_could_not_load_autocomplete_suggestions, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to load autocomplete suggestions for search criteria" + searchCriteria.toString(), t);
                }
            },
            uiExecutor);
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
//        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.recipe_list_action_bar, menu);
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
                        //navigate back to meal plan tab
                        actionMode.finish();
                        ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager))
                                .setCurrentItem(MainContentFragment.MEAL_PLAN_VIEWPAGER_INDEX);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(requireContext(), R.string.error_adding_recipe_to_meal, Toast.LENGTH_LONG).show();
                        t.printStackTrace();
                    }
                },
                ContextCompat.getMainExecutor(requireContext()));
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
        else if (adapter.getSelectedItemCount() == 1){
            //change the title to say how many recipes are selected
            actionMode.setTitle(R.string.one_recipe_selected);
            actionMode.invalidate();
        }
        else {
            //change the title to say how many recipes are selected
            actionMode.setTitle(getString((R.string.many_recipes_selected), adapter.getSelectedItemCount()));
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
                //show appropriate hint
                switch (pos){
                    case 0:
                        searchCriteria = SearchCriteria.NAME;
                        binding.searchHint.setVisibility(View.GONE);
                        binding.searchBar.setHint(R.string.searchbar_name_hint);
                        break;
                    case 1:
                        searchCriteria = SearchCriteria.INGREDIENT;
                        binding.searchHint.setText(R.string.ingredient_search_hint);
                        binding.searchHint.setVisibility(View.VISIBLE);
                        binding.searchBar.setHint(R.string.searchbar_ingredient_hint);
                        break;
                    case 2:
                        searchCriteria = SearchCriteria.TAG;
                        binding.searchHint.setText(R.string.tag_search_hint);
                        binding.searchBar.setHint(R.string.searchbar_tag_hint);
                        binding.searchHint.setVisibility(View.VISIBLE);
                        break;
                }

                //notify adapter of change
                adapter.setSearchCriteria(searchCriteria);
                //refilter list based on new search criteria
                adapter.refilter(this::scrollToTop);

                //update autocompelte suggestions for the new search criteria
                setAutoCompleteSuggestions();

                break;

            //when an option is selected in the "order by" spinner
            case R.id.order_by_spinner:
                adapter.sort(pos, this::scrollToTop);
                break;
        }

        //scroll recyclerview back to the top
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        //neither spinner has a "nothing" option
    }

    private void scrollToTop() {
        binding.recipeRecyclerview.getLayoutManager().scrollToPosition(0);
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
                    return true;

                case R.id.action_export_recipe:
                    RecipeSharer.launchSharingIntent(requireContext(), adapter.getSelectedItems());
                    return true;

                case R.id.action_select_all:
                    adapter.selectAll();
                    //change the title to say how many recipes are selected
                    actionMode.setTitle(getString((R.string.many_recipes_selected), adapter.getSelectedItemCount()));
                    actionMode.invalidate();
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelections();
            if(sharedViewModel.getSelectingForMeal() != null) {
                ((ViewPager) requireActivity().findViewById(R.id.main_content_view_pager))
                        .setCurrentItem(MainContentFragment.MEAL_PLAN_VIEWPAGER_INDEX);
                sharedViewModel.clearSelectingForMeal();
            }
            actionMode = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //close action bar if user navigates away
        if(null != actionMode){
            actionMode.finish();
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
}