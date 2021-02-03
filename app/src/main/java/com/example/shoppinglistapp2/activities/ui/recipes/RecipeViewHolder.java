package com.example.shoppinglistapp2.activities.ui.recipes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.Recipe;

public class RecipeViewHolder extends RecyclerView.ViewHolder {
    private final TextView recipeNameView;

    public RecipeViewHolder(@NonNull View itemView) {
        super(itemView);
        recipeNameView = itemView.findViewById(R.id.textView);
    }

    public void bind (Recipe recipe){
        recipeNameView.setText(recipe.getName());
    }

    public static RecipeViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_recyclerview_item, parent, false);
        return new RecipeViewHolder(view);
    }
}
