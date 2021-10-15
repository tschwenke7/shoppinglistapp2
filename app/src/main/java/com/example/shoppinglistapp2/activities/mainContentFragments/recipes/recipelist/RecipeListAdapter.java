package com.example.shoppinglistapp2.activities.mainContentFragments.recipes.recipelist;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.Tag;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.example.shoppinglistapp2.helpers.RecipeComparators;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class RecipeListAdapter extends BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients> implements Filterable {
    private List<RecipeWithTagsAndIngredients> itemsFull;
    private OnRecipeClickListener onRecipeClickListener;
    private List<Integer> selectedPositions = new ArrayList<>();
    private DecimalFormat ratingFormat = new DecimalFormat("#.#");
    private RecipeListFragment.SearchCriteria searchCriteria;
    private int orderByCriteria;
    private CharSequence latestConstraint;



    public RecipeListAdapter(Executor listUpdateExecutor, OnRecipeClickListener onRecipeClickListener){
        super(listUpdateExecutor);
        this.onRecipeClickListener = onRecipeClickListener;
        searchCriteria = RecipeListFragment.SearchCriteria.NAME;
    }

    @NonNull
    @Override
    public BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients>.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_recipe, parent, false);
        return new ViewHolder(view, onRecipeClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients>.ViewHolder holder, int position) {
        holder.itemView.setSelected(selectedPositions.contains(position));
        holder.bind(getItem(position));
    }

    /** Use this method in fragment */
    public void updateList(@Nullable List<RecipeWithTagsAndIngredients> list, @Nullable Runnable commitCallback){
        updateListExecutor.execute(() -> {
            backupFullList(list);
            sortList(itemsFull);
            filter(latestConstraint);//calls submit list after filtering

            //run callback after all this has finished if one provided
            if(commitCallback != null) {
                Handler uiHandler = new Handler(Looper.getMainLooper());
                uiHandler.post(commitCallback);
            }
        });
    }

    /** Use this method in fragment */
    public void updateList(@Nullable List<RecipeWithTagsAndIngredients> list){
        submitList(list, null);
    }

    @Override
    protected BaseDiffCallback<RecipeWithTagsAndIngredients> createDiffCallback(List<RecipeWithTagsAndIngredients> newList, List<RecipeWithTagsAndIngredients> oldList) {
        return new BaseDiffCallback<RecipeWithTagsAndIngredients>(newList, oldList) {
            @Override
            public boolean areItemsTheSame(RecipeWithTagsAndIngredients oldItem, RecipeWithTagsAndIngredients newItem) {
                return oldItem.getRecipe().getId() == newItem.getRecipe().getId();
            }

            @Override
            public boolean areContentsTheSame(RecipeWithTagsAndIngredients oldItem, RecipeWithTagsAndIngredients newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    public List<String> getOtherMatchingTags() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return getCurrentList().stream()
                    .map(RecipeWithTagsAndIngredients::getTags)
                    .flatMap(Collection::stream)
                    .map(Tag::getName)
                    .distinct()
                    .collect(Collectors.toList());
        }
        else {
            List<String> list = new ArrayList<>();
            Set<String> uniqueValues = new HashSet<>();
            for (RecipeWithTagsAndIngredients recipeWithTagsAndIngredients : getCurrentList()) {
                List<Tag> tags = recipeWithTagsAndIngredients.getTags();
                for (Tag tag : tags) {
                    String name = tag.getName();
                    if (uniqueValues.add(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        }
    }

    public List<String> getOtherMatchingIngredients() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return getCurrentList().stream()
                    .map(RecipeWithTagsAndIngredients::getIngredients)
                    .flatMap(Collection::stream)
                    .map(IngListItem::getName)
                    .distinct()
                    .collect(Collectors.toList());
        }
        else {
            List<String> list = new ArrayList<>();
            Set<String> uniqueValues = new HashSet<>();
            for (RecipeWithTagsAndIngredients recipeWithTagsAndIngredients : getCurrentList()) {
                List<IngListItem> ingredients = recipeWithTagsAndIngredients.getIngredients();
                for (IngListItem ingredient : ingredients) {
                    String name = ingredient.getName();
                    if (uniqueValues.add(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        }
    }

    private void backupFullList(@Nullable List<RecipeWithTagsAndIngredients> list) {
        //update full list
        if (itemsFull != null) {
            itemsFull.clear();
        } else {
            itemsFull = new ArrayList<>();
        }

        if (list != null) {
            itemsFull.addAll(list);
        }
    }


    public void setSearchCriteria(RecipeListFragment.SearchCriteria searchCriteria){
        this.searchCriteria = searchCriteria;
    }

    public void setOrderByCriteria(int criteriaIndex){
        this.orderByCriteria = criteriaIndex;
    }

    public void setLatestConstraint(CharSequence constraint){
        this.latestConstraint = constraint;
    }

    public void sort(int orderingCriteria){
        //set the orderByCriteria
        this.orderByCriteria = orderingCriteria;

        //sort the filtered and full lists
        if(itemsFull != null) {
            List<RecipeWithTagsAndIngredients> sortedListFull = new ArrayList<>(itemsFull);
            this.itemsFull = sortList(sortedListFull);
        }

        if (getCurrentList() != null) {
            List<RecipeWithTagsAndIngredients> sortedList = new ArrayList<>(getCurrentList());

            //submit sorted list to adapter
            this.submitList(sortList(sortedList));
        }
    }

    private List<RecipeWithTagsAndIngredients> sortList(List<RecipeWithTagsAndIngredients> list) {
        //choose a comparator depending on which option was selected by the user
        Comparator<RecipeWithTagsAndIngredients> comparator = null;
        switch (orderByCriteria){
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

        Collections.sort(list, comparator);
        return list;
    }

    public List<RecipeWithTagsAndIngredients> getSelectedItems(){
        List<RecipeWithTagsAndIngredients> selectedItems = new ArrayList<>();
        for(Integer position : selectedPositions){
            selectedItems.add(getCurrentList().get(position));
        }
        return selectedItems;
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
            case NAME:
            default:
                return nameFilter;
        }
    }

    public void filter(CharSequence constraint) {
        latestConstraint = constraint;
        getFilter().filter(constraint);
    }

    public void refilter(){
        filter(latestConstraint);
    }

    /** Filters recipes to match those whose name contains the query string */
    private Filter nameFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<RecipeWithTagsAndIngredients> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                //if recipes haven't finished loading yet, wait until they have
                while (itemsFull == null){
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(itemsFull);
            }
            else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                //find all recipes whose name contains the query string
                for (RecipeWithTagsAndIngredients recipe : itemsFull){
                    if(recipe.getRecipe().getName().toLowerCase().contains(filterPattern)) {
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
            if(filterResults.values != null){
                submitList((List<RecipeWithTagsAndIngredients>) filterResults.values);
            }
        }
    };

    /** Matches recipes which contain ingredients containing each comma-separated string in query */
    private Filter ingredientFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<RecipeWithTagsAndIngredients> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                //if recipes haven't finished loading yet, wait until they have
                while (itemsFull == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(itemsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                String[] ingredientsToMatch = filterPattern.split(",");

                //trim ingredient names
                for (int i = 0; i < ingredientsToMatch.length; i++){
                    ingredientsToMatch[i] = ingredientsToMatch[i].trim();
                }

                //find all recipes who have ingredients matching each of these
                for (RecipeWithTagsAndIngredients recipe : itemsFull) {
                    //this array maintains which ingredients have been matched
                    // as we iterate through the list of ingredients
                    boolean[] found = new boolean[ingredientsToMatch.length];
                    //for each ingredient of the recipe
                    for(IngListItem ing : recipe.getIngredients()) {
                        //check its name against all search parameter ingredients which haven't yet been found
                        int i = 0;
                        while(i < ingredientsToMatch.length && !isAllTrue(found)){
                            if (!found[i] && ing.getName().toLowerCase().contains(ingredientsToMatch[i])) {
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
            if(filterResults.values != null){
                submitList((List<RecipeWithTagsAndIngredients>) filterResults.values);
            }
        }
    };

    /** Matches recipes which contain tags containing each comma-separated string in query */
    private Filter tagFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<RecipeWithTagsAndIngredients> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                //if recipes haven't finished loading yet, wait until they have
                while (itemsFull == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(itemsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                String[] tagsToMatch = filterPattern.split(",");

                //trim ingredient names
                for (int i = 0; i < tagsToMatch.length; i++){
                    tagsToMatch[i] = tagsToMatch[i].trim();
                }

                //find all recipes who have ingredients matching each of these
                for (RecipeWithTagsAndIngredients recipe : itemsFull) {
                    //this array maintains which ingredients have been matched
                    // as we iterate through the list of ingredients
                    boolean[] found = new boolean[tagsToMatch.length];
                    //for each ingredient of the recipe
                    for(Tag tag : recipe.getTags()) {
                        //check its name against all search parameter tags which haven't yet been found
                        int i = 0;
                        while(i < tagsToMatch.length && !isAllTrue(found)){
                            if (!found[i] && tag.getName().toLowerCase().contains(tagsToMatch[i])) {
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
            if(filterResults.values != null){
                submitList((List<RecipeWithTagsAndIngredients>) filterResults.values);
            }
        }
    };

    private boolean isAllTrue(boolean... array){
        for (boolean b: array){
            if(!b) return false;
        }
        return true;
    }

    public void selectAll() {
        selectedPositions.clear();
        for(int i = 0; i < getCurrentList().size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends BaseRecyclerViewAdapter<RecipeWithTagsAndIngredients>.ViewHolder {
        private final View itemView;
        private OnRecipeClickListener onRecipeClickListener;
        private int recipeId;

        public ViewHolder(@NonNull View itemView, OnRecipeClickListener onRecipeClickListener) {
            super(itemView);
            this.itemView = itemView;

            //set click listeners
            this.onRecipeClickListener = onRecipeClickListener;
        }

        public void bind (RecipeWithTagsAndIngredients recipeWithTagsAndIngredients){
            Recipe recipe = recipeWithTagsAndIngredients.getRecipe();
            recipeId = recipe.getId();

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
            for (Tag tag : recipeWithTagsAndIngredients.getTags()){
                //add a sample tag
                Chip chip = (Chip) LayoutInflater.from(itemView.getContext()).inflate(R.layout.tag_chip_recipe_card, null, false);
                chip.setText(tag.getName());

                chipGroup.addView(chip);
            }

            //attach click listeners to both parent, and to tags scrollview (which annoyingly
            //intercepts the click events otherwise)
            itemView.setOnLongClickListener(this::handleLongClick);
            itemView.setOnClickListener(this::handleClick);
        }

        /** Returns the recipeId of the recipe bound to this viewholder*/
        public void handleClick(View ignored) {
            onRecipeClickListener.onRecipeClick(recipeId);
        }

        public boolean handleLongClick(View ignored) {
            //if this item was already selected, deselect
            if(selectedPositions.contains(getAdapterPosition())){
                selectedPositions.remove((Integer) getAdapterPosition());
                itemView.setSelected(false);
            }
            //otherwise select this item
            else{
                selectedPositions.add(getAdapterPosition());
                itemView.setSelected(true);
            }

            //call owner's click handler
            onRecipeClickListener.onRecipeLongPress(itemView, getAdapterPosition());
            return true;
        }
    }

    public interface OnRecipeClickListener {
        void onRecipeClick(int position);
        boolean onRecipeLongPress(View view, int position);
    }
}
