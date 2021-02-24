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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class RecipeListAdapter extends RecyclerView.Adapter<RecipeListAdapter.ViewHolder> {
    private List<Recipe> recipes;
    private OnRecipeClickListener onRecipeClickListener;
    private List<Integer> selectedPositions = new ArrayList<>();
    private DecimalFormat ratingFormat = new DecimalFormat("#.#");

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
        holder.itemView.setSelected(selectedPositions.contains(position));
        holder.bind(current);
    }

    public void setRecipes(List<Recipe> newRecipes) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new RecipeDiff(newRecipes, recipes));
        diffResult.dispatchUpdatesTo(this);
        this.recipes = newRecipes;
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

    public Recipe[] getSelectedItems(){
        List<Recipe> selectedItems = new ArrayList<>();
        for(Integer position : selectedPositions){
            selectedItems.add(recipes.get(position));
        }
        return selectedItems.toArray(new Recipe[selectedItems.size()]);
    }

    public int getSelectedItemCount(){
        return selectedPositions.size();
    }

    public void clearSelections() {
        for(int position : selectedPositions){
            notifyItemChanged(position);
        }
        selectedPositions.clear();
    }


    public static class RecipeDiff extends DiffUtil.Callback {
        List<Recipe> newList;
        List<Recipe> oldList;

        public RecipeDiff (List<Recipe> newList, List<Recipe> oldList){
            this.newList = newList;
            this.oldList = oldList;
        }

        @Override
        public int getOldListSize() {
            if(oldList == null){
                return 0;
            }
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            if (newList == null){
                return 0;
            }
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final View itemView;
        private OnRecipeClickListener onRecipeClickListener;
        private Recipe recipe;

        public ViewHolder(@NonNull View itemView, OnRecipeClickListener onRecipeClickListener) {
            super(itemView);
            this.itemView = itemView;

            //set click listeners
            this.onRecipeClickListener = onRecipeClickListener;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void bind (Recipe recipe){
            this.recipe = recipe;
            //set name
            TextView recipeNameView = itemView.findViewById(R.id.recipe_name);
            recipeNameView.setText(recipe.getName());

            //set prep and cook times
            String timeUnit = itemView.getContext().getString(R.string.abbreviated_time_unit);
            TextView prepTimeView = itemView.findViewById(R.id.edit_text_prep_time);
            if(0 != recipe.getPrepTime()){
                prepTimeView.setText(String.format("%d %s",recipe.getPrepTime(), timeUnit));
            }
            else{
                prepTimeView.setText("-");
            }
            TextView cookTimeView = itemView.findViewById(R.id.cook_time);
            if(0 != recipe.getCookTime()){
                cookTimeView.setText(String.format("%d %s",recipe.getCookTime(),timeUnit));
            }
            else{
                cookTimeView.setText("-");
            }

            //set ratings
            TextView tierRatingView = itemView.findViewById(R.id.tier_rating);
            if(recipe.getTier_rating() != 0){
                float tierRating = ((float) recipe.getTier_rating()) / 2.0f;
                tierRatingView.setText(ratingFormat.format(tierRating));
            }
            else{
                tierRatingView.setText("-");
            }

            TextView tomRatingView = itemView.findViewById(R.id.tom_rating);
            if(recipe.getTom_rating() != 0){
                float tomRating = ((float) recipe.getTom_rating()) / 2.0f;
                tomRatingView.setText(ratingFormat.format(tomRating));
            }
            else{
                tomRatingView.setText("-");
            }
        }

        @Override
        public void onClick(View view) {
            onRecipeClickListener.onRecipeClick(getAdapterPosition());
        }
        @Override
        public boolean onLongClick(View view) {
            //if this item was already selected, deselect
            if(selectedPositions.contains(getAdapterPosition())){
                selectedPositions.remove((Integer) getAdapterPosition());
                view.setSelected(false);
            }
            //otherwise select this item
            else{
                selectedPositions.add(getAdapterPosition());
                view.setSelected(true);
            }

            //call owner's click handler
            onRecipeClickListener.onRecipeLongPress(view, getAdapterPosition());
            return true;
        }
    }

    public interface OnRecipeClickListener {
        void onRecipeClick(int position);
        boolean onRecipeLongPress(View view, int position);
    }
}
