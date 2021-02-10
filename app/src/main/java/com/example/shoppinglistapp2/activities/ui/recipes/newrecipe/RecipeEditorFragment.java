package com.example.shoppinglistapp2.activities.ui.recipes.newrecipe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;

public class RecipeEditorFragment extends Fragment implements IngredientListEditorAdapter.ItemClickListener {
    private RecipesViewModel recipesViewModel;
    private Recipe currentRecipe;
    private View root;
    private boolean newRecipeFlag;
    private boolean saved = false;

    private static final String TAG = "TOM_TEST";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        root = inflater.inflate(R.layout.fragment_new_recipe, container, false);

        //load the recipe to be edited
        currentRecipe = recipesViewModel.getRecipeById(RecipeEditorFragmentArgs.fromBundle(getArguments()).getRecipeId());
        //read new recipe flag
        newRecipeFlag = RecipeEditorFragmentArgs.fromBundle(getArguments()).getNewRecipeFlag();

        //if the editor is being opened for an existing recipe, prefill the fields with the saved data
        if(!newRecipeFlag){
            populateEditor(root, currentRecipe);
        }
        saved = false;

        //setup action bar
        this.setHasOptionsMenu(true);

        //setup ingredient list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_ingredients_list);
        final IngredientListEditorAdapter adapter = new IngredientListEditorAdapter(new IngredientListEditorAdapter.IngredientDiff(), this);
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update ingredient list when it changes
        recipesViewModel.getRecipeIngredientsById(currentRecipe.getId())
                .observe(getViewLifecycleOwner(), currentRecipeIngredients -> {
            adapter.submitList(currentRecipeIngredients);
        });

        //handle ingredient being added
        Button addIngredientButton = root.findViewById(R.id.recipe_add_ingredient_button);
        addIngredientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addIngredients(view);
            }
        });

        //handle the save recipe button
        Button saveRecipeButton = root.findViewById(R.id.save_recipe_button);
        saveRecipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRecipe();
                //if this was a new recipe, return to recipe list
                if(newRecipeFlag){
                    Toast.makeText(view.getContext(), "Recipe created successfully",Toast.LENGTH_LONG).show();
                    Navigation.findNavController(view).navigate(R.id.action_save_or_cancel_and_return_to_recipe_list);
                }
                //if it was an existing recipe, return to its viewRecipe
                else{
                    Toast.makeText(view.getContext(), "Recipe updated",Toast.LENGTH_LONG).show();
                    Navigation.findNavController(view).navigate(R.id.action_recipe_editor_to_view_recipe);
                }
            }
        });

        //handle the cancel recipe button
        Button cancelRecipeButton = root.findViewById(R.id.cancel_recipe_button);
        cancelRecipeButton.setOnClickListener(view -> {
            deleteRecipe();

            //navigate back to recipe list
            Navigation.findNavController(view).navigate(R.id.action_save_or_cancel_and_return_to_recipe_list);
        });

        return root;
    }

    private void populateEditor(View root, Recipe currentRecipe) {
        //name
        ((TextView) root.findViewById(R.id.recipe_name)).setText(currentRecipe.getName());

        //prep and cook time
        ((TextView) root.findViewById(R.id.prep_time)).setText(currentRecipe.getPrepTime());
        ((TextView) root.findViewById(R.id.cook_time)).setText(currentRecipe.getCookTime());

        //ingredients taken care of by recyclerview and currentRecipeIngredients LiveData
        //url
        if(null != currentRecipe.getUrl() && !currentRecipe.getUrl().isEmpty()){
            ((TextView) root.findViewById(R.id.edit_text_url)).setText(currentRecipe.getUrl());
        }

        //notes
        if(null != currentRecipe.getNotes() && !currentRecipe.getNotes().isEmpty()){
            ((TextView) root.findViewById(R.id.recipe_notes)).setText(currentRecipe.getNotes());
        }
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
            Toast.makeText(this.getContext(), "No ingredient entered", Toast.LENGTH_LONG);
        }
    }

    /**
     * Read the contents of the form and compile and update the corresponding edited Recipe
     * object in the database with the form values.
     */
    private void saveRecipe(){

        /* Read all fields */
        String recipeName = ((TextView) root.findViewById(R.id.edit_text_recipe_name))
                .getText().toString();
        //read website link
        String url = ((TextView)  root.findViewById(R.id.edit_text_url)).getText().toString();

        //read prep time if provided
        String prepTime = ((TextView) root.findViewById(R.id.prep_time)).getText().toString();

        //read cook time if provided
        String cookTime = ((TextView) root.findViewById(R.id.cook_time)).getText().toString();


        //read notes
        String notes = ((TextView) root.findViewById(R.id.recipe_notes)).getText().toString();

        //ingredients are already saved, and so don't need to be read

        /* VALIDATION */
        //check that a recipe name was entered
        if (null == recipeName || recipeName.isEmpty()){
            Toast.makeText(this.getContext(), "Please enter a name for this recipe",Toast.LENGTH_LONG).show();
        }

        //check that the name is unique
        else if (!recipesViewModel.recipeNameIsUnique(recipeName)){
            Toast.makeText(this.getContext(), "This recipe name is already in use - please choose another",Toast.LENGTH_LONG).show();
        }

        /* Save recipe if validation passed*/
        else{
            //set all fields to form values
            currentRecipe.setName(recipeName);
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
            currentRecipe.setUrl(url);
            currentRecipe.setNotes(notes);

            //update the database entry for recipe accordingly
            recipesViewModel.updateRecipe(currentRecipe);
            //set saved flag so recipe is not deleted when exiting fragment if its a brand new recipe
            saved = true;
        }
    }

    private void deleteRecipe() {
        //if this was a new recipe, the button should fully delete the recipe being edited
        if (newRecipeFlag){
            Toast.makeText(this.getContext(), "Recipe draft discarded",Toast.LENGTH_LONG).show();
            //delete the recipe from db
            recipesViewModel.deleteRecipes(currentRecipe);
        }

    }

    //hide back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            activity.showUpButton();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(newRecipeFlag && !saved){
            deleteRecipe();
        }
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if(newRecipeFlag){
//            saveRecipe();
//        }
//    }

    /** Respond to menu items from action bar being pressed */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            case android.R.id.home:
                ((MainActivity) getActivity()).onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDeleteClicked(int position) {

    }
}
