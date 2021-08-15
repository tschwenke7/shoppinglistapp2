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
import com.example.shoppinglistapp2.activities.ui.SharedViewModel;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * A simple {@link Fragment} subclass.
 */
public class CreateRecipeFragment extends Fragment {
    private CreateRecipeViewModel viewModel;
    private SharedViewModel sharedViewModel;
    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;
    private View root;
    private ProgressBar progressBar;
    private View mainContent;

    public CreateRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_create_recipe, container, false);

        viewModel =
                new ViewModelProvider(requireActivity()).get(CreateRecipeViewModel.class);
        sharedViewModel =
                new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(this.requireContext());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi();
//        viewModel.loadFromBackup(this,backgroundExecutor);
    }

    private void setupUi(){

        //setup action bar to allow back button
        this.setHasOptionsMenu(true);

        progressBar = root.findViewById(R.id.progressBar);
        mainContent = root.findViewById(R.id.main_content_container);

        TextView supportedSitesText = root.findViewById(R.id.text_view_supported_websites);
        supportedSitesText.setText(Html.fromHtml(getString(R.string.supported_recipes)));
        supportedSitesText.setMovementMethod(LinkMovementMethod.getInstance());

        //setup "create recipe from website" button
        Button websiteButton = root.findViewById(R.id.create_recipe_from_website_button);
        websiteButton.setOnClickListener(this::onWebsiteButtonClicked);

        //setup manual recipe creation button to get a new recipe id for an empty recipe and navigate
        //to editor
        Button manualButton = root.findViewById(R.id.create_recipe_manually_button);
        manualButton.setOnClickListener(view -> onManualButtonClicked());



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

    //create new empty recipe and navigate to recipe editor screen
    private void onManualButtonClicked() {
        //show progress bar and fade everything else
        showProgressBar();

        //attempt to generate new recipe id, and navigate to editor page on its success
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.generateNewRecipeId()),
            new FutureCallback<Integer>() {

                @Override
                public void onSuccess(@Nullable Integer result) {
                    CreateRecipeFragmentDirections.ActionCreateRecipeToViewRecipe action =
                            CreateRecipeFragmentDirections.actionCreateRecipeToViewRecipe();
                    //pass id of newly created website as parameter of navigation
                    action.setRecipeId(result);
                    Navigation.findNavController(root).navigate(action);

                    //hide progress bar and reset opacity of everything else
                    hideProgressBar();
                }

                @Override
                public void onFailure(Throwable t) {
                    //hide progress bar and reset opacity of everything else
                    hideProgressBar();
                }
            },
            ContextCompat.getMainExecutor(requireContext()));
    }

    private void onWebsiteButtonClicked(View view){
        //show progress bar and fade everything else
        showProgressBar();

        String url = ((EditText) root.findViewById(R.id.edit_text_recipe_website)).getText().toString().trim();
        //attempt to create a Recipe from the website//attempt to load the website
        ListenableFuture<Integer> recipeId = backgroundExecutor.submit(
               () -> viewModel.generateRecipeIdFromUrl(url));

        Futures.addCallback(recipeId, new FutureCallback<Integer>() {
            @Override
            public void onSuccess(@Nullable Integer recipeId) {
                //if recipe loading failed, display error message
                if(recipeId == null || recipeId == -1){
                    Toast.makeText(requireContext(), getString(R.string.recipe_url_error),Toast.LENGTH_LONG).show();
                }
                //otherwise, navigate to editor for the new recipe
                else{
                    CreateRecipeFragmentDirections.ActionCreateRecipeToViewRecipe action =
                            CreateRecipeFragmentDirections.actionCreateRecipeToViewRecipe();

                    //pass id of newly created recipe
                    action.setRecipeId(recipeId);

                    //navigate
                    Navigation.findNavController(root).navigate(action);
                    //hide progress bar and reset opacity of everything else
                    progressBar.setVisibility(View.GONE);
                    mainContent.setAlpha(1.0f);
                }
            }
            //respond to differenct potential errors
            @Override
            public void onFailure(Throwable t) {
                //the url entered was not valid for some reason
                if (t instanceof InvalidRecipeUrlExeception){
                    Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                }
                //the thread processing this was interrupted
                else if (t instanceof ExecutionException) {
                    t.printStackTrace();
                    Toast.makeText(getContext(), getString(R.string.recipe_url_timeout_error), Toast.LENGTH_LONG).show();
                }
                //another error I wasn't expecting
                else{
                    t.printStackTrace();
                    Toast.makeText(getContext(), getString(R.string.unknown_error) + t.getMessage(), Toast.LENGTH_LONG).show();
                }
                //hide progress bar and reset opacity of everything else
                hideProgressBar();
            }
        }, uiExecutor);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //back button pressed
            case android.R.id.home:
                ((MainActivity) requireParentFragment().requireActivity()).onBackPressed();
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
        MainActivity activity = (MainActivity) requireParentFragment().requireActivity();
        if (activity != null) {
            activity.showUpButton();

            //check if we need to redirect to a recipe, and if so, go back to recipe list so we
            //can navigate to the desired recipe
            if(null != sharedViewModel.getNavigateToRecipeId()){
                activity.onBackPressed();
                activity.onBackPressed();
            }
        }

        //set title of page
        ((AppCompatActivity) requireParentFragment().requireActivity()).getSupportActionBar().setTitle(R.string.create_recipe_title);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    /** hide progress bar and reset opacity of everything else */
    private void showProgressBar(){
        progressBar.setVisibility(View.VISIBLE);
        mainContent.setAlpha(0.2f);
    }

    /** fade main content and display loading spinner over the top */
    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        mainContent.setAlpha(1.0f);
    }
}