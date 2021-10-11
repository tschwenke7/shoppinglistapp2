package com.example.shoppinglistapp2.activities.importRecipes;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglistapp2.R;
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
        String json = getArguments().getString("jsonRecipes");
        binding.sampleText.setText(json);

    }
}