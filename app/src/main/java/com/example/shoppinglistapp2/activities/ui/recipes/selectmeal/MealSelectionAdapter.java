package com.example.shoppinglistapp2.activities.ui.recipes.selectmeal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.ui.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewMealPlanMealSelectModeBinding;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;

import java.util.List;

public class MealSelectionAdapter extends BaseRecyclerViewAdapter<MealWithRecipe> {
    private final ClickListener clickListener;

    public MealSelectionAdapter(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    protected BaseDiffCallback<MealWithRecipe> createDiffCallback(List<MealWithRecipe> newList, List<MealWithRecipe> oldList) {
        return new BaseDiffCallback<MealWithRecipe>(newList, oldList) {
            @Override
            public boolean areItemsTheSame(MealWithRecipe oldItem, MealWithRecipe newItem) {
                return oldItem.getMeal().getId() == newItem.getMeal().getId();
            }

            @Override
            public boolean areContentsTheSame(MealWithRecipe oldItem, MealWithRecipe newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(RecyclerviewMealPlanMealSelectModeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false),
                clickListener);
    }

    public class ViewHolder extends BaseRecyclerViewAdapter<MealWithRecipe>.ViewHolder {
        private final RecyclerviewMealPlanMealSelectModeBinding binding;
        private final ClickListener clickListener;

        public ViewHolder(RecyclerviewMealPlanMealSelectModeBinding binding, ClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
        }

        @Override
        public void bind(MealWithRecipe item) {
            binding.dayTitle.setText(item.getMeal().getDayTitle());

            //fill in recipe if provided
            Recipe recipe = item.getRecipe();
            if(recipe != null) {
                String timeUnit = itemView.getContext().getString(R.string.abbreviated_time_unit);
                binding.recipeTitle.setText(recipe.getName());
                binding.prepTime.setText(String.format("%d %s", recipe.getPrepTime(), timeUnit));
                binding.cookTime.setText(String.format("%d %s", recipe.getCookTime(), timeUnit));
            }
            else {
                binding.recipeCardview.setVisibility(View.GONE);
            }

            //fill in notes if they exist
            if (item.getMeal().getNotes() != null) {
                binding.mealPlanNotes.setText(item.getMeal().getNotes());
            }
            else{
                binding.mealPlanNotes.setVisibility(View.GONE);
            }

            //set select listener
            binding.getRoot().setOnClickListener((v) -> clickListener.onMealClicked(getAdapterPosition()));
        }
    }

    public interface ClickListener {
        void onMealClicked(int pos);
    }
}
