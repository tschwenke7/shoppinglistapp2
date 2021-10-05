package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.SharedViewModel;
import com.example.shoppinglistapp2.activities.ui.ViewPagerNavigationCallback;
import com.example.shoppinglistapp2.databinding.FragmentViewRecipeBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.helpers.KeyboardHider;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.google.android.material.chip.Chip;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.validator.routines.UrlValidator;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class ViewRecipeFragment extends Fragment implements IngredientListAdapter.IngredientClickListener {
    private static final String TAG = "TDB_VIEW_REC_FRAG";
    private ViewRecipeViewModel viewModel;
    private SharedViewModel sharedViewModel;
    private FragmentViewRecipeBinding binding;

    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;

    private int recipeId;
    private boolean editingFlag;
    private boolean newRecipeFlag;
    private LiveData<Recipe> currentRecipe;
    private boolean saved;
    private LiveData<List<IngListItem>> ingredients;
    private ActionMode actionMode;
    private ActionMode.Callback actionModeCallback = new ViewRecipeFragment.ActionModeCallback();
    private IngredientListAdapter adapter;
    private String pageTitle;
    private ViewPagerNavigationCallback callback;

    private int lastServesVal;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel =
                new ViewModelProvider(requireActivity()).get(ViewRecipeViewModel.class);
        sharedViewModel =
                new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentViewRecipeBinding.inflate(inflater,container,false);

        callback = (ViewPagerNavigationCallback) getActivity();

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(requireContext());

        //retrieve navigation args
        //recipe to be viewed
        recipeId = ViewRecipeFragmentArgs.fromBundle(getArguments()).getRecipeId();

        //true if this recipe was just created rather than selected from recipe list
        newRecipeFlag = ViewRecipeFragmentArgs.fromBundle(getArguments()).getNewRecipeFlag();

        saved = false;

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        backgroundExecutor.submit(() -> viewModel.saveBackupOfRecipe(recipeId));

        //decide whether to start in edit mode or not
        setupViews(view, ViewRecipeFragmentArgs.fromBundle(getArguments()).getEditingFlag());
    }

    private void setupViews(View root, boolean editingFlag){
        //setup action bar
        this.setHasOptionsMenu(true);

        currentRecipe = viewModel.getRecipe(recipeId);


        /* fill in textViews with saved recipe data where available */
        currentRecipe.observe(getViewLifecycleOwner(),
            ((recipe) -> {
                if(recipe != null){
                    populateRecipeViews(recipe);
                }
            })
        );

        //setup add-to buttons
        binding.buttonAddToShoppingList.setOnClickListener((v) -> promptSendIngredientsToShoppingList());
        binding.buttonAddToMealPlan.setOnClickListener(this::addRecipeToMealPlan);

        //setup ingredient list recyclerview
        //set observer to update ingredient list if it changes
        ingredients = viewModel.getRecipeIngredientsById(recipeId);
        adapter = new IngredientListAdapter(this);
        binding.recipeIngredientsList.setAdapter(adapter);
        binding.recipeIngredientsList.setLayoutManager(new LinearLayoutManager(this.getContext()));

        ingredients.observe(getViewLifecycleOwner(), (list) -> {
            //if list is empty, show placeholder text instead
            if(list == null || list.size() == 0){
                binding.recipeIngredientsList.setVisibility(View.GONE);
                binding.textviewNoIngredientsPlaceholder.setVisibility(View.VISIBLE);
                binding.editIngredientsHint.setVisibility(View.GONE);
            }
            else{
                binding.recipeIngredientsList.setVisibility(View.VISIBLE);
                binding.textviewNoIngredientsPlaceholder.setVisibility(View.GONE);
                if (editingFlag) {
                    binding.editIngredientsHint.setVisibility(View.VISIBLE);
                }
                adapter.submitList(list);
            }

        });

        //handle ingredient being added
        binding.recipeAddIngredientButton.setOnClickListener((v) -> addIngredients());

        //add existing tags
        Futures.addCallback(
                backgroundExecutor.submit(() -> viewModel.getRecipeTags(recipeId)),
                new FutureCallback<List<Tag>>() {
                    @Override
                    public void onSuccess(@Nullable List<Tag> result) {
                        if(null != result){
                            for (Tag tag : result){
                                addTag(tag);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(requireContext(), R.string.error_loading_tags, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "getting tags while setting up: ", t);
                    }
                },
                uiExecutor
        );

        //handle new tag being added
        binding.addTagButton.setOnClickListener((view) -> {
            TextView tagField =  root.findViewById(R.id.edit_text_tag);
            String tagName = tagField.getText().toString();
            if (!tagName.isEmpty()){
                //persist to db
                Tag newTag = new Tag(recipeId, tagName);
                viewModel.insertTag(newTag);
                //add tag chip to ui
                addTag(newTag);

                //clear input field
                tagField.setText("");
            }
        });

        //setup tag input autocomplete
        Futures.addCallback(
            viewModel.getDistinctTagNames(),
            new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(@Nullable List<String> result) {
                    binding.editTextTag.post(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                getContext(), android.R.layout.simple_dropdown_item_1line, result);
                        binding.editTextTag.setAdapter(adapter);
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "getting all tag names for autocomplete: ", t);
                }
            },
            uiExecutor
        );

        //setup serves editor
        binding.editTextServes.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                changeServes();
            }
        });



        //configure either in edit mode or view only mode
        if(editingFlag){
            enterEditMode();
        }
        else{
            enterViewMode();
        }

        //make back button work within these nested fragments
        Fragment f1 = this;
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavHostFragment.findNavController(f1).navigateUp();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    private void changeServes() {
        int newServes = Integer.parseInt(binding.editTextServes.getText().toString());

        //validate that serves is not 0 or negative
        if(newServes < 1) {
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.error_title)
                .setMessage(R.string.error_serves_cant_be_negative)
                .setPositiveButton(R.string.ok, null)
                .show();
            binding.editTextServes.setText(lastServesVal);
        }
        else if (newServes != lastServesVal){
            //prompt user if they want to increase ingredients qtys in proportion
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_serves_dialog_title)
                .setMessage(getString(R.string.change_serves_dialog_message, lastServesVal, newServes))
                .setPositiveButton(R.string.yes, (dialogInterface, i) ->
                    Futures.addCallback(backgroundExecutor.submit(
                        () -> viewModel.changeAllQtys(lastServesVal, newServes)),
                        new FutureCallback<Object>() {
                            @Override
                            public void onSuccess(@Nullable Object result) {
                                lastServesVal = newServes;
                            }

                            @Override
                            public void onFailure(Throwable t) {

                            }
                        },uiExecutor))
                //otherwise don't do anything
                .setNegativeButton(R.string.no, null)
                .show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void populateRecipeViews(Recipe recipe){
        View root = requireView();
        //set name as action bar title
        pageTitle = recipe.getName();

        ActionBar actionBar = ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar();
        //don't change the title if we've navigated to another tab of the viewpager
        if (!(null != actionBar.getTitle()
                && (
                actionBar.getTitle().equals(requireActivity().getResources().getString(R.string.title_shopping_list)) ||
                        actionBar.getTitle().equals(requireActivity().getResources().getString(R.string.title_meal_plan))
        )
        )){
            actionBar.setTitle(pageTitle);
        }

        //prefill recipe name field
        binding.editTextRecipeName.setText(recipe.getName());

        //number of serves
        binding.editTextServes.setText(Integer.toString(recipe.getServes()));
        binding.textViewServes.setText(Integer.toString(recipe.getServes()));
        lastServesVal = recipe.getServes();

        //prep and cook times
        binding.editTextPrepTime.setText(Integer.toString(recipe.getPrepTime()));
        binding.textViewPrepTime.setText(Integer.toString(recipe.getPrepTime()));

        binding.editTextCookTime.setText(Integer.toString(recipe.getCookTime()));
        binding.textViewCookTime.setText(Integer.toString(recipe.getCookTime()));

        //website link
        if (null != recipe.getUrl() && !recipe.getUrl().isEmpty()){
            binding.recipeUrlButton.setText(R.string.view_recipe_website_button);
            binding.recipeUrlButton.setOnClickListener(view -> {
                Uri uri = Uri.parse(recipe.getUrl());
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(launchBrowser);
            });
        }
        else{
            binding.recipeUrlButton.setText(getString(R.string.default_url_button_text));
            binding.recipeUrlButton.setOnClickListener(null);
        }

        //ratings
        binding.tiernanRatingBar.setRating(((float) recipe.getTier_rating()) / 2f);
        binding.tomRatingBar.setRating(((float) recipe.getTom_rating()) / 2f);

        //url field
        if(null != recipe.getUrl()){
            binding.editTextUrl.setText(recipe.getUrl());
        }

        //notes
        if (null != recipe.getNotes() && !recipe.getNotes().isEmpty()){
            binding.recipeNotes.setText(recipe.getNotes());
            binding.editTextRecipeNotes.setText(recipe.getNotes());
        }
        else{
            binding.recipeNotes.setText(getString(R.string.default_notes_text));
        }
    }

    private void addTag(Tag tag){
        //add a tag
        binding.recipeTags.post(() -> {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, null, false);
            chip.setText(tag.getName());

            //show close icon only if we are in editing mode
            chip.setCloseIconVisible(editingFlag);


            chip.setOnCloseIconClickListener((view -> {
                viewModel.deleteTag(tag);
                binding.recipeTags.removeView(view);
            }));

            binding.recipeTags.addView(chip);
        });
    }

    /**
     * Adds the ingredient/s (separated by newlines) in the edit_text_ingredient
     * textview to this recipe.
     */
    private void addIngredients(){
        String inputText = binding.editTextIngredient.getText().toString();
        Log.d("TEST", "inputText: " + inputText);

        if (!(inputText.isEmpty())){
            //split input in case of multiple lines
            String[] items = inputText.split("(\\r\\n|\\r|\\n)");

            //send all items to viewModel to be processed/stored
            Futures.addCallback(
                backgroundExecutor.submit(() -> viewModel.addIngredientsToRecipe(items)),
                new FutureCallback<Boolean>() {
                    @Override
                    public void onSuccess(@Nullable Boolean result) {
                        //clear new item input
                        binding.editTextIngredient.setText("");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if(t instanceof InterruptedException) {
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
        //if nothing was entered, then simply display an error message instead
        else{
            Toast.makeText(this.getContext(), "No ingredient entered", Toast.LENGTH_LONG).show();
        }
    }

    private void enterEditMode(){
        editingFlag = true;
        //save backups of current state of chips, ingredients in case changes are to be discarded
        backgroundExecutor.submit(() -> viewModel.saveBackupOfRecipe(recipeId));

        //start edit mode - replacing the action bar with edit mode bar
        if (actionMode == null){
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }

        //set the title to tell user they're editing the recipe
        actionMode.setTitle(R.string.edit_mode_title);
        actionMode.invalidate();

        //show recipe name field
        binding.editTextRecipeName.setVisibility(View.VISIBLE);

        //hide add-to buttons
        binding.actionButtonsContainer.setVisibility(View.GONE);

        //show new ingredient field, button and editing hint
        binding.recipeAddIngredientButton.setVisibility(View.VISIBLE);
        binding.editTextIngredient.setVisibility(View.VISIBLE);
        binding.editIngredientsHint.setVisibility(View.VISIBLE);

        //show add tag field and button
        binding.addTagButton.setVisibility(View.VISIBLE);
        binding.editTextTag.setVisibility(View.VISIBLE);

        //show delete tag icons
        for (int i = 0; i < binding.recipeTags.getChildCount(); i++){
            Chip chip = (Chip) binding.recipeTags.getChildAt(i);
            chip.setCloseIconVisible(true);
        }

        //swap serves textView to editText
        binding.editTextServes.setVisibility(View.VISIBLE);
        binding.textViewServes.setVisibility(View.GONE);

        //swap prep and cook time textViews to editTexts
        binding.editTextPrepTime.setVisibility(View.VISIBLE);
        binding.textViewPrepTime.setVisibility(View.GONE);

        binding.editTextCookTime.setVisibility(View.VISIBLE);
        binding.textViewCookTime.setVisibility(View.GONE);

        //enable ratings bars
        binding.tiernanRatingBar.setIsIndicator(false);
        binding.tomRatingBar.setIsIndicator(false);

        //swap url button for field/title
        binding.urlEditorContainer.setVisibility(View.VISIBLE);
        binding.recipeUrlButton.setVisibility(View.GONE);

        //swap notes textView for editText
        binding.editTextRecipeNotes.setVisibility(View.VISIBLE);
        binding.recipeNotes.setVisibility(View.GONE);

        //show per-ingredient delete icons
        adapter.setEditMode(true);
        binding.recipeIngredientsList.setAdapter(adapter);

        //swap placeholder text for no ingredients
        binding.textviewNoIngredientsPlaceholder.setText(R.string.recipe_no_ingredients_placeholder_edit_mode);
    }

    private void enterViewMode(){
        editingFlag = false;

        //hide keyboard in case it was open
        KeyboardHider.hideKeyboard(requireActivity());

        //hide recipe name field
        binding.editTextRecipeName.setVisibility(View.GONE);

        //show add-to buttons
        binding.actionButtonsContainer.setVisibility(View.VISIBLE);

        //hide new ingredient field, button and hint
        binding.recipeAddIngredientButton.setVisibility(View.GONE);
        binding.editTextIngredient.setVisibility(View.GONE);
        binding.editIngredientsHint.setVisibility(View.GONE);

        //show add tag field and button
        binding.addTagButton.setVisibility(View.GONE);
        binding.editTextTag.setVisibility(View.GONE);

        //hide delete tag icons
        for (int i = 0; i < binding.recipeTags.getChildCount(); i++){
            Chip chip = (Chip) binding.recipeTags.getChildAt(i);
            chip.setCloseIconVisible(false);
        }

        //swap serves editText to textview
        binding.editTextServes.setVisibility(View.GONE);
        binding.textViewServes.setVisibility(View.VISIBLE);

        //swap prep and cook time editTexts to textViews
        binding.editTextPrepTime.setVisibility(View.GONE);
        binding.textViewPrepTime.setVisibility(View.VISIBLE);

        binding.editTextCookTime.setVisibility(View.GONE);
        binding.textViewCookTime.setVisibility(View.VISIBLE);

        //disable ratings bars
        binding.tiernanRatingBar.setIsIndicator(true);
        binding.tomRatingBar.setIsIndicator(true);

        //swap url field/title for button
        binding.urlEditorContainer.setVisibility(View.GONE);
        binding.recipeUrlButton.setVisibility(View.VISIBLE);

        //swap notes editText for textView
        binding.editTextRecipeNotes.setVisibility(View.GONE);
        binding.recipeNotes.setVisibility(View.VISIBLE);

        //hide per-ingredient delete icons and reset selections
        adapter.setEditMode(false);
        adapter.resetSelections();
        binding.recipeIngredientsList.setAdapter(adapter);

        //swap placeholder text for no ingredients
        binding.textviewNoIngredientsPlaceholder.setText(R.string.recipe_no_ingredients_placeholder_view_mode);
    }

    /**
     * Read the contents of the form and compile and update the corresponding edited Recipe
     * object in the database with the form values.
     */
    private void saveRecipe(){
        Recipe recipe = currentRecipe.getValue();

        /* Read all fields */
        String recipeName = binding.editTextRecipeName.getText().toString();
        //read website link
        String url = binding.editTextUrl.getText().toString();

        //read number of serves
        String serves = binding.editTextServes.getText().toString();

        //read prep time if provided
        String prepTime = binding.editTextPrepTime.getText().toString();

        //read cook time if provided
        String cookTime = binding.editTextCookTime.getText().toString();

        //read notes
        String notes = binding.editTextRecipeNotes.getText().toString();

        //read ratings - * 2 and cast to int so we can store half stars as ints
        int tierRating = (int) (binding.tiernanRatingBar.getRating() * 2);
        int tomRating = (int) (binding.tomRatingBar.getRating() * 2);

        //ingredients and tags are already saved, and so don't need to be read

        /* VALIDATION */
        try{
            UrlValidator urlValidator = new UrlValidator();
            //check that a recipe name was entered
            if (recipeName.isEmpty()){
                cancelSave(getString(R.string.error_no_recipe_name_entered),binding.editTextRecipeName);
            }

            //check that the name is unique if it has been changed
            else if (!recipeName.equals(recipe.getName()) && !viewModel.recipeNameIsUnique(recipeName)){
                cancelSave(getString(R.string.error_recipe_name_already_used), binding.editTextRecipeName);
            }

            //check that recipe url is a valid url
            else if (!url.isEmpty() && !urlValidator.isValid(url)){
                cancelSave(getString(R.string.recipe_url_invalid), null);
            }

            /* Save recipe if validation passed*/
            else{
                //set all fields to form values
                recipe.setName(recipeName);

                if(!serves.isEmpty()){
                    recipe.setServes(Integer.parseInt(serves));
                }
                else{
                    recipe.setServes(0);
                }

                if(!prepTime.isEmpty()){
                    recipe.setPrepTime(Integer.parseInt(prepTime));
                }
                else{
                    recipe.setPrepTime(0);
                }
                if(!cookTime.isEmpty()){
                    recipe.setCookTime(Integer.parseInt(cookTime));
                }
                else{
                    recipe.setCookTime(0);
                }

                recipe.setTier_rating(tierRating);
                recipe.setTom_rating(tomRating);
                recipe.setUrl(url);
                recipe.setNotes(notes);

                //update the database entry for recipe accordingly
                viewModel.updateRecipe(recipe);

                //set saved flag so recipe is not deleted when exiting fragment if it's a brand new recipe
                saved = true;

                //ends edit mode (if applicable), returning control of action bar back to the fragment
                //and calling onDestroyActionMode
                if (actionMode != null) {
                    actionMode.finish();
                }

                //notify user of success
                Toast.makeText(getContext(),R.string.save_recipe_toast,Toast.LENGTH_SHORT).show();
                enterViewMode();
            }
        } catch (InterruptedException | ExecutionException e) {
            Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
            Log.e(TAG, "during saving recipe: ", e);
        }
    }

    private void cancelSave(String errorMessage, View scrollTo) {
        //display error message to the user
        Toast.makeText(this.getContext(), errorMessage, Toast.LENGTH_LONG).show();

        //restore to action mode if we are validating after user cancelled edit mode
        // but then chose to save changes, only to receive this message.
        if (actionMode == null){
            actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
            actionMode.setTitle(R.string.edit_mode_title);
            actionMode.invalidate();
            callback.setViewpagerTo(MainActivity.RECIPE_LIST_VIEWPAGER_INDEX);
        }

        //scroll to the offending view that prevented saving
        if(scrollTo != null){
            binding.scrollview.post(() -> {
                binding.scrollview.smoothScrollTo(0, scrollTo.getTop());
            });
        }

        ((ActionModeCallback) actionModeCallback).saveClicked = false;
    }

    private void discardChanges() {
        //restore ingredients to previous state
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.resetIngredientsToBackup()),
                new FutureCallback<Boolean>() {
                    @Override
                    public void onSuccess(@Nullable Boolean result) {
                        //do nothing
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                    }
                },
                uiExecutor
        );



        //restore tags to previous state
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.restoreTagsToBackup()),
                new FutureCallback<List<Tag>>() {
                    @Override
                    public void onSuccess(@Nullable List<Tag> tagsToRestoreTo) {
                        //if null returned, it means there's been no change and we don't need to update them
                        if (null != tagsToRestoreTo){
                            //clear all chips
                            binding.recipeTags.removeAllViews();

                            //re-add backup tags
                            for (Tag tag : tagsToRestoreTo){
                                addTag(tag);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                    }
                },
                uiExecutor);

        //restore other views to previous state
        if (null != currentRecipe.getValue()){
            populateRecipeViews(currentRecipe.getValue());
        }

        //revert display to view-only UI
        enterViewMode();
    }

    //hide back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);

        //show back button
        MainActivity activity = (MainActivity) getParentFragment().getActivity();
        if (activity != null) {
            activity.showUpButton();

            //check if we need to redirect to a recipe, and if so, go back to recipe list so we
            //can navigate to the desired recipe
            if(null != sharedViewModel.getNavigateToRecipeId() || null != sharedViewModel.getSelectingForMeal()){
                activity.onBackPressed();
                activity.onBackPressed();
            }
        }

        //set name as action bar title
        ((AppCompatActivity) getParentFragment().getActivity()).getSupportActionBar().setTitle(pageTitle);
    }

    @Override
    public void onPause() {
        setHasOptionsMenu(false);

        //close action bar if user navigates away
        if(null != actionMode){
            actionMode.finish();
        }

        super.onPause();
    }

    /** Merges extra menu items into the default activity action bar, according to provided menu xml */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        requireActivity().invalidateOptionsMenu();
        inflater.inflate(R.menu.view_recipe_action_bar, menu);
    }

    /** Respond to menu items from action bar being pressed */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //back button pressed
            case android.R.id.home:
                ((MainActivity) requireActivity()).onBackPressed();
                return true;

            //edit button pressed
            case R.id.action_edit_recipe:
                //modify UI to edit mode
                enterEditMode();
                return true;

//            case R.id.action_add_all_to_list:
//                promptSendIngredientsToShoppingList();
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void promptSendIngredientsToShoppingList() {
        //prompt for confirmation first
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_all_ingredients_dialog_title)
                .setMessage(R.string.add_all_ingredients_dialog)
                .setPositiveButton(R.string.add_all_ingredients_dialog_positive_button, (dialogInterface, i) -> {
                    sendIngredientsToShoppingList();
                })
                //otherwise don't do anything
                .setNegativeButton(R.string.add_all_ingredients_dialog_negative_button, null)
                .show();
    }

    private void sendIngredientsToShoppingList(){
        Futures.addCallback(
                backgroundExecutor.submit(() ->
                        viewModel.addIngredientsToShoppingList(adapter.getSelectedIngredients())),
                new FutureCallback<Boolean>() {
                    @Override
                    public void onSuccess(@Nullable Boolean result) {
                        Toast.makeText(getContext(),R.string.add_all_ingredients_toast,Toast.LENGTH_LONG).show();
                        callback.setViewpagerTo(MainActivity.SHOPPING_LIST_VIEWPAGER_INDEX);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(getContext(),R.string.error_sending_ingredients_to_shopping_list,Toast.LENGTH_LONG).show();
                        Log.e(TAG, "sending ingredients to shopping list: ", t);
                    }
                },
                uiExecutor
        );
    }

    private void addRecipeToMealPlan(View v) {
        ViewRecipeFragmentDirections.ActionViewRecipeFragmentToSelectMealFragment action =
                ViewRecipeFragmentDirections.actionViewRecipeFragmentToSelectMealFragment(recipeId, chooseMealPlanId());
        Navigation.findNavController(requireView()).navigate(action);
    }

    private int chooseMealPlanId() {
        //when we have multiple, make this into a spinner to choose which meal plan to add to
        return 1;
    }

    @Override
    public void onDeleteClicked(int position) {
        viewModel.deleteIngredient(ingredients.getValue().get(position));
    }

    @Override
    public void onConfirmEditClicked(int position, String newIngredientText) {
        viewModel.editItem(ingredients.getValue().get(position), newIngredientText);
    }

    @Override
    public void onStop() {
        super.onStop();
        //delete recipe if it was new and user navigated away before saving
        if(newRecipeFlag && !saved){
            deleteRecipe();
        }
        //hide keyboard if it was open
        KeyboardHider.hideKeyboard(requireActivity());
    }

    private void deleteRecipe() {
        //if this was a new recipe, the button should fully delete the recipe being edited
        if (newRecipeFlag){
            Toast.makeText(this.getContext(), "Recipe draft discarded",Toast.LENGTH_LONG).show();
            //delete the recipe from db
            viewModel.deleteRecipe(recipeId);
        }
    }

    /**Creates and handles a contextual action bar for when one or more recipes are selected */
    private class ActionModeCallback implements ActionMode.Callback{
        public boolean saveClicked;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.recipe_editor_action_bar, menu);
            saveClicked = false;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()){
                //Handle clicking of save button
                case R.id.action_save_recipe:
                    //sets this flag to prevent prompt to save/discard changes onActionModeDestroyed
                    saveClicked = true;

                    //save recipe
                    saveRecipe();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //prompt user to save changes if they haven't yet done so
            actionMode = null;
            if(!saveClicked){
                showSavePrompt();
            }
        }
    }

    public void showSavePrompt() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.save_recipe_prompt_title)
                .setMessage(R.string.save_recipe_prompt_message)
                .setPositiveButton(R.string.save_recipe_prompt_positive_button,
                        (DialogInterface.OnClickListener) (dialog, which) -> {
                    saveRecipe();
                })
                .setNegativeButton(R.string.save_recipe_prompt_negative_button, (dialog, which) -> {
                    discardChanges();
                })
                .show();
    }
}


