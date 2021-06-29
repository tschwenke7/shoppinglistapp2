package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListFragmentDirections;
import com.example.shoppinglistapp2.helpers.KeyboardHider;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListViewModel;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ViewRecipeFragment extends Fragment implements IngredientListAdapter.IngredientClickListener {
    private RecipesViewModel recipesViewModel;
    private ShoppingListViewModel shoppingListViewModel;
    private int recipeId;
    private boolean editingFlag;
    private boolean newRecipeFlag;
    private Recipe currentRecipe;
    private boolean saved;
    private LiveData<List<Ingredient>> ingredients;
    private ActionMode actionMode;
    private ActionMode.Callback actionModeCallback = new ViewRecipeFragment.ActionModeCallback();
    private IngredientListAdapter adapter;
    private RecyclerView ingredientRecyclerView;
    private String pageTitle;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);
        shoppingListViewModel =
                new ViewModelProvider(getActivity()).get(ShoppingListViewModel.class);

        //retrieve navigation args
        //recipe to be viewed
        recipeId = ViewRecipeFragmentArgs.fromBundle(getArguments()).getRecipeId();
        currentRecipe = recipesViewModel.getRecipeById(recipeId);
        //decide whether to start in edit mode or not
        editingFlag = ViewRecipeFragmentArgs.fromBundle(getArguments()).getEditingFlag();
        //true if this recipe was just created rather than selected from recipe list
        newRecipeFlag = ViewRecipeFragmentArgs.fromBundle(getArguments()).getNewRecipeFlag();

        View root = inflater.inflate(R.layout.fragment_view_recipe, container, false);

        saved = false;

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
    }

    private void setupViews(View root){
        //setup action bar
        this.setHasOptionsMenu(true);

        /* fill in textViews with saved recipe data where available */
        populateViews(root, currentRecipe);

        //setup ingredient list recyclerview
        ingredientRecyclerView = root.findViewById(R.id.recipe_ingredients_list);
        //set observer to update ingredient list if it changes
        ingredients = recipesViewModel.getRecipeIngredientsById(recipeId);
        adapter = new IngredientListAdapter(this);
        ingredientRecyclerView.setAdapter(adapter);
        ingredientRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        ingredients.observe(getViewLifecycleOwner(), (list) -> adapter.setList(list));

        //handle ingredient being added
        Button addIngredientButton = root.findViewById(R.id.recipe_add_ingredient_button);
        addIngredientButton.setOnClickListener(view -> addIngredients(view));

        //add existing tags
        for (String tag : recipesViewModel.getTagsByRecipe(recipeId)){
            addTag(root, tag);
        }

        //handle new tag being added
        Button addTagButton = root.findViewById(R.id.add_tag_button);
        addTagButton.setOnClickListener((view) -> {
            TextView tagField =  root.findViewById(R.id.edit_text_tag);
            String tagName = tagField.getText().toString();
            if (!tagName.isEmpty()){
                //persist to db
                recipesViewModel.insertTag(recipeId, tagName);
                //add tag chip to ui
                addTag(root, tagName);

                //clear input field
                tagField.setText("");
            }
        });

        //setup tag input autocomplete
        AutoCompleteTextView tagField = root.findViewById(R.id.edit_text_tag);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_dropdown_item_1line,recipesViewModel.getAllTags());
        tagField.setAdapter(adapter);

        //configure either in edit mode or view only mode
        if(editingFlag){
            enterEditMode(root);
        }
        else{
            enterViewMode(root);
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

    private void populateViews(View root, Recipe recipe){
        //set name as action bar title
        pageTitle = recipe.getName();
        ((AppCompatActivity) getParentFragment().getActivity()).getSupportActionBar().setTitle(pageTitle);

        //prefill recipe name field
        ((TextView) root.findViewById(R.id.edit_text_recipe_name)).setText(recipe.getName());

        //number of serves
        ((TextView) root.findViewById(R.id.edit_text_serves)).setText(Integer.toString(recipe.getServes()));
        ((TextView) root.findViewById(R.id.text_view_serves)).setText(Integer.toString(recipe.getServes()));

        //prep and cook times
        ((TextView) root.findViewById(R.id.edit_text_prep_time)).setText(Integer.toString(recipe.getPrepTime()));
        ((TextView) root.findViewById(R.id.text_view_prep_time)).setText(Integer.toString(recipe.getPrepTime()));

        ((TextView) root.findViewById(R.id.edit_text_cook_time)).setText(Integer.toString(recipe.getCookTime()));
        ((TextView) root.findViewById(R.id.text_view_cook_time)).setText(Integer.toString(recipe.getCookTime()));

        //website link
        Button websiteButton = root.findViewById(R.id.recipe_url_button);
        if (null != recipe.getUrl() && !recipe.getUrl().isEmpty()){
            websiteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri uri = Uri.parse(recipe.getUrl());
                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(launchBrowser);
                }
            });
        }
        else{
            websiteButton.setText(getString(R.string.default_url_button_text));
        }

        //ratings
        ((RatingBar) root.findViewById(R.id.tiernan_rating_bar)).setRating(((float) currentRecipe.getTier_rating()) / 2f);
        ((RatingBar) root.findViewById(R.id.tom_rating_bar)).setRating(((float) currentRecipe.getTom_rating()) / 2f);

        //url field
        if(null != recipe.getUrl()){
            ((TextView) root.findViewById(R.id.edit_text_url)).setText(recipe.getUrl());
        }

        //notes
        TextView notesField = root.findViewById(R.id.recipe_notes);
        TextView editNotesField = root.findViewById(R.id.edit_text_recipe_notes);
        if (null != recipe.getNotes() && !recipe.getNotes().isEmpty()){
            notesField.setText(recipe.getNotes());
            editNotesField.setText(recipe.getNotes());
        }
        else{
            notesField.setText(getString(R.string.default_notes_text));
        }
    }

    private void addTag(View root, String tagName){

        //add a sample tag
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, null, false);
        chip.setText(tagName);
        ChipGroup chipGroup = root.findViewById(R.id.recipe_tags);

        chip.setOnCloseIconClickListener((view -> {
            recipesViewModel.deleteTag(recipeId, tagName);
            chipGroup.removeView(view);
        }));

        chipGroup.addView(chip);
    }

    /**
     * Adds the ingredient/s (separated by newlines) in the edit_text_ingredient
     * textview to this recipe.
     * @param view
     */
    private void addIngredients(View view){
        EditText input = view.getRootView().findViewById(R.id.edit_text_ingredient);
        String inputText = input.getText().toString();
        Log.d("TEST", "inputText: " + inputText);

        if (!(inputText.isEmpty())){
            //split input in case of multiple lines
            String[] items = inputText.split("(\\r\\n|\\r|\\n)");

            //send all items to viewModel to be processed/stored
            recipesViewModel.addIngredientsToRecipe(currentRecipe.getId(), items);

            //clear new item input
            input.setText("");
        }
        //if nothing was entered, then simply display an error message instead
        else{
            Toast.makeText(this.getContext(), "No ingredient entered", Toast.LENGTH_LONG).show();
        }
    }

    private void enterEditMode(View root){
        //start edit mode - replacing the action bar with edit mode bar
        if (actionMode == null){
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }

        //set the title to tell user they're editing the recipe
        actionMode.setTitle(R.string.edit_mode_title);
        actionMode.invalidate();

        //show recipe name field
        root.findViewById(R.id.edit_text_recipe_name).setVisibility(View.VISIBLE);

        //show new ingredient field and button
        root.findViewById(R.id.recipe_add_ingredient_button).setVisibility(View.VISIBLE);
        root.findViewById(R.id.edit_text_ingredient).setVisibility(View.VISIBLE);

        //show add tag field and button
        root.findViewById(R.id.add_tag_button).setVisibility(View.VISIBLE);
        root.findViewById(R.id.edit_text_tag).setVisibility(View.VISIBLE);

        //show delete tag icons
        ChipGroup tagContainer = root.findViewById(R.id.recipe_tags);
        for (int i = 0; i < tagContainer.getChildCount(); i++){
            Chip chip = (Chip) tagContainer.getChildAt(i);
            chip.setCloseIconVisible(true);
        }

        //swap serves textView to editText
        root.findViewById(R.id.edit_text_serves).setVisibility(View.VISIBLE);
        root.findViewById(R.id.text_view_serves).setVisibility(View.GONE);

        //swap prep and cook time textViews to editTexts
        root.findViewById(R.id.edit_text_prep_time).setVisibility(View.VISIBLE);
        root.findViewById(R.id.text_view_prep_time).setVisibility(View.GONE);

        root.findViewById(R.id.edit_text_cook_time).setVisibility(View.VISIBLE);
        root.findViewById(R.id.text_view_cook_time).setVisibility(View.GONE);

        //enable ratings bars
        ((RatingBar) root.findViewById(R.id.tiernan_rating_bar)).setIsIndicator(false);
        ((RatingBar) root.findViewById(R.id.tom_rating_bar)).setIsIndicator(false);

        //swap url button for field/title
        root.findViewById(R.id.url_editor_container).setVisibility(View.VISIBLE);
        root.findViewById(R.id.recipe_url_button).setVisibility(View.GONE);

        //swap notes textView for editText
        root.findViewById(R.id.edit_text_recipe_notes).setVisibility(View.VISIBLE);
        root.findViewById(R.id.recipe_notes).setVisibility(View.GONE);

        //show per-ingredient delete icons
        adapter.setEditMode(true);
        ingredientRecyclerView.setAdapter(adapter);

    }

    private void enterViewMode(View root){
        //hide recipe name field
        root.findViewById(R.id.edit_text_recipe_name).setVisibility(View.GONE);

        //hide new ingredient field and button
        root.findViewById(R.id.recipe_add_ingredient_button).setVisibility(View.GONE);
        root.findViewById(R.id.edit_text_ingredient).setVisibility(View.GONE);

        //show add tag field and button
        root.findViewById(R.id.add_tag_button).setVisibility(View.GONE);
        root.findViewById(R.id.edit_text_tag).setVisibility(View.GONE);

        //hide delete tag icons
        ChipGroup tagContainer = root.findViewById(R.id.recipe_tags);
        for (int i = 0; i < tagContainer.getChildCount(); i++){
            Chip chip = (Chip) tagContainer.getChildAt(i);
            chip.setCloseIconVisible(false);
        }

        //swap serves editText to textview
        root.findViewById(R.id.edit_text_serves).setVisibility(View.GONE);
        root.findViewById(R.id.text_view_serves).setVisibility(View.VISIBLE);

        //swap prep and cook time editTexts to textViews
        TextView prepTimeEditText = root.findViewById(R.id.edit_text_prep_time);
        prepTimeEditText.setVisibility(View.GONE);
        TextView prepTimeTextView = root.findViewById(R.id.text_view_prep_time);
        prepTimeTextView.setVisibility(View.VISIBLE);

        TextView cookTimeEditText = root.findViewById(R.id.edit_text_cook_time);
        cookTimeEditText.setVisibility(View.GONE);
        TextView cookTimeTextView = root.findViewById(R.id.text_view_cook_time);
        cookTimeTextView.setVisibility(View.VISIBLE);

        //disable ratings bars
        ((RatingBar) root.findViewById(R.id.tiernan_rating_bar)).setIsIndicator(true);
        ((RatingBar) root.findViewById(R.id.tom_rating_bar)).setIsIndicator(true);

        //swap url field/title for button
        root.findViewById(R.id.url_editor_container).setVisibility(View.GONE);
        root.findViewById(R.id.recipe_url_button).setVisibility(View.VISIBLE);

        //swap notes editText for textView
        TextView notesEditText = root.findViewById(R.id.edit_text_recipe_notes);
        notesEditText.setVisibility(View.GONE);
        TextView notesTextView = root.findViewById(R.id.recipe_notes);
        notesTextView.setVisibility(View.VISIBLE);

        //hide per-ingredient delete icons and reset selections
        adapter.setEditMode(false);
        adapter.resetSelections();
        ingredientRecyclerView.setAdapter(adapter);
    }

    /**
     * Read the contents of the form and compile and update the corresponding edited Recipe
     * object in the database with the form values.
     */
    private void saveRecipe(){
        View root = getView();

        /* Read all fields */
        String recipeName = ((TextView) root.findViewById(R.id.edit_text_recipe_name))
                .getText().toString();
        //read website link
        String url = ((TextView)  root.findViewById(R.id.edit_text_url)).getText().toString();

        //read number of serves
        String serves = ((TextView) root.findViewById(R.id.edit_text_serves)).getText().toString();

        //read prep time if provided
        String prepTime = ((TextView) root.findViewById(R.id.edit_text_prep_time)).getText().toString();

        //read cook time if provided
        String cookTime = ((TextView) root.findViewById(R.id.edit_text_cook_time)).getText().toString();

        //read notes
        String notes = ((TextView) root.findViewById(R.id.edit_text_recipe_notes)).getText().toString();

        //read ratings - * 2 and cast to int so we can store half stars as ints
        int tierRating = (int) (((RatingBar) root.findViewById(R.id.tiernan_rating_bar)).getRating() * 2);
        int tomRating = (int) (((RatingBar) root.findViewById(R.id.tom_rating_bar)).getRating() * 2);

        //ingredients are already saved, and so don't need to be read

        /* VALIDATION */

        //check that a recipe name was entered
        if (recipeName.isEmpty()){
            Toast.makeText(this.getContext(), "Please enter a name for this recipe",Toast.LENGTH_LONG).show();
        }

        //check that the name is unique if it has been changed
        else if (!recipeName.equals(currentRecipe.getName()) && !recipesViewModel.recipeNameIsUnique(recipeName)){
            Toast.makeText(this.getContext(), "This recipe name is already in use - please choose another",Toast.LENGTH_LONG).show();
        }

        /* Save recipe if validation passed*/
        else{
            //set all fields to form values
            currentRecipe.setName(recipeName);

            if(!serves.isEmpty()){
                currentRecipe.setServes(Integer.parseInt(serves));
            }
            else{
                currentRecipe.setServes(0);
            }

            if(!prepTime.isEmpty()){
                currentRecipe.setPrepTime(Integer.parseInt(prepTime));
            }
            else{
                currentRecipe.setPrepTime(0);
            }
            if(!cookTime.isEmpty()){
                currentRecipe.setCookTime(Integer.parseInt(cookTime));
            }
            else{
                currentRecipe.setCookTime(0);
            }

            currentRecipe.setTier_rating(tierRating);
            currentRecipe.setTom_rating(tomRating);
            currentRecipe.setUrl(url);
            currentRecipe.setNotes(notes);

            //update the database entry for recipe accordingly
            recipesViewModel.updateRecipe(currentRecipe);

            //set saved flag so recipe is not deleted when exiting fragment if its a brand new recipe
            saved = true;

            //update any fields with their newly saved values
            populateViews(getView(), currentRecipe);

            //hide keyboard in case it was open
            KeyboardHider.hideKeyboard(getActivity());
        }
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
            if(null != recipesViewModel.getNavigateToRecipeId()){
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
                enterEditMode(getView());
                return true;

            case R.id.action_add_all_to_list:
                //prompt for confirmation first
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.add_all_ingredients_dialog_title)
                        .setMessage(R.string.add_all_ingredients_dialog)
                        .setPositiveButton(R.string.add_all_ingredients_dialog_positive_button, (dialogInterface, i) -> {
                            shoppingListViewModel.addIngredientsToShoppingList(adapter.getSelectedIngredients());
                            Toast.makeText(getContext(),R.string.add_all_ingredients_toast,Toast.LENGTH_LONG).show();
                        })
                        //otherwise don't do anything
                        .setNegativeButton(R.string.add_all_ingredients_dialog_negative_button, null)
                        .show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDeleteClicked(int position) {
        recipesViewModel.deleteIngredients(ingredients.getValue().get(position));
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
            recipesViewModel.deleteRecipes(currentRecipe);
        }
    }

    /**Creates and handles a contextual action bar for when one or more recipes are selected */
    private class ActionModeCallback implements ActionMode.Callback{

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.recipe_editor_action_bar, menu);
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
                    //ends the actionmode, returning control of action bar back to the fragment
                    //and calling onDestroyActionMode
                    actionMode.finish();
                    //save recipe
                    saveRecipe();
                    //notify user
                    Toast.makeText(getContext(),R.string.save_recipe_toat,Toast.LENGTH_SHORT).show();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            //revert display to view-only UI
            enterViewMode(requireView());
            //hide keyboard if it was open
            KeyboardHider.hideKeyboard(requireActivity());
        }
    }
}


