package com.example.shoppinglistapp2.activities.importRecipes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.databinding.FragmentImportRecipesBinding;
import com.example.shoppinglistapp2.helpers.Animations;
import com.example.shoppinglistapp2.helpers.ErrorsUI;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.List;
import java.util.concurrent.Executor;

public class ImportRecipesFragment extends Fragment implements ImportListAdapter.ClickListener {

    private ImportRecipesViewModel viewModel;
    private FragmentImportRecipesBinding binding;
    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;

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

        ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar()
        .setTitle(R.string.import_recipes_title);

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
                        ImportListAdapter adapter = new ImportListAdapter(backgroundExecutor, fragment, tagsAdapter);
                        binding.importRecyclerview.setAdapter(adapter);
                        binding.importRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

                        viewModel.getRecipesToImport().observe(getViewLifecycleOwner(), (list) -> {
                            adapter.submitList(list, () -> {
                                if (binding.contentContainer.getVisibility() == View.GONE) {
                                    Animations.fadeSwap(binding.importProgressBar, binding.contentContainer);
                                }
                            });
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

            //add a tag to UI
            chipGroup.post(() -> {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.tag_chip, null, false);
                chip.setText(tagName);

                //show close icon only if we are in editing mode
                chip.setCloseIconVisible(true);


                chip.setOnCloseIconClickListener((view -> {
                    viewModel.deleteTag(tagName);
                    chipGroup.removeView(view);

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

    @Override
    public void onSaveAllClicked() {

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