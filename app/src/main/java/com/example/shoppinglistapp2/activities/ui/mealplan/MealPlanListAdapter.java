package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.ui.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListAdapter;
import com.example.shoppinglistapp2.databinding.MealPlanRecyclerviewItemBinding;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;
import com.example.shoppinglistapp2.helpers.KeyboardHider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MealPlanListAdapter extends BaseRecyclerViewAdapter<MealWithRecipe>{

    private MealPlanClickListener mealPlanClickListener;


    public MealPlanListAdapter(MealPlanClickListener mealPlanClickListener){
        this.mealPlanClickListener = mealPlanClickListener;
    }

    @NonNull
    @Override
    public BaseRecyclerViewAdapter<MealWithRecipe>.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MealPlanListAdapter.ViewHolder(
                MealPlanRecyclerviewItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                mealPlanClickListener
        );
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

    public class ViewHolder extends BaseRecyclerViewAdapter<MealWithRecipe>.ViewHolder {
        private final MealPlanRecyclerviewItemBinding binding;
        private final MealPlanClickListener mealPlanClickListener;

        public ViewHolder(@NonNull MealPlanRecyclerviewItemBinding binding, MealPlanClickListener mealPlanClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.mealPlanClickListener = mealPlanClickListener;
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(MealWithRecipe mealWithRecipe) {
            Meal meal = mealWithRecipe.getMeal();
            Recipe recipe = mealWithRecipe.getRecipe();

            View addNotesButton = binding.addPlanNotesButton;
            View chooseRecipeButton = binding.chooseRecipeButton;
            View plusIcon = binding.plusIcon;
            CardView cardView = binding.recipeCardview;
            EditText notesView = binding.mealPlanNotes;

            /* set day name */
            binding.dayTitle.setText(meal.getDayTitle());

            /* Listen for click on day name for editing */
            binding.dayTitle.setOnTouchListener((v, event) -> {
                if(MotionEvent.ACTION_UP == event.getAction()){
                    binding.editDayTitleConfirm.setVisibility(View.VISIBLE);
                    binding.deleteMealIcon.setVisibility(View.VISIBLE);

                    //hide other buttons in title row
                    plusIcon.setVisibility(View.GONE);
                    chooseRecipeButton.setVisibility(View.GONE);
                    addNotesButton.setVisibility(View.GONE);
                }
                return false;
            });

            /* Listen for title change confirm "tick icon" clicked, and tell fragment to update */
            binding.editDayTitleConfirm.setOnClickListener(view -> {
                mealPlanClickListener.onTitleConfirmClicked(getAdapterPosition(), binding.dayTitle.getText().toString());
                binding.dayTitle.clearFocus();
                binding.editDayTitleConfirm.setVisibility(View.GONE);
                binding.deleteMealIcon.setVisibility(View.GONE);

                //restore applicable buttons in title row
                if(cardView.getVisibility() == View.GONE){
                    chooseRecipeButton.setVisibility(View.VISIBLE);
                }
                if(notesView.getVisibility() == View.GONE){
                    addNotesButton.setVisibility(View.VISIBLE);
                }
                if(chooseRecipeButton.getVisibility() == View.VISIBLE || addNotesButton.getVisibility() == View.VISIBLE){
                    plusIcon.setVisibility(View.VISIBLE);
                }
            });

            /* Listen for delete meal icon click */
            binding.deleteMealIcon.setOnClickListener((view) -> {
                mealPlanClickListener.onDeleteMealClicked(getAdapterPosition());
                binding.dayTitle.clearFocus();
            });

            /* set recipe details if provided - otherwise hide recipe cardview */
            if (null != recipe){
                chooseRecipeButton.setVisibility(View.GONE);
                cardView.setVisibility(View.VISIBLE);

                //recipe name
                binding.recipeTitle.setText(recipe.getName());

                //set prep and cook times
                String timeUnit = itemView.getContext().getString(R.string.abbreviated_time_unit);

                if(0 != recipe.getPrepTime()){
                    binding.editTextPrepTime.setText(String.format("%d %s",recipe.getPrepTime(), timeUnit));
                }
                else{
                    binding.editTextPrepTime.setText("-");
                }
                if(0 != recipe.getCookTime()){
                    binding.cookTime.setText(String.format("%d %s",recipe.getCookTime(),timeUnit));
                }
                else{
                    binding.cookTime.setText("-");
                }
            }
            else{
                cardView.setVisibility(View.GONE);
                chooseRecipeButton.setVisibility(View.VISIBLE);
            }

            // set click listener for recipe
            cardView.setOnClickListener(v -> mealPlanClickListener.onRecipeClicked(getAdapterPosition()));
            // set click listener for choose recipe button
            chooseRecipeButton
                    .setOnClickListener(v -> mealPlanClickListener.onChooseRecipeClicked(getAdapterPosition()));
            //set click listener for delete recipe icon
            binding.deleteIcon
                    .setOnClickListener(v -> mealPlanClickListener.onRemoveRecipeClicked(getAdapterPosition()));

            /* set notes if provided, and edit notes listeners */
            //set values of notes
            if (meal.getNotes() != null) {
                notesView.setText(meal.getNotes());

                notesView.setVisibility(View.VISIBLE);
                addNotesButton.setVisibility(View.GONE);
            }
            else{
                notesView.setVisibility(View.GONE);
                addNotesButton.setVisibility(View.VISIBLE);
            }

            //set listener for notes clicked to enable save button
            notesView.setOnTouchListener((v, event) -> {
                if (MotionEvent.ACTION_UP == event.getAction()) {
                    binding.editNotesConfirm.setVisibility(View.VISIBLE);
                    binding.deleteNotes.setVisibility(View.VISIBLE);
                }
                return false;
            });

            binding.editNotesConfirm.setOnClickListener((view) -> {
                mealPlanClickListener.onNotesConfirmClicked(getAdapterPosition(), notesView.getText().toString());
                binding.editNotesConfirm.setVisibility(View.GONE);
                binding.deleteNotes.setVisibility(View.GONE);
                notesView.clearFocus();
            });

            binding.deleteNotes.setOnClickListener((view) -> {
                mealPlanClickListener.onDeleteNotesClicked(getAdapterPosition());
                notesView.clearFocus();
                notesView.setVisibility(View.GONE);
                binding.editNotesConfirm.setVisibility(View.GONE);
                binding.deleteNotes.setVisibility(View.GONE);
                addNotesButton.setVisibility(View.VISIBLE);
                plusIcon.setVisibility(View.VISIBLE);
            });

            //listen to add notes button
            addNotesButton.setOnClickListener((view) -> {
                addNotesButton.setVisibility(View.GONE);
                notesView.setVisibility(View.VISIBLE);

                //if recipe and notes are both provided, we can remove the plus icon too
                if(chooseRecipeButton.getVisibility() == View.GONE && addNotesButton.getVisibility() == View.GONE){
                    binding.plusIcon.setVisibility(View.GONE);
                }
            });

            //if recipe and notes are both provided, we can remove the plus icon too
            if(chooseRecipeButton.getVisibility() == View.GONE && addNotesButton.getVisibility() == View.GONE){
                binding.plusIcon.setVisibility(View.GONE);
            }
        }
    }

    public interface MealPlanClickListener {
        void onTitleConfirmClicked(int position, String newTitle);
        void onNotesConfirmClicked(int position, String newNotes);
        void onDeleteNotesClicked(int position);
        void onChooseRecipeClicked(int position);
        void onRecipeClicked(int position);
        void onRemoveRecipeClicked(int position);
        void onDeleteMealClicked(int position);
    }
}
