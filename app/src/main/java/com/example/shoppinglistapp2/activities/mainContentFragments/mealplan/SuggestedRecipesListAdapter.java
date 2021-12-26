package com.example.shoppinglistapp2.activities.mainContentFragments.mealplan;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewSuggestedRecipeBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.withextras.PopulatedRecipeWithScore;

import java.text.DecimalFormat;
import java.util.List;

public class SuggestedRecipesListAdapter extends BaseRecyclerViewAdapter<PopulatedRecipeWithScore> {
    private DecimalFormat ratingFormat = new DecimalFormat("#.#");
    private final ClickListener clickListener;

    public SuggestedRecipesListAdapter(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    protected BaseDiffCallback<PopulatedRecipeWithScore> createDiffCallback(List<PopulatedRecipeWithScore> newList, List<PopulatedRecipeWithScore> oldList) {
        return new BaseDiffCallback<PopulatedRecipeWithScore>(newList, oldList) {
            @Override
            public boolean areItemsTheSame(PopulatedRecipeWithScore oldItem, PopulatedRecipeWithScore newItem) {
                return oldItem.getRecipe().getId() == newItem.getRecipe().getId();
            }

            @Override
            public boolean areContentsTheSame(PopulatedRecipeWithScore oldItem, PopulatedRecipeWithScore newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SuggestedRecipesListAdapter.ViewHolder(
            RecyclerviewSuggestedRecipeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                clickListener);
    }

    public class ViewHolder extends BaseRecyclerViewAdapter<PopulatedRecipeWithScore>.ViewHolder {
        private final RecyclerviewSuggestedRecipeBinding binding;
        private final ClickListener clickListener;

        public ViewHolder(RecyclerviewSuggestedRecipeBinding binding, ClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
        }

        @Override
        public void bind(PopulatedRecipeWithScore item) {
            Recipe recipe = item.getRecipe();

            //set recipe name
            binding.recipeTitle.setText(recipe.getName());

            //set prep and cook times
            String timeUnit = itemView.getContext().getString(R.string.abbreviated_time_unit);
            if(0 != recipe.getPrepTime()){
                binding.prepTime.setText(String.format("%d %s",recipe.getPrepTime(), timeUnit));
            }
            else{
                binding.prepTime.setText("-");
            }

            if(0 != recipe.getCookTime()){
                binding.cookTime.setText(String.format("%d %s",recipe.getCookTime(),timeUnit));
            }
            else{
                binding.cookTime.setText("-");
            }

            //set ratings
            if(recipe.getTier_rating() != 0){
                float tierRating = ((float) recipe.getTier_rating()) / 2.0f;
                binding.tierRating.setText(ratingFormat.format(tierRating));
            }
            else{
                binding.tierRating.setText("-");
            }

            if(recipe.getTom_rating() != 0){
                float tomRating = ((float) recipe.getTom_rating()) / 2.0f;
                binding.tomRating.setText(ratingFormat.format(tomRating));
            }
            else{
                binding.tomRating.setText("-");
            }

            /* set ingredients text */
            //compile html text content, with matched items bolded
            StringBuilder stringBuilder = new StringBuilder();
            for (IngListItem ingredient : item.getIngredients()) {
                if(!ingredient.isChecked()) {
                    stringBuilder.append("<b>").append(ingredient.getName()).append("</b>");
                }
                else {
                    stringBuilder.append(ingredient.getName());
                }
                stringBuilder.append(", ");
            }
            String htmlIngString = stringBuilder.toString();
            //remove trailing comma
            htmlIngString = htmlIngString.replaceAll(",$", "");
            //remove trailing comma inside of bold content
            htmlIngString = htmlIngString.replaceAll(",</b>$", "");

            binding.recipeIngredients.setText(HtmlCompat.fromHtml(htmlIngString, HtmlCompat.FROM_HTML_MODE_LEGACY));

            binding.getRoot().setOnClickListener((v) -> clickListener.onSuggestionClicked(recipe.getId()));
        }
    }

    public interface ClickListener {
        void onSuggestionClicked(int pos);
    }
}
