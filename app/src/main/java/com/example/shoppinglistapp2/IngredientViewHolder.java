package com.example.shoppinglistapp2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;

public class IngredientViewHolder extends RecyclerView.ViewHolder {
    private final TextView ingredientNameView;

    public IngredientViewHolder(@NonNull View itemView) {
        super(itemView);
        ingredientNameView = itemView.findViewById(R.id.ingredient_name);
    }

    public void bind (Ingredient ingredient){
        ingredientNameView.setText(ingredient.getName());
    }

    public static IngredientViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ingredient_list_item, parent, false);
        return new IngredientViewHolder(view);
    }
}
