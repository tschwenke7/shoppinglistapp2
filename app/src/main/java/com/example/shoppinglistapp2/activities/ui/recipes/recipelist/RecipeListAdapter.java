package com.example.shoppinglistapp2.activities.ui.recipes.recipelist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.helpers.RecipeComparators;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RecipeListAdapter extends RecyclerView.Adapter<RecipeListAdapter.ViewHolder> implements Filterable {
    private List<Recipe> recipes;
    private List<Recipe> recipesFull;

    private OnRecipeClickListener onRecipeClickListener;
    private List<Integer> selectedPositions = new ArrayList<>();
    private DecimalFormat ratingFormat = new DecimalFormat("#.#");
    private SearchCriteria searchCriteria;

    public enum SearchCriteria {
        NAME,
        INGREDIENT,
        TAG
    };

    public RecipeListAdapter(OnRecipeClickListener onRecipeClickListener){
        this.onRecipeClickListener = onRecipeClickListener;
        searchCriteria = SearchCriteria.NAME;
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
        this.recipesFull = new ArrayList<>(newRecipes);
    }

    public void setSearchCriteria(int searchCriteria){
        switch (searchCriteria){
            case 0:
                this.searchCriteria = SearchCriteria.NAME;
                break;
            case 1:
                this.searchCriteria = SearchCriteria.INGREDIENT;
                break;
            case 2:
                this.searchCriteria = SearchCriteria.TAG;
                break;
        }
    }

    public void sort(int orderingCriteria){
        //choose a comparator depending on which option was selected by the user
        Comparator<Recipe> comparator = null;
        switch (orderingCriteria){
            //alphabetically by recipe name
            case 0:
                comparator = new RecipeComparators.CompareRecipeName();
                break;
            //by prep time
            case 1:
                comparator = new RecipeComparators.ComparePrepTime();
                break;
            //by total time
            case 2:
                comparator = new RecipeComparators.CompareTotalTime();
                break;
            //by Tom rating
            case 3:
                comparator = new RecipeComparators.CompareTomRating();
                break;
            //by Tiernan rating
            case 4:
                comparator = new RecipeComparators.CompareTiernanRating();
                break;
            //by combined rating
            case 5:
                comparator = new RecipeComparators.CompareCombinedRating();
                break;
        }

        //now sort both the filtered and full lists using this comparator
        Collections.sort(recipes, comparator);
        Collections.sort(recipesFull, comparator);
        //notify the adapter of the change
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

    @Override
    public Filter getFilter() {
        switch (searchCriteria){
            case INGREDIENT:
                return ingredientFilter;
            case TAG:
                return tagFilter;
            default:
                return nameFilter;
        }
    }

    /** Filters recipes to match those whose name contains the query string */
    private Filter nameFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Recipe> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                //if recipes haven't finished loading yet, wait until they have
                while (recipesFull == null){
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(recipesFull);
            }
            else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                //find all recipes whose name contains the query string
                for (Recipe recipe : recipesFull){
                    if(recipe.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(recipe);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            recipes.clear();
            if(filterResults.values != null){
                recipes.addAll((List) filterResults.values);
            }
            notifyDataSetChanged();
        }
    };

    /** Matches recipes which contain ingredients containing each comma-separated string in query */
    private Filter ingredientFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Recipe> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                //if recipes haven't finished loading yet, wait until they have
                while (recipesFull == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(recipesFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                String[] ingredientsToMatch = filterPattern.split(",");

                //trim ingredient names
                for (int i = 0; i < ingredientsToMatch.length; i++){
                    ingredientsToMatch[i] = ingredientsToMatch[i].trim();
                }

                //find all recipes who have ingredients matching each of these
                for (Recipe recipe : recipesFull) {
                    //this array maintains which ingredients have been matched
                    // as we iterate through the list of ingredients
                    boolean[] found = new boolean[ingredientsToMatch.length];
                    //for each ingredient of the recipe
//                    for(Ingredient ing : recipe.getIngredients()) {
//                        //check its name against all search parameter ingredients which haven't yet been found
//                        int i = 0;
//                        while(i < ingredientsToMatch.length && !isAllTrue(found)){
//                            if (!found[i] && ing.getName().toLowerCase().contains(ingredientsToMatch[i])) {
//                                found[i] = true;
//                            }
//                            i++;
//                        }
//                    }
                    //if we found all search parameter ingredients, then add this to the list
                    if (isAllTrue((found))) {
                        filteredList.add(recipe);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }


        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            recipes.clear();
            if (filterResults.values != null) {
                recipes.addAll((List) filterResults.values);
            }
            notifyDataSetChanged();
        }
    };

    /** Matches recipes which contain tags containing each comma-separated string in query */
    private Filter tagFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Recipe> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                //if recipes haven't finished loading yet, wait until they have
                while (recipesFull == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(recipesFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                String[] tagsToMatch = filterPattern.split(",");

                //trim ingredient names
                for (int i = 0; i < tagsToMatch.length; i++){
                    tagsToMatch[i] = tagsToMatch[i].trim();
                }

                //find all recipes who have ingredients matching each of these
                for (Recipe recipe : recipesFull) {
                    //this array maintains which ingredients have been matched
                    // as we iterate through the list of ingredients
                    boolean[] found = new boolean[tagsToMatch.length];
                    //for each ingredient of the recipe
                    for(String tag : recipe.getTags()) {
                        //check its name against all search parameter tags which haven't yet been found
                        int i = 0;
                        while(i < tagsToMatch.length && !isAllTrue(found)){
                            if (!found[i] && tag.toLowerCase().contains(tagsToMatch[i])) {
                                found[i] = true;
                            }
                            i++;
                        }
                    }
                    //if we found all search parameter ingredients, then add this to the list
                    if (isAllTrue((found))) {
                        filteredList.add(recipe);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }


        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            recipes.clear();
            if (filterResults.values != null) {
                recipes.addAll((List) filterResults.values);
            }
            notifyDataSetChanged();
        }
    };

    private boolean isAllTrue(boolean... array){
        for (boolean b: array){
            if(!b) return false;
        }
        return true;
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

            //remove any previous tags
            ChipGroup chipGroup = itemView.findViewById(R.id.recipe_tags);
            chipGroup.removeAllViews();
            //add tags
            for (String tagName : recipe.getTags()){
                //add a sample tag
                Chip chip = (Chip) LayoutInflater.from(itemView.getContext()).inflate(R.layout.tag_chip_recipe_card, null, false);
                chip.setText(tagName);

                chipGroup.addView(chip);
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
