package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.ui.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewMealPlanMealContentBinding;
import com.example.shoppinglistapp2.databinding.RecyclerviewMealPlanMealTitleBinding;
import com.example.shoppinglistapp2.db.tables.Meal;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.relations.MealWithRecipe;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MealPlanListAdapter extends BaseRecyclerViewAdapter<MealWithRecipe>{

    private MealPlanClickListener mealPlanClickListener;


    public MealPlanListAdapter(MealPlanClickListener mealPlanClickListener){
        this.mealPlanClickListener = mealPlanClickListener;
    }

    @Override
    public void submitList(List<MealWithRecipe> newItems, @Nullable Runnable callback) {
        //duplicate each item. We will inflate 2 viewholders for each item -
        // 1st as the header, 2nd as the content using getItemViewType
        List<MealWithRecipe> doubledUpList = new ArrayList<>();
        for (MealWithRecipe item : newItems) {
            doubledUpList.add(item);
            doubledUpList.add(item);
        }

        super.submitList(doubledUpList, callback);
    }

    /**
     * Collapse doubled up list back into list of meals to return
     * with any reordering that might have happened, reading to be updated in the db.
     * @return the list of Meals represented by this adapter.
     */
    public List<Meal> getMealsToPersist() {
        List<MealWithRecipe> doubledUpList = getCurrentList();
        List<Meal> mealsList = new ArrayList<>();

        //iterate through each pair of meal header/content viewholders
        for (int i = 0; i < doubledUpList.size(); i+=2) {
            mealsList.add(doubledUpList.get(i).getMeal());
        }

        return mealsList;
    }

    @Override
    public int getItemViewType(int position) {
        return position % 2;
    }

    @NonNull
    @Override
    public BaseRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            //Render a title element
            case 0:
                return new TitleViewHolder(
                        RecyclerviewMealPlanMealTitleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                        mealPlanClickListener
                );
            //render a content element
            case 1:
            default:
                return new ContentViewHolder(
                        RecyclerviewMealPlanMealContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                        mealPlanClickListener
                );
        }
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

    public void swap(int fromPos, int toPos) {
        //Change the underlying data of the items so they don't get animated
        //when the updated list is submitted
        MealWithRecipe fromMeal = getItem(fromPos);
        MealWithRecipe toMeal = getItem(toPos);

        MealWithRecipe swappedMealTo = new MealWithRecipe();
        swappedMealTo.setMeal(toMeal.getMeal().deepCopy());
        swappedMealTo.getMeal().setRecipeId(fromMeal.getMeal().getRecipeId());
        swappedMealTo.setRecipe(fromMeal.getRecipe());
        swappedMealTo.getMeal().setNotes(fromMeal.getMeal().getNotes());

        MealWithRecipe swappedMealFrom = new MealWithRecipe();
        swappedMealFrom.setMeal(fromMeal.getMeal().deepCopy());
        swappedMealFrom.getMeal().setRecipeId(toMeal.getMeal().getRecipeId());
        swappedMealFrom.setRecipe(toMeal.getRecipe());
        swappedMealFrom.getMeal().setNotes(toMeal.getMeal().getNotes());

        getCurrentList().set(toPos, swappedMealTo);
        getCurrentList().set(toPos - 1, swappedMealTo);

        getCurrentList().set(fromPos, swappedMealFrom);
        getCurrentList().set(fromPos - 1, swappedMealFrom);

        notifyItemMoved(fromPos,toPos);
    }

    public class TitleViewHolder extends BaseRecyclerViewAdapter<MealWithRecipe>.ViewHolder {
        private final RecyclerviewMealPlanMealTitleBinding binding;
        private final MealPlanClickListener mealPlanClickListener;

        public TitleViewHolder(@NonNull RecyclerviewMealPlanMealTitleBinding binding, MealPlanClickListener mealPlanClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.mealPlanClickListener = mealPlanClickListener;
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(MealWithRecipe mealWithRecipe) {
            Meal meal = mealWithRecipe.getMeal();

            /* set day name */
            binding.dayTitle.setText(meal.getDayTitle());

            /* Set default visibilities */
            binding.editDayTitleConfirm.setVisibility(View.GONE);
            binding.deleteMealIcon.setVisibility(View.GONE);

            /* Listen for click on day name for editing */
            binding.dayTitle.setOnTouchListener((v, event) -> {
                if(MotionEvent.ACTION_UP == event.getAction()){
                    binding.editDayTitleConfirm.setVisibility(View.VISIBLE);
                    binding.deleteMealIcon.setVisibility(View.VISIBLE);
                }
                return false;
            });

            /* Listen for title change confirm "tick icon" clicked, and tell fragment to update */
            binding.editDayTitleConfirm.setOnClickListener(view -> {
                mealPlanClickListener.onTitleConfirmClicked(
                        getAdapterPosition()/2, binding.dayTitle.getText().toString());
                binding.dayTitle.clearFocus();
                binding.editDayTitleConfirm.setVisibility(View.GONE);
                binding.deleteMealIcon.setVisibility(View.GONE);
            });

            /* Listen for delete meal icon click */
            binding.deleteMealIcon.setOnClickListener((view) -> {
                mealPlanClickListener.onDeleteMealClicked(getAdapterPosition()/2);
                binding.dayTitle.clearFocus();
            });
        }
    }

    public class ContentViewHolder extends BaseRecyclerViewAdapter<MealWithRecipe>.ViewHolder {
        private final MealPlanClickListener clickListener;
        private final RecyclerviewMealPlanMealContentBinding binding;

        public ContentViewHolder(RecyclerviewMealPlanMealContentBinding binding, MealPlanClickListener clickListener) {
            super(binding.getRoot());
            this.clickListener = clickListener;
            this.binding = binding;
        }

        @Override
        public void bind(MealWithRecipe mealWithRecipe) {
            Meal meal = mealWithRecipe.getMeal();
            Recipe recipe = mealWithRecipe.getRecipe();

            /* set recipe details if provided - otherwise hide recipe cardview */
            if (null != recipe){
                binding.recipeCardview.setVisibility(View.VISIBLE);
                binding.addRecipeButton.setVisibility(View.GONE);

                //recipe name
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
            }
            else{
                binding.recipeCardview.setVisibility(View.GONE);
                binding.addRecipeButton.setVisibility(View.VISIBLE);
            }

            binding.addRecipeButton.setOnClickListener((v) -> {
                clickListener.onChooseRecipeClicked((getAdapterPosition()-1)/2);
            });

            // set click listener for recipe
            binding.recipeCardview.setOnClickListener(v -> mealPlanClickListener.onRecipeClicked((getAdapterPosition()-1)/2));

            //set click listener for delete recipe icon
            binding.deleteIcon
                    .setOnClickListener(v -> mealPlanClickListener.onRemoveRecipeClicked((getAdapterPosition()-1)/2));

            /* set notes if provided, and edit notes listeners */
            //set values of notes
            if (meal.getNotes() != null) {
                binding.mealPlanNotes.setText(meal.getNotes());
                binding.mealPlanNotes.setVisibility(View.VISIBLE);
                binding.addNotesButton.setVisibility(View.GONE);
            }
            else{
                binding.mealPlanNotes.setText("");
                binding.mealPlanNotes.setVisibility(View.GONE);
                binding.addNotesButton.setVisibility(View.VISIBLE);
            }

            //set listener for notes clicked to enable save button
            binding.mealPlanNotes.setOnTouchListener((v, event) -> {
                if (MotionEvent.ACTION_UP == event.getAction()) {
                    binding.editNotesConfirm.setVisibility(View.VISIBLE);
                    binding.deleteNotes.setVisibility(View.VISIBLE);
                    v.performClick();
                }
                return false;
            });

            binding.editNotesConfirm.setOnClickListener((view) -> {
                mealPlanClickListener.onNotesConfirmClicked((getAdapterPosition()-1)/2, binding.mealPlanNotes.getText().toString());
                binding.editNotesConfirm.setVisibility(View.GONE);
                binding.deleteNotes.setVisibility(View.GONE);
                binding.mealPlanNotes.clearFocus();
                //set notes for internal list, since livedata update won't trigger here for some reason
//                getItem(getAdapterPosition()).getMeal().setNotes(binding.mealPlanNotes.getText().toString());
            });

            binding.deleteNotes.setOnClickListener((view) -> {
                mealPlanClickListener.onDeleteNotesClicked((getAdapterPosition()-1)/2);
                binding.mealPlanNotes.clearFocus();
                binding.mealPlanNotes.setVisibility(View.GONE);
                binding.editNotesConfirm.setVisibility(View.GONE);
                binding.deleteNotes.setVisibility(View.GONE);
                binding.addNotesButton.setVisibility(View.VISIBLE);
            });

            //listen to add notes button
            binding.addNotesButton.setOnClickListener((view) -> {
                binding.addNotesButton.setVisibility(View.GONE);
                binding.mealPlanNotes.setVisibility(View.VISIBLE);
            });

            binding.handle.setOnTouchListener((v, event) -> {
                clickListener.startDragging(this);
                return true;
            });
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
        void startDragging(ViewHolder viewHolder);
    }

}
