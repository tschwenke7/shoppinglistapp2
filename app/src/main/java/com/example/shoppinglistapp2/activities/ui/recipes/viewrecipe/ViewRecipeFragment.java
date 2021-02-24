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
    private boolean editingFlag;
    private LiveData<List<Ingredient>> ingredients;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);
        shoppingListViewModel =
                new ViewModelProvider(getActivity()).get(ShoppingListViewModel.class);

        //retrieve recipe to be viewed
        recipeId = ViewRecipeFragmentArgs.fromBundle(getArguments()).getRecipeId();
        Recipe recipe = recipesViewModel.getRecipeById(recipeId);

        //decide whether to start in edit mode or not
        editingFlag = ViewRecipeFragmentArgs.fromBundle(getArguments()).getEditingFlag();

        View root = inflater.inflate(R.layout.fragment_view_recipe, container, false);

        //setup action bar
        this.setHasOptionsMenu(true);

        /* fill in textViews with saved recipe data where available */
        populateViews(root, recipe);

        if(editingFlag){
            enterEditMode(root);
        }
        else{
            enterViewMode(root);
        }

        return root;
    }

    private void populateViews(View root, Recipe recipe){
        //set name as action bar title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(recipe.getName());

        //prefill recipe name field
        ((TextView) root.findViewById(R.id.edit_text_recipe_name)).setText(recipe.getName());

        //setup ingredient list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_ingredients_list);
        final IngredientListAdapter adapter = new IngredientListAdapter(new IngredientListAdapter.IngredientDiff());
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update ingredient list if it changes
        ingredients = recipesViewModel.getRecipeIngredientsById(recipe.getId());
        ingredients.observe(getViewLifecycleOwner(), currentRecipeIngredients -> {
            adapter.submitList(currentRecipeIngredients);
        });

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

        //url field
        if(null != recipe.getUrl()){
            ((TextView) root.findViewById(R.id.edit_text_url)).setText(recipe.getUrl());
        }

        //notes
        TextView notesField = root.findViewById(R.id.recipe_notes);
        if (null != recipe.getNotes() && !recipe.getNotes().isEmpty()){
            notesField.setText(recipe.getNotes());
        }
        else{
            notesField.setText(getString(R.string.default_notes_text));
        }
    }

    private void enterEditMode(View root){

    }

    private void enterViewMode(View root){
        //hide recipe name field
        root.findViewById(R.id.edit_text_recipe_name).setVisibility(View.GONE);

        //hide new ingredient field and button
        root.findViewById(R.id.recipe_add_ingredient_button).setVisibility(View.GONE);
        root.findViewById(R.id.edit_text_ingredient).setVisibility(View.GONE);

        //swap prep and cook time editTexts to textViews
        TextView prepTimeEditText = root.findViewById(R.id.edit_text_prep_time);
        prepTimeEditText.setVisibility(View.GONE);
        TextView prepTimeTextView = root.findViewById(R.id.text_view_prep_time);
        prepTimeTextView.setVisibility(View.VISIBLE);
        prepTimeTextView.setText(prepTimeEditText.getText());

        TextView cookTimeEditText = root.findViewById(R.id.edit_text_cook_time);
        cookTimeEditText.setVisibility(View.GONE);
        TextView cookTimeTextView = root.findViewById(R.id.text_view_cook_time);
        cookTimeTextView.setVisibility(View.VISIBLE);
        cookTimeTextView.setText(cookTimeEditText.getText());

        //swap url field/title for button
        root.findViewById(R.id.url_editor).setVisibility(View.GONE);
        root.findViewById(R.id.recipe_url_button).setVisibility(View.VISIBLE);

        //cancel action mode, reverting top bar back to normal

        //hide per-ingredient delete icons
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
                //todo - switch to edit mode
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


