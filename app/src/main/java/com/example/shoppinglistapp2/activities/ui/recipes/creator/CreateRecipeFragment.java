package com.example.shoppinglistapp2.activities.ui.recipes.creator;

import android.os.AsyncTask;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe.ViewRecipeFragmentDirections;
import com.example.shoppinglistapp2.helpers.RecipeWebsiteUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A simple {@link Fragment} subclass.
 */
public class CreateRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;
    private View root;
    private ExecutorService websiteExecutor = Executors.newSingleThreadExecutor();

    public CreateRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_create_recipe, container, false);

        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        //setup action bar to allow back button
        this.setHasOptionsMenu(true);

        //setup "create recipe from website" button
        Button websiteButton = root.findViewById(R.id.create_recipe_from_website_button);
        websiteButton.setOnClickListener(view -> {
            onWebsiteButtonClicked(view);
        });

        //setup manual recipe creation button to get a new recipe id for an empty recipe and navigate
        //to editor
        Button manualButton = root.findViewById(R.id.create_recipe_manually_button);
        manualButton.setOnClickListener(view -> {
            //create new empty recipe and navigate to recipe editor screen when "+" icon clicked
            CreateRecipeFragmentDirections.ActionCreateRecipeToRecipeEditorFragment action =
                    CreateRecipeFragmentDirections.actionCreateRecipeToRecipeEditorFragment();
            action.setRecipeId(recipesViewModel.generateNewRecipeId());
            Navigation.findNavController(root).navigate(action);
        });

        return root;
    }

    private void onWebsiteButtonClicked(View view){
        String url = ((EditText) root.findViewById(R.id.edit_text_recipe_website)).getText().toString();

        //check that something was entered
        if(url.isEmpty()){
            Toast.makeText(getContext(), getContext().getString(R.string.recipe_url_empty),Toast.LENGTH_LONG).show();
        }

        //validate url format
        else if(!RecipeWebsiteUtils.isValidUrl(url)){
            Toast.makeText(getContext(), getContext().getString(R.string.recipe_url_invalid),Toast.LENGTH_LONG).show();
        }

        //check that this website is one of the supported websites
        else if(RecipeWebsiteUtils.getDomain(url) == RecipeWebsiteUtils.Domain.NOT_SUPPORTED){
            Toast.makeText(getContext(), getContext().getString(R.string.recipe_url_unsupported),Toast.LENGTH_LONG).show();
        }

        //attempt to create a Recipe from the website
        else{
            Callable<Integer> generateRecipe = () -> recipesViewModel.generateRecipeIdFromUrl(url);
            Future<Integer> recipeId = websiteExecutor.submit(generateRecipe);

            try{
                //if it failed, display error message
                if(recipeId.get() == -1){
                    Toast.makeText(getContext(), getContext().getString(R.string.recipe_url_error),Toast.LENGTH_LONG).show();
                }
                //otherwise, navigate to editor for the new recipe
                else{
                    CreateRecipeFragmentDirections.ActionCreateRecipeToRecipeEditorFragment action =
                            CreateRecipeFragmentDirections.actionCreateRecipeToRecipeEditorFragment();

                    //pass id of newly created recipe
                    action.setRecipeId(recipeId.get());

                    //navigate
                    Navigation.findNavController(root).navigate(action);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), getContext().getString(R.string.recipe_url_timeout_error),Toast.LENGTH_LONG).show();
            } catch (ExecutionException e) {
                Toast.makeText(getContext(), getContext().getString(R.string.recipe_url_timeout_error),Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
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