package com.example.shoppinglistapp2.activities.ui.recipes.newrecipe;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.shoppinglistapp2.db.tables.Ingredient;

public class IngredientListAdapter extends ListAdapter<Ingredient, IngredientViewHolder> {
    public IngredientListAdapter(@NonNull DiffUtil.ItemCallback<Ingredient> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return IngredientViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Ingredient current = getItem(position);
        holder.bind(current);
    }

    public static class IngredientDiff extends DiffUtil.ItemCallback<Ingredient> {

        @Override
        public boolean areItemsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            return oldItem.getId() == newItem.getId();
        }
    }
}
