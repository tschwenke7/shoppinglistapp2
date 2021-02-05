package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RecipeListFragment extends Fragment implements RecipeListAdapter.OnRecipeClickListener{

    private View root;
    private RecipesViewModel recipesViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        root = inflater.inflate(R.layout.fragment_recipe_list, container, false);

        //setup action bar
        this.setHasOptionsMenu(true);


        //setup recipe list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_recyclerview);
        final RecipeListAdapter adapter = new RecipeListAdapter(this);
        recipeRecyclerView.setAdapter(adapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update recipe list if it changes
        recipesViewModel.getAllRecipes().observe(getViewLifecycleOwner(), recipes -> {
            adapter.setRecipes(recipes);
        });

        return root;
    }

    //hide back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            activity.hideUpButton();
        }
    }

    /** Merges extra menu items into the default activity action bar, according to provided menu xml */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.recipe_list_action_bar, menu);
    }

    /** Handle onClick for the custom action bar menu items for this fragment */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_recipe:  {
                // navigate to new recipe screen when "+" icon clicked
                Navigation.findNavController(root).navigate(R.id.action_recipe_list_to_new_recipe);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onRecipeClick(int position) {
        Log.d("TOM_TEST", "onRecipeClick triggered for item " + position);
        recipesViewModel.setRecipeToView(position);
        Navigation.findNavController(root).navigate(R.id.action_recipe_list_to_view_recipe);
    }
}