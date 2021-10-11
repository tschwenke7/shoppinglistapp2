package com.example.shoppinglistapp2.activities.mainContentFragments.recipes.viewrecipe.selectmeal;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.activities.mainContentFragments.SharedViewModel;
import com.example.shoppinglistapp2.databinding.FragmentSelectMealBinding;
import com.example.shoppinglistapp2.helpers.RecursiveViewClickable;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Executor;

public class SelectMealFragment extends Fragment implements MealSelectionAdapter.ClickListener{

    private SelectMealViewModel viewModel;
    private FragmentSelectMealBinding binding;
    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;
    private SharedViewModel sharedViewModel;

    public static SelectMealFragment newInstance() {
        return new SelectMealFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSelectMealBinding.inflate(inflater, container, false);
        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(requireContext());


        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel = new ViewModelProvider(this).get(SelectMealViewModel.class);

        setupViews();
    }

    private void setupViews() {
        //get params from navArgs
        viewModel.setRecipeId(SelectMealFragmentArgs.fromBundle(getArguments()).getSelectingRecipeId());
        viewModel.setMealPlanId(SelectMealFragmentArgs.fromBundle(getArguments()).getMealPlanId());

        //set page title to
        updateHintText();

        //setup selection recyclerview
        MealSelectionAdapter adapter = new MealSelectionAdapter(this);
        binding.selectMealRecyclerview.setAdapter(adapter);
        binding.selectMealRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        viewModel.getMeals().observe(getViewLifecycleOwner(), adapter::submitList);

        //setup action bar
        this.setHasOptionsMenu(true);
        //configure back button to work within parent fragment
        Fragment f1 = this;
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavHostFragment.findNavController(f1).navigateUp();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
        ((MainActivity) requireActivity()).showUpButton();
    }

    private void updateHintText() {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.getRecipeNameById(viewModel.getRecipeId())),
            new FutureCallback<String>() {
                @Override
                public void onSuccess(@Nullable String result) {
                    ActionBar actionBar =  ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar();
                    actionBar.setTitle(R.string.select_meal_title);
                    binding.selectRecipeHint.setText(getString(R.string.select_meal_hint, result));
                }

                @Override
                public void onFailure(Throwable t) {
                    Toast.makeText(requireContext(), R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
            }, uiExecutor);
    }

    @Override
    public void onMealClicked(int pos) {
        int recipeToReplaceId = viewModel.getRecipeIdAtPos(pos);
        String mealName = viewModel.getMealNameAtPos(pos);

        //if there was already a recipe in this meal, prompt user to decide what to do
        if (recipeToReplaceId != -1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            TextView titleView = (TextView) getLayoutInflater().inflate(R.layout.custom_alert_title, null);
            titleView.setText(getString(R.string.replace_meal_prompt_message,
                    mealName, viewModel.getRecipeNameAtPos(pos)));
            builder.setCustomTitle(titleView);

            builder.setItems(new CharSequence[]{
                getString(R.string.replace_meal_button_override),
                getString(R.string.replace_meal_button_move),
                getString(R.string.replace_meal_button_keep)
            },
                (dialog, which) -> {
                    // The 'which' argument contains the index position
                    // of the selected item
                    switch (which) {
                        case 0:
                            //add the new recipe then navigate back to the ViewRecipe page
                            Futures.addCallback(backgroundExecutor.submit(() -> viewModel.addRecipeToMealAtPos(pos)),
                                    new FutureCallback<Object>() {
                                        @Override
                                        public void onSuccess(@Nullable Object result) {
                                            //go back to recipe page now that selection is complete
                                            emphasiseThenNav();
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            Toast.makeText(requireContext(), R.string.unknown_error, Toast.LENGTH_LONG).show();
                                        }
                                    },
                                    uiExecutor);
                            break;
                        case 1:
                            //add the new recipe and reset the scenario for the recipe being replaced
                            Futures.addCallback(backgroundExecutor.submit(() -> viewModel.addRecipeToMealAtPos(pos)),
                                new FutureCallback<Object>() {
                                    @Override
                                    public void onSuccess(@Nullable Object result) {
                                        viewModel.setRecipeId(recipeToReplaceId);
                                        updateHintText();
                                        showSuccessToast();
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        Toast.makeText(requireContext(), R.string.unknown_error, Toast.LENGTH_LONG).show();
                                    }
                                },
                                uiExecutor);
                            break;
                        //close dialog
                        case 2:
                            break;
                    }
                });
            builder.create().show();
        }
        //otherwise just add the recipe
        else {

            backgroundExecutor.submit(() -> viewModel.addRecipeToMealAtPos(pos));
            emphasiseThenNav();
        }
    }

    private void emphasiseThenNav() {
        showSuccessToast();
        //prevent user from clicking anything while the delay expires
        //set clickable to false for all elements of recyclerview
        RecursiveViewClickable.setClickable(binding.selectMealRecyclerview, false);

        Handler handler = new Handler();
        //return to ViewRecipe page after 1000 millis
        handler.postDelayed(() -> {
            requireActivity().onBackPressed();

        }, 1000);
    }

    private void showSuccessToast() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(requireContext(), "Recipe added successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    /** Merges extra menu items into the default activity action bar, according to provided menu xml */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        requireActivity().invalidateOptionsMenu();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //back button pressed
            case android.R.id.home:
                requireActivity().onBackPressed();
                return true;
        }
        return false;
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
            if(null != sharedViewModel.getNavigateToRecipeId() || null != sharedViewModel.getSelectingForMeal()){
                activity.onBackPressed();
            }
        }

        //set title of page
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.select_meal_title);
    }

}