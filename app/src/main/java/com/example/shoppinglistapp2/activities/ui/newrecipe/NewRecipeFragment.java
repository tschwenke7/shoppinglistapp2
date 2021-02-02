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
        saveRecipeButton.setOnClickListener(view -> {
            //read the contents of the form  and compile into recipe Object for storage
            Recipe recipe = new Recipe();

            //read name of recipe
            TextView recipeNameField = root.findViewById(R.id.edit_text_recipe_name);
            recipe.setName(recipeNameField.getText().toString());

            //validate that fields were entered correctly
            if (null == recipe.getName()){
                Toast.makeText(this.getContext(), "Please enter a name for this recipe",Toast.LENGTH_LONG).show();
            }

            //save recipe, navigate back to main recipe list page, and display success message
            else{
                Toast.makeText(this.getContext(), "Recipe \"" + recipe.getName() + "\" saved",Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).navigate(R.id.action_save_or_cancel_and_return_to_recipe_list);
            }
        });

        //handle the cancel recipe button
        Button cancelRecipeButton = root.findViewById(R.id.cancel_recipe_button);
        cancelRecipeButton.setOnClickListener(view -> {
            Toast.makeText(this.getContext(), "Recipe draft discarded",Toast.LENGTH_LONG).show();
            Navigation.findNavController(view).navigate(R.id.action_save_or_cancel_and_return_to_recipe_list);
        });

        return root;
    }

    private void addIngredients(View view){
        EditText input = view.getRootView().findViewById(R.id.edit_text_ingredient);
        String inputText = input.getText().toString();
        Log.d("TEST", "inputText: " + inputText);

        if (!(inputText.isEmpty())){
            //split input in case of multiple lines
            String[] items = inputText.split("(\\r\\n|\\r|\\n)");
            Log.d("TOM_TEST", "items (split): " + items.toString());

            recipesViewModel.addIngredientToNewRecipe(items);

            //clear new item input
            input.setText("");
        }
        else{
            Toast.makeText(this.getContext(), "No ingredient entered", Toast.LENGTH_LONG);
        }
    }
}
