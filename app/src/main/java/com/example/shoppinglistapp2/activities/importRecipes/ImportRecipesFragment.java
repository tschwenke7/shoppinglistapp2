package com.example.shoppinglistapp2.activities.importRecipes;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.databinding.FragmentImportRecipesBinding;

public class ImportRecipesFragment extends Fragment {

    private ImportRecipesViewModel mViewModel;
    private FragmentImportRecipesBinding binding;

    public static ImportRecipesFragment newInstance() {
        return new ImportRecipesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(ImportRecipesViewModel.class);

        binding = FragmentImportRecipesBinding.inflate(inflater,container,false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupViews();
    }

    public void setupViews() {
        setHasOptionsMenu(true);
        ((MainActivity) requireActivity()).showUpButton();

        ((AppCompatActivity) getParentFragment().requireActivity()).getSupportActionBar()
        .setTitle(R.string.import_recipes_title);

        String json = getArguments().getString("jsonRecipes");
        binding.sampleText.setText(json);
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