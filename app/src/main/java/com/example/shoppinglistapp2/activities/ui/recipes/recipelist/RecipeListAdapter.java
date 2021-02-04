package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeListAdapter extends RecyclerView.Adapter<RecipeListAdapter.ViewHolder> {
    private List<Recipe> recipes;
    private OnRecipeClickListener onRecipeClickListener;

    public RecipeListAdapter(OnRecipeClickListener onRecipeClickListener){
        this.onRecipeClickListener = onRecipeClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_recyclerview_item, parent, false);
        return new ViewHolder(view, onRecipeClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe current = getItem(position);
        holder.bind(current);
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(null != recipes){
            return recipes.size();
        }
        return 0;
    }

    public Recipe getItem(int position){
        if(null != recipes && recipes.size() > position){
            return recipes.get(position);
        }
        return null;
    }



    public static class RecipeDiff extends DiffUtil.ItemCallback<Recipe> {

        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return oldItem.getId() == newItem.getId();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView recipeNameView;
        private OnRecipeClickListener onRecipeClickListener;

        public ViewHolder(@NonNull View itemView, OnRecipeClickListener onRecipeClickListener) {
            super(itemView);
            recipeNameView = itemView.findViewById(R.id.textView);

            this.onRecipeClickListener = onRecipeClickListener;
            itemView.setOnClickListener(this);
        }

        public void bind (Recipe recipe){
            recipeNameView.setText(recipe.getName());
        }

        @Override
        public void onClick(View view) {
            onRecipeClickListener.onRecipeClick(getAdapterPosition());
        }
    }

    public interface OnRecipeClickListener {
        void onRecipeClick(int position);
    }
}
