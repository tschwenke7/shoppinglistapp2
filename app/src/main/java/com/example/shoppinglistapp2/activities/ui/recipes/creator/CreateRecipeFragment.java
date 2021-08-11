package com.example.shoppinglistapp2.activities.ui.recipes.creator;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesViewModel;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 */
public class CreateRecipeFragment extends Fragment {
    private RecipesViewModel recipesViewModel;
    private View root;
//    private ExecutorService websiteExecutor = Executors.newSingleThreadExecutor();

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

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi(root);
    }

    private void setupUi(View root){
        //setup action bar to allow back button
        this.setHasOptionsMenu(true);
        TextView supportedSitesText = root.findViewById(R.id.text_view_supported_websites);
        supportedSitesText.setText(Html.fromHtml(getString(R.string.supported_recipes)));
        supportedSitesText.setMovementMethod(LinkMovementMethod.getInstance());

        //setup "create recipe from website" button
        Button websiteButton = root.findViewById(R.id.create_recipe_from_website_button);
        websiteButton.setOnClickListener(this::onWebsiteButtonClicked);

        //setup manual recipe creation button to get a new recipe id for an empty recipe and navigate
        //to editor
        Button manualButton = root.findViewById(R.id.create_recipe_manually_button);
        manualButton.setOnClickListener(view -> {
            //create new empty recipe and navigate to recipe editor screen when "+" icon clicked
            CreateRecipeFragmentDirections.ActionCreateRecipeToViewRecipe action =
                    CreateRecipeFragmentDirections.actionCreateRecipeToViewRecipe();
            //todo action.setRecipeId(recipesViewModel.generateNewRecipeId());
            Navigation.findNavController(root).navigate(action);
        });



        //configure back button to work within parent fragment
        Fragment f1 = this;
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavHostFragment.findNavController(f1).navigateUp();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    private void onWebsiteButtonClicked(View view){
        //show progress bar and fade everything else
        ProgressBar progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        View mainContent = root.findViewById(R.id.main_content_container);
        mainContent.setAlpha(0.2f);

        String url = ((EditText) root.findViewById(R.id.edit_text_recipe_website)).getText().toString().trim();



        //attempt to create a Recipe from the website
        //attempt to load the website
//       todo ListenableFuture<Integer> recipeId = ((App) getActivity().getApplication())
//                .backgroundExecutorService.submit(() -> recipesViewModel.generateRecipeIdFromUrl(url));

//        Futures.addCallback(recipeId, new FutureCallback<Integer>() {
//            @Override
//            public void onSuccess(@Nullable Integer recipeId) {
//                //if recipe loading failed, display error message
//                if(recipeId == null || recipeId == -1){
//                    Toast.makeText(getContext(), requireContext().getString(R.string.recipe_url_error),Toast.LENGTH_LONG).show();
//                }
//                //otherwise, navigate to editor for the new recipe
//                else{
//                    CreateRecipeFragmentDirections.ActionCreateRecipeToViewRecipe action =
//                            CreateRecipeFragmentDirections.actionCreateRecipeToViewRecipe();
//
//                    //pass id of newly created recipe
//                    action.setRecipeId(recipeId);
//
//                    //navigate
//                    Navigation.findNavController(root).navigate(action);
//                    //hide progress bar and reset opacity of everything else
//                    progressBar.setVisibility(View.GONE);
//                    mainContent.setAlpha(1.0f);
//                }
//            }
//            //respond to differenct potential errors
//            @Override
//            public void onFailure(Throwable t) {
//                //the url entered was not valid for some reason
//                if (t instanceof InvalidRecipeUrlExeception){
//                    Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
//                }
//                //the thread processing this was interrupted
//                else if (t instanceof ExecutionException) {
//                    t.printStackTrace();
//                    Toast.makeText(getContext(), R.string.recipe_url_timeout_error, Toast.LENGTH_LONG).show();
//                }
//                //another error I wasn't expecting
//                else{
//                    t.printStackTrace();
//                    Toast.makeText(getContext(), R.string.unknown_error + t.getMessage(), Toast.LENGTH_LONG).show();
//                }
//                //hide progress bar and reset opacity of everything else
//                progressBar.setVisibility(View.GONE);
//                mainContent.setAlpha(1.0f);
//            }
//        }, ContextCompat.getMainExecutor(this.requireContext()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //back button pressed
            case android.R.id.home:
                ((MainActivity) getParentFragment().getActivity()).onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //show back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();

        //show back button
        MainActivity activity = (MainActivity) getParentFragment().getActivity();
        if (activity != null) {
            activity.showUpButton();

            //check if we need to redirect to a recipe, and if so, go back to recipe list so we
            //can navigate to the desired recipe
//    todo        if(null != recipesViewModel.getNavigateToRecipeId()){
//                activity.onBackPressed();
//                activity.onBackPressed();
//            }
        }

        //set title of page
        ((AppCompatActivity) getParentFragment().getActivity()).getSupportActionBar().setTitle(R.string.create_recipe_title);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}