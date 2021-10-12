package com.example.shoppinglistapp2.activities.importRecipes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.mainContentFragments.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.activities.mainContentFragments.recipes.recipelist.RecipeListAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewImportHeaderBinding;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ImportListHeaderAdapter extends RecipeListAdapter {
    private final ClickListener clickListener;
    private final ArrayAdapter<String> tagsAdapter;
    public ImportListHeaderAdapter(Executor listUpdateExecutor, ClickListener clickListener, ArrayAdapter<String> tagsAdapter) {
        super(listUpdateExecutor, clickListener);
        this.clickListener = clickListener;
        this.tagsAdapter = tagsAdapter;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 1;
        }
        return 2;
    }

    @Override
    public void submitList(List<RecipeWithTagsAndIngredients> newItems, @Nullable Runnable callback) {

        super.submitList(newItems, callback);
    }

    @Override
    public int getItemCount() {
        return items.size() + 2;
    }

    @Override
    public RecipeWithTagsAndIngredients getItem(int position) {
        return items.get(position - 1);
    }

    @NonNull
    @Override
    public BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients>.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            //header item
            case 1:
                return new HeaderViewHolder(RecyclerviewImportHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                    clickListener);
            //recipe item
            case 2:
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_recipe, parent, false);
                return new RecipeListAdapter.ViewHolder(view, clickListener);
        }

        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients>.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            holder.bind(null);
        }
        else {
            super.onBindViewHolder(holder, position);
        }
    }

    public class HeaderViewHolder extends BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients>.ViewHolder {
    private final ClickListener clickListener;
    private final RecyclerviewImportHeaderBinding binding;

        public HeaderViewHolder(RecyclerviewImportHeaderBinding binding, ClickListener clickListener) {
            super(binding.getRoot());
            this.clickListener = clickListener;
            this.binding = binding;
        }


        @Override
        public void bind(RecipeWithTagsAndIngredients item) {
            binding.editTextTag.setAdapter(tagsAdapter);
            binding.addTagButton.setOnClickListener((v) -> {
                clickListener.onAddTagClicked(binding.editTextTag, binding.chipgroup, binding.noTagsPlaceholder);
            });

            binding.buttonSaveAll.setOnClickListener((v) -> clickListener.onSaveAllClicked());
        }
    }

    public interface ClickListener extends RecipeListAdapter.OnRecipeClickListener {
        void onAddTagClicked(AutoCompleteTextView tagInputField, ChipGroup chipGroup, View noTagPlaceholder);
        void onSaveAllClicked();
    }
}
