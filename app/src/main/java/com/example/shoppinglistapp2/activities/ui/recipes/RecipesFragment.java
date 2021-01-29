package com.example.shoppinglistapp2.activities.ui.recipes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.RecipeListAdapter;
import com.example.shoppinglistapp2.db.SlaViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RecipesFragment extends Fragment {

    private RecipesViewModel recipesViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                ViewModelProviders.of(this).get(RecipesViewModel.class);
        View root = inflater.inflate(R.layout.fragment_recipe_list, container, false);

        //setup recipe list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_recyclerview);
        final RecipeListAdapter adapter = new RecipeListAdapter(new RecipeListAdapter.RecipeDiff());
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //initiate viewModel to manage data
//        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(root.getapp)
//                .create(SlaViewModel.class);
//
//        //set observer to update recipe list if it changes
//        viewModel.getAllRecipes().observe(this, recipes -> {
//            adapter.submitList(recipes);
//        });

        //setup the new recipe button
        FloatingActionButton newRecipeButton = root.findViewById(R.id.new_recipe_button);
        newRecipeButton.setOnClickListener(view -> {
//            Intent intent = new Intent(MainActivity.this)
        });

        return root;
    }
}