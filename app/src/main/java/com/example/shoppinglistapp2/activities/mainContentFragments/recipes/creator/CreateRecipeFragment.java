package com.example.shoppinglistapp2.activities.mainContentFragments.recipes.creator;

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
import android.widget.Toast;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ContentFragment;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.mainContentFragments.SharedViewModel;
import com.example.shoppinglistapp2.databinding.FragmentCreateRecipeBinding;
import com.example.shoppinglistapp2.helpers.Domain;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.example.shoppinglistapp2.helpers.RecipeWebsiteUtils;
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
public class CreateRecipeFragment extends ContentFragment {
    private CreateRecipeViewModel viewModel;
    private SharedViewModel sharedViewModel;

    private FragmentCreateRecipeBinding binding;

    public CreateRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentCreateRecipeBinding.inflate(inflater, container, false);

        viewModel =
                new ViewModelProvider(requireActivity()).get(CreateRecipeViewModel.class);
        sharedViewModel =
                new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi();
//        viewModel.loadFromBackup(this,backgroundExecutor);
    }

    private void setupUi(){
        //setup action bar
        setHasMenu(true);
        setPageTitle(getString(R.string.create_recipe_title));
        setShowUpButton(true);

        binding.textViewSupportedWebsites.setText(Html.fromHtml(getString(R.string.supported_recipes)));
        binding.textViewSupportedWebsites.setMovementMethod(LinkMovementMethod.getInstance());

        //setup "create recipe from website" button
        binding.createRecipeFromWebsiteButton.setOnClickListener(this::onWebsiteButtonClicked);

        //setup manual recipe creation button to get a new recipe id for an empty recipe and navigate
        //to editor
        binding.createRecipeManuallyButton.setOnClickListener(view -> onManualButtonClicked());

        //configure back button to work within parent fragment
        addDefaultOnBackPressedCallback();
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
                    action.setPrefillSource(Domain.FROM_SCRATCH);
                    Navigation.findNavController(requireView()).navigate(action);

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

        String url = binding.editTextRecipeWebsite.getText().toString().trim();
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
                    action.setPrefillSource(RecipeWebsiteUtils.getDomain(url));

                    //pass id of newly created recipe
                    action.setRecipeId(recipeId);

                    //navigate
                    Navigation.findNavController(requireView()).navigate(action);
                    //hide progress bar and reset opacity of everything else
                    hideProgressBar();
                }
            }
            //respond to different potential errors
            @Override
            public void onFailure(Throwable t) {
                //the url entered was not valid for some reason
                if (t instanceof InvalidRecipeUrlExeception){
                    Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                }
                //the thread processing this was interrupted
                else if (t instanceof InterruptedException) {
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
                requireActivity().onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //show back button in action bar for this fragment
    @Override
    public void onResume() {
        super.onResume();
        //check if we need to redirect to a recipe, and if so, go back to recipe list so we
        //can navigate to the desired recipe
        if(null != sharedViewModel.getNavigateToRecipeId() || null != sharedViewModel.getSelectingForMeal()){
            requireActivity().onBackPressed();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    /** hide progress bar and reset opacity of everything else */
    private void showProgressBar(){
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.mainContentContainer.setAlpha(0.2f);
        binding.createRecipeManuallyButton.setEnabled(false);
        binding.createRecipeFromWebsiteButton.setEnabled(false);

    }

    /** fade main content and display loading spinner over the top */
    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
        binding.mainContentContainer.setAlpha(1.0f);
        binding.createRecipeManuallyButton.setEnabled(true);
        binding.createRecipeFromWebsiteButton.setEnabled(true);
    }
}