package com.example.shoppinglistapp2.activities.ui.recipes.creator;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe.ViewRecipeFragmentDirections;

/**
 * A simple {@link Fragment} subclass.
 */
public class CreateRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;

    public CreateRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_create_recipe, container, false);

        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        //setup action bar to allow back button
        this.setHasOptionsMenu(true);

        //setup manual recipe creation button to get a new recipe id for an empty recipe and navigate
        //to editor
        Button manualButton = root.findViewById(R.id.create_recipe_manually_button);
        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //create new empty recipe and navigate to recipe editor screen when "+" icon clicked
                CreateRecipeFragmentDirections.ActionCreateRecipeToRecipeEditorFragment action =
                        CreateRecipeFragmentDirections.actionCreateRecipeToRecipeEditorFragment();
                action.setRecipeId(recipesViewModel.generateNewRecipeId());
                Navigation.findNavController(root).navigate(action);
            }
        });

        return root;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //back button pressed
            case android.R.id.home:
                ((MainActivity) getActivity()).onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}