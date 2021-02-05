package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.activities.ui.recipes.newrecipe.IngredientListAdapter;
import com.example.shoppinglistapp2.db.tables.Recipe;

public class ViewRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        View root = inflater.inflate(R.layout.fragment_view_recipe, container, false);

        //setup action bar
        this.setHasOptionsMenu(true);

        //setup ingredient list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_ingredients_list);
        final IngredientListAdapter adapter = new IngredientListAdapter(new IngredientListAdapter.IngredientDiff());
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //fill in textViews with saved recipe data where available
        Recipe recipe = recipesViewModel.getCurrentRecipe();
        Log.d("TOM_TEST", "url: " + recipe.getUrl());



        //set name as action bar title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(recipe.getName());

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
        recipesViewModel.getCurrentRecipeIngredients().observe(getViewLifecycleOwner(), currentRecipeIngredients -> {

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
}


