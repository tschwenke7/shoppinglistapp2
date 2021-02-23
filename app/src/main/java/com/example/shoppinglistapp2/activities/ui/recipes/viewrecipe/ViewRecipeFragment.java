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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListViewModel;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.List;

public class ViewRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;
    private ShoppingListViewModel shoppingListViewModel;
    private int recipeId;
    private LiveData<List<Ingredient>> ingredients;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);
        shoppingListViewModel =
                new ViewModelProvider(getActivity()).get(ShoppingListViewModel.class);

        //retrieve recipe to be edited
        recipeId = ViewRecipeFragmentArgs.fromBundle(getArguments()).getRecipeId();
        Recipe recipe = recipesViewModel.getRecipeById(recipeId);

        //set name as action bar title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(recipe.getName());

        View root = inflater.inflate(R.layout.fragment_view_recipe, container, false);

        //setup action bar
        this.setHasOptionsMenu(true);

        /* fill in textViews with saved recipe data where available */
        //setup ingredient list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_ingredients_list);
        final IngredientListAdapter adapter = new IngredientListAdapter(new IngredientListAdapter.IngredientDiff());
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));


        //prep and cook times
        TextView prepTimeField = root.findViewById(R.id.prep_time);
        prepTimeField.setText(recipe.getPrepTime() + " " +  getString(R.string.abbreviated_time_unit));

        TextView cookTimeField = root.findViewById(R.id.cook_time);
        cookTimeField.setText(recipe.getCookTime() + " " +  getString(R.string.abbreviated_time_unit));

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

        //notes
        TextView notesField = root.findViewById(R.id.recipe_notes);
        if (null != recipe.getNotes() && !recipe.getNotes().isEmpty()){
            notesField.setText(recipe.getNotes());
        }
        else{
            notesField.setText(getString(R.string.default_notes_text));
        }


        //set observer to update ingredient list if it changes
        ingredients = recipesViewModel.getRecipeIngredientsById(recipe.getId());
        ingredients.observe(getViewLifecycleOwner(), currentRecipeIngredients -> {
            adapter.submitList(currentRecipeIngredients);
        });

        return root;
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

    /** Merges extra menu items into the default activity action bar, according to provided menu xml */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.view_recipe_action_bar, menu);
    }

    /** Respond to menu items from action bar being pressed */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //back button pressed
            case android.R.id.home:
                ((MainActivity) getActivity()).onBackPressed();
                return true;

            //edit button pressed
            case R.id.action_edit_recipe:
                //send to editor page with recipeId of the recipe being viewed
                ViewRecipeFragmentDirections.ActionViewRecipeToRecipeEditor action =
                        ViewRecipeFragmentDirections.actionViewRecipeToRecipeEditor();
                action.setRecipeId(recipeId);
                Navigation.findNavController(getView()).navigate(action);
                return true;

            case R.id.action_add_all_to_list:
                //prompt for confirmation first
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.add_all_ingredients_dialog_title)
                        .setMessage(R.string.add_all_ingredients_dialog)
                        .setPositiveButton(R.string.add_all_ingredients_dialog_positive_button, (dialogInterface, i) -> {
                            shoppingListViewModel.addItemsFromRecipe(ingredients.getValue());
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
}


