package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;

public class RecipeListFragment extends Fragment implements RecipeListAdapter.OnRecipeClickListener{

    private View root;
    private RecipesViewModel recipesViewModel;
    private ActionMode actionMode;
    private ActionMode.Callback actionModeCallback = new ActionModeCallback();
    private RecipeListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel =
                new ViewModelProvider(getActivity()).get(RecipesViewModel.class);

        root = inflater.inflate(R.layout.fragment_recipe_list, container, false);

        //setup action bar
        this.setHasOptionsMenu(true);

        //setup recipe list recyclerview
        RecyclerView recipeRecyclerView = root.findViewById(R.id.recipe_recyclerview);
        adapter = new RecipeListAdapter(this);
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
                //navigate to recipe creation hub
                Navigation.findNavController(root).navigate(R.id.action_recipe_list_to_create_recipe);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRecipeClick(int position) {
        Log.d("TOM_TEST", "onRecipeClick triggered for item " + position);

        //navigate to view recipe, passing id of clicked recipe along
        RecipeListFragmentDirections.ActionRecipeListToViewRecipe action = RecipeListFragmentDirections.actionRecipeListToViewRecipe();
        action.setRecipeId(recipesViewModel.getRecipeIdAtPosition(position));
        Navigation.findNavController(root).navigate(action);
    }

    @Override
    public boolean onRecipeLongPress(View view, int position) {
        if (actionMode == null){
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }

        //check if all items have been deselected to close actionMode
        if (adapter.getSelectedItemCount() == 0){
            actionMode.finish();
        }
        //otherwise update the heading
        else {
            //change the title to say how many recipes are selected
            actionMode.setTitle(String.format("%d recipe/s selected",adapter.getSelectedItemCount()));
            actionMode.invalidate();
        }

        return true;
    }

    /**Creates and handles a contextual action bar for when one or more recipes are selected */
    private class ActionModeCallback implements ActionMode.Callback{

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.recipe_selected_action_bar, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()){
                case R.id.action_add_all_to_list:

                    //TODO - add all ingredients from these recipes to the shopping list
                    actionMode.finish();
                    return true;

                //Handle clicking of delete button
                case R.id.action_delete_recipe:
                    //prompt for confirmation first
                    new AlertDialog.Builder(root.getContext())
                            .setTitle(R.string.delete_warning_title)
                            .setMessage(String.format("%s %d %s",
                                    root.getContext().getString(R.string.delete_warning_prompt1),
                                    adapter.getSelectedItemCount(),
                                    root.getContext().getString(R.string.delete_warning_prompt2)))
                            .setPositiveButton(R.string.delete_warning_yes_button, new DialogInterface.OnClickListener() {
                                //actually delete selected recipes if confirmed
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(root.getContext(), String.format("Deleted %d recipes", adapter.getSelectedItemCount()), Toast.LENGTH_LONG).show();
                                    recipesViewModel.deleteRecipes(adapter.getSelectedItems());
                                    actionMode.finish();//remove action bar
                                }
                            })
                            //otherwise don't do anything
                            .setNegativeButton(R.string.delete_warning_cancel_button, null)
                            .show();

                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelections();
            actionMode = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //close action bar if user navigates away
        if(null != actionMode){
            actionMode.finish();
        }

    }
}