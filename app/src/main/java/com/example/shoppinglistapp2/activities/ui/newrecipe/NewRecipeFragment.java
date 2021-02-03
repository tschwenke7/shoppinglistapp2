package com.example.shoppinglistapp2.activities.ui.newrecipe;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.example.shoppinglistapp2.IngredientListAdapter;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.RecipeListAdapter;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class NewRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        View root = inflater.inflate(R.layout.fragment_new_recipe, container, false);

        //setup ingredient list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_ingredients_list);
        final IngredientListAdapter adapter = new IngredientListAdapter(new IngredientListAdapter.IngredientDiff());
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update recipe list if it changes
        recipesViewModel.getNewRecipeIngredients().observe(getViewLifecycleOwner(), newRecipeIngredients -> {
            adapter.submitList(newRecipeIngredients);
            Log.d("TOM_TEST", "Ingredients list observer triggered.");
            for (Ingredient ing: newRecipeIngredients){
                Log.d("TOM_TEST", ing.getName());
            }

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
                saveRecipe(view);
            }
        });

        //handle the cancel recipe button
        Button cancelRecipeButton = root.findViewById(R.id.cancel_recipe_button);
        cancelRecipeButton.setOnClickListener(view -> {
            Toast.makeText(this.getContext(), "Recipe draft discarded",Toast.LENGTH_LONG).show();
            recipesViewModel.clearNewRecipe();//clears the ingredients section
            Navigation.findNavController(view).navigate(R.id.action_save_or_cancel_and_return_to_recipe_list);
        });

        return root;
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
            recipesViewModel.addIngredientToNewRecipe(items);

            //clear new item input
            input.setText("");
        }
        //if nothing was entered, then simply display an error message instead
        else{
            Toast.makeText(this.getContext(), "No ingredient entered", Toast.LENGTH_LONG);
        }
    }

    private void saveRecipe(View view){
        View root = view.getRootView();
        //read the contents of the form  and compile into recipe Object for storage
        Recipe recipe = new Recipe();

        //read name of recipe
        TextView recipeNameField = root.findViewById(R.id.edit_text_recipe_name);
        recipe.setName(recipeNameField.getText().toString());


        //read website link
        TextView urlField = root.findViewById(R.id.edit_text_url);
        recipe.setUrl(urlField.getText().toString());

        //ingredients are already stored in viewModel, and so don't need to be read

        //read prep time if provided
        TextView prepTimeField = root.findViewById(R.id.edit_text_prep_time);
        if(!prepTimeField.getText().toString().isEmpty()){
            recipe.setPrepTime(Integer.parseInt(prepTimeField.getText().toString()));
        }


        //read cook time if provided
        TextView cookTimeField = root.findViewById(R.id.edit_text_cook_time);
        if(!cookTimeField.getText().toString().isEmpty()) {
            recipe.setCookTime(Integer.parseInt(cookTimeField.getText().toString()));
        }

        //read notes
        TextView notesField = root.findViewById(R.id.edit_text_recipe_notes);
        recipe.setUrl(notesField.getText().toString());

        /* VALIDATION */
        //check that a recipe name was entered
        if (null == recipe.getName() || recipe.getName().isEmpty()){
            Toast.makeText(this.getContext(), "Please enter a name for this recipe",Toast.LENGTH_LONG).show();
        }

        //todo - check that recipe name is unique

        //save recipe
        else{
            //save recipe and show error message if it fails
            if(!recipesViewModel.addNewRecipe(recipe)){
                Toast.makeText(getContext(),"Error adding recipe. Please try again later.", Toast.LENGTH_SHORT).show();
            }

            Log.d("TOM_TEST", "Recipe_id: " + recipe.getId());
            //navigate back to main recipe list page, and display success message
            Toast.makeText(this.getContext(), "Recipe \"" + recipe.getName() + "\" saved",Toast.LENGTH_LONG).show();
            recipesViewModel.clearNewRecipe();//clears the ingredients section
            Navigation.findNavController(view).navigate(R.id.action_save_or_cancel_and_return_to_recipe_list);
        }
    }
}
