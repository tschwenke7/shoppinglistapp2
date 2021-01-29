package com.example.shoppinglistapp2.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.RecipeListAdapter;
import com.example.shoppinglistapp2.db.SlaViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private SlaViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipes);

        //setup recipe list recyclerview
        RecyclerView recipeRecyclerView = findViewById(R.id.recipe_recyclerview);
        final RecipeListAdapter adapter = new RecipeListAdapter(new RecipeListAdapter.RecipeDiff());
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //initiate viewModel to manage data
        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())
                .create(SlaViewModel.class);

        //set observer to update recipe list if it changes
        viewModel.getAllRecipes().observe(this, recipes -> {
            adapter.submitList(recipes);
        });

        //setup the new recipe button
        FloatingActionButton newRecipeButton = findViewById(R.id.new_recipe_button);
        newRecipeButton.setOnClickListener(view -> {
//            Intent intent = new Intent(MainActivity.this)
        });
    }
}