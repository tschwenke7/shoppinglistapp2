package com.example.shoppinglistapp2.activities.importRecipes;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.mainContentFragments.MainContentFragment;
import com.example.shoppinglistapp2.databinding.FragmentImportRecipesBinding;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.Animations;
import com.example.shoppinglistapp2.helpers.ErrorsUI;
import com.example.shoppinglistapp2.helpers.RecursiveViewHelpers;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class ImportRecipesFragment extends Fragment implements ImportListAdapter.ClickListener {

    private static final String TAG = "T_DBG_IMPORT_FRAG";
    private ImportRecipesViewModel viewModel;
    private FragmentImportRecipesBinding binding;
    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;
    private ImportListAdapter adapter;

    private int conflictStrategy = ImportRecipesViewModel.NOT_SET;

    public static ImportRecipesFragment newInstance() {
        return new ImportRecipesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ImportRecipesViewModel.class);

        binding = FragmentImportRecipesBinding.inflate(inflater,container,false);

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(this.requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupViews();
    }

    public void setupViews() {
        setHasOptionsMenu(true);
//        ((MainActivity) requireActivity()).showUpButton();

        String json = getArguments().getString("jsonRecipes");
        //parse the json recipes so they can be displayed in the ui
        //results will be availble from viewmodel.getRecipesToImport Livedata
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.importFromJson(json)),
                new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(@Nullable Object result) {}

                    @Override
                    public void onFailure(Throwable t) {
                        ErrorsUI.showAlert(requireContext(), R.string.error_importing_recipes);
                    }
                },
                uiExecutor);

        //setup tag input autocomplete
        ImportListAdapter.ClickListener fragment = this;
        Futures.addCallback(
                viewModel.getDistinctTagNames(),
                new FutureCallback<List<String>>() {
                    @Override
                    public void onSuccess(@Nullable List<String> result) {

                        ArrayAdapter<String> tagsAdapter = new ArrayAdapter<>(
                                getContext(), android.R.layout.simple_dropdown_item_1line, result);

                        //setup recyclerview
                        adapter = new ImportListAdapter(backgroundExecutor, fragment, tagsAdapter);
                        binding.importRecyclerview.setAdapter(adapter);
                        binding.importRecyclerview.setLayoutManager(new NpaLinearLayoutManager(requireContext()));

                        viewModel.getRecipesToImport().observe(getViewLifecycleOwner(), new Observer<List<RecipeWithTagsAndIngredients>>() {
                            @Override
                            public void onChanged(List<RecipeWithTagsAndIngredients> list) {
                                //set title
                                if(list.size() == 1) {
                                    ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar()
                                            .setTitle(R.string.import_recipes_title_single);
                                }
                                else {
                                    ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar()
                                            .setTitle(getString(R.string.import_recipes_title_multiple, list.size()));
                                }

                                //populate recyclerview
                                adapter.submitList(list, () -> {
//                                    binding.contentContainer.setVisibility(View.VISIBLE);
//                                    binding.importProgressBar.setVisibility(View.GONE);
                                    Animations.fadeSwap(binding.importProgressBar, binding.contentContainer);
                                    //trying to observe while list is rapidly shortened during save all
                                    //causes errors when recyclerview tries to keep up. Instead,
                                    //we manually refresh recyclerview after this point
                                    viewModel.getRecipesToImport().removeObserver(this);
                                });
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ErrorsUI.showDefaultToast(requireContext());
                    }
                },
                uiExecutor
        );
    }

    @Override
    public void onRecipeClick(int position) {
        //unused
    }

    @Override
    public boolean onRecipeLongPress(View view, int position) {
        //unused
        return false;
    }

    @Override
    public void onAddTagClicked(AutoCompleteTextView tagField, ChipGroup chipGroup, View noTagsPlaceHolder) {
        String tagName = tagField.getText().toString();
        if (!tagName.isEmpty()){
            //add to viewmodel
            viewModel.addTag(tagName);
            adapter.notifyDataSetChanged();

            //add a tag to UI
            chipGroup.post(() -> {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, null, false);
                chip.setText(tagName);

                //show close icon only if we are in editing mode
                chip.setCloseIconVisible(true);

                chip.setOnCloseIconClickListener((view -> {
                    viewModel.deleteTag(tagName);
                    chipGroup.removeView(view);
                    adapter.notifyDataSetChanged();

                    //show placeholder again if all tags deleted
                    if(chipGroup.getChildCount() == 0) {
                        noTagsPlaceHolder.setVisibility(View.VISIBLE);
                    }
                }));

                chipGroup.addView(chip);

                //hide placeholder text
                noTagsPlaceHolder.setVisibility(View.GONE);
            });

            //clear input field
            tagField.setText("");
        }


    }

    private void enableContent() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                binding.contentContainer.setAlpha(1f);
                binding.importProgressBar.setVisibility(View.GONE);
                requireView().findViewById(R.id.add_tag_button).setEnabled(true);
                requireView().findViewById(R.id.edit_text_tag).setEnabled(true);
                requireView().findViewById(R.id.chipgroup).setEnabled(true);
                requireView().findViewById(R.id.keep_ratings_switch).setEnabled(true);
                requireView().findViewById(R.id.button_save_all).setEnabled(true);
            } catch (Exception ignored) {}
        });
    }

    private void disableContent() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                binding.contentContainer.setAlpha(0.5f);
                binding.importProgressBar.setVisibility(View.VISIBLE);
                requireView().findViewById(R.id.add_tag_button).setEnabled(false);
                requireView().findViewById(R.id.edit_text_tag).setEnabled(false);
                requireView().findViewById(R.id.chipgroup).setEnabled(false);
                requireView().findViewById(R.id.keep_ratings_switch).setEnabled(false);
                requireView().findViewById(R.id.button_save_all).setEnabled(false);
            }
            catch (Exception ignored) {}
        });
    }

    @Override
    public void onSaveAllClicked() {
        //update ui to reflect what's going on
        disableContent();

        Futures.addCallback(backgroundExecutor.submit(() -> {
                try {
                    //attempt to save each recipe
                    List<RecipeWithTagsAndIngredients> recipes = new ArrayList<>(viewModel.getRecipesToImport().getValue());
                    for (RecipeWithTagsAndIngredients recipe : recipes) {
                        try {
                            viewModel.saveRecipe(recipe);
                        } catch (ImportRecipesViewModel.DuplicateRecipeNameException e) {
                            //update list
                            adapter.submitList(viewModel.getListNonLive());
                            enableContent();
                            //the latch blocks continuation of saving until user picks an option
                            // from the conflict resolution dialog
                            final CountDownLatch latch = new CountDownLatch(1);
                            promptConflictStrategy(recipe, latch);
                            latch.await();
                            disableContent();
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    uiExecutor.execute(() -> {
                        ErrorsUI.showDefaultToast(requireContext());
                        enableContent();
                    });
                }
            }),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {
                    //restore UI
                    enableContent();
                    //show success message
                    Toast.makeText(requireContext(), R.string.imported_successfully, Toast.LENGTH_LONG).show();

                    //navigate to recipe book
                    ImportRecipesFragmentDirections.ActionImportToMainContent action
                            = ImportRecipesFragmentDirections.actionImportToMainContent();
                    action.setSetViewpagerTo(MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX);
                    Navigation.findNavController(requireView()).navigate(action);
                }

                @Override
                public void onFailure(Throwable t) {
                    ErrorsUI.showDefaultToast(requireContext());
                    Log.e(TAG, "saving imported recipes: ", t);
                    enableContent();
                }
            },
            uiExecutor);
    }



    private void promptConflictStrategy(RecipeWithTagsAndIngredients recipe, CountDownLatch latch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        TextView titleView = (TextView) getLayoutInflater().inflate(R.layout.custom_alert_title, null);
        titleView.setText(getString(R.string.import_meal_conflict_title, recipe.getRecipe().getName()));
        builder.setCustomTitle(titleView);

        builder.setItems(new CharSequence[]{
                        getString(R.string.import_conflict_delete_old),
                        getString(R.string.import_conflict_delete_new),
                        getString(R.string.import_conflict_keep_both)
                },
                (dialog, which) -> {
                    // The 'which' argument contains the index position
                    // of the selected item
                    conflictStrategy = which;
                    try {
                        viewModel.saveRecipe(recipe, conflictStrategy);
                    } catch (ExecutionException | InterruptedException e) {
                        ErrorsUI.showDefaultToast(requireContext());
                    }
                    finally {
                        latch.countDown();
                    }
                });
        uiExecutor.execute(() -> builder.create().show());
    }

    @Override
    public void onKeepRatingsToggled(boolean isChecked) {
        viewModel.setKeepRatings(isChecked);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                requireActivity().onBackPressed();
        }
        return true;
    }
}