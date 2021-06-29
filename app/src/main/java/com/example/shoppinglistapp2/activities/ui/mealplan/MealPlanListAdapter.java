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
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListAdapter;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.helpers.KeyboardHider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MealPlanListAdapter extends RecyclerView.Adapter<MealPlanListAdapter.ViewHolder> {

    private List<MealPlan> mealPlans;
    private MealPlanClickListener mealPlanClickListener;


    public MealPlanListAdapter(MealPlanClickListener mealPlanClickListener){
        this.mealPlanClickListener = mealPlanClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.meal_plan_recyclerview_item, parent, false);
        return new MealPlanListAdapter.ViewHolder(view, mealPlanClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        MealPlan current = mealPlans.get(position);
        holder.bind(current);
    }

    @Override
    public int getItemCount() {
        if(mealPlans != null){
            return mealPlans.size();
        }
        return 0;
    }

    public void setList(List<MealPlan> newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MealPlanDiff(newList, mealPlans));
        diffResult.dispatchUpdatesTo(this);
        this.mealPlans = newList;

//        mealPlans = newList;
//        notifyDataSetChanged();
    }

    public static class MealPlanDiff extends DiffUtil.Callback {
        List<MealPlan> newList;
        List<MealPlan> oldList;

        public MealPlanDiff(List<MealPlan> newList, List<MealPlan> oldList) {
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
            if(newList == null){
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View itemView;
        private MealPlan mealPlan;
        private MealPlanClickListener mealPlanClickListener;

        public ViewHolder(@NonNull @NotNull View itemView, MealPlanClickListener mealPlanClickListener) {
            super(itemView);
            this.itemView = itemView;
            this.mealPlanClickListener = mealPlanClickListener;
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(MealPlan mealPlan) {
            this.mealPlan = mealPlan;

            /* set day name */
            TextView dayTitle = itemView.findViewById(R.id.day_title);
            dayTitle.setText(mealPlan.getDayTitle());

            /* Listen for click on day name for editing */
            View confirmDayTitle = itemView.findViewById(R.id.edit_day_title_confirm);
            dayTitle.setOnTouchListener((v, event) -> {
                if(MotionEvent.ACTION_UP == event.getAction()){
                    confirmDayTitle.setVisibility(View.VISIBLE);
                }
                return false;
            });

            /* Listen for title change confirm "tick icon" clicked, and tell fragment to update */
            confirmDayTitle.setOnClickListener(view -> {
                mealPlanClickListener.onTitleConfirmClicked(getAdapterPosition(), dayTitle.getText().toString());
                dayTitle.clearFocus();
                confirmDayTitle.setVisibility(View.GONE);
            });

            /* set recipe details if provided - otherwise hide recipe cardview */
            CardView cardView = itemView.findViewById(R.id.recipe_cardview);

            if (null != mealPlan.getRecipe()){
                itemView.findViewById(R.id.choose_recipe_button).setVisibility(View.GONE);
                cardView.setVisibility(View.VISIBLE);
                Recipe recipe = mealPlan.getRecipe();

                //recipe name
                ((TextView) cardView.findViewById(R.id.recipe_title)).setText(recipe.getName());

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
            }
            else{
                cardView.setVisibility(View.GONE);
                itemView.findViewById(R.id.choose_recipe_button).setVisibility(View.VISIBLE);
            }

            // set click listener for recipe
            cardView.setOnClickListener(v -> mealPlanClickListener.onRecipeClicked(getAdapterPosition()));
            // set click listener for choose recipe button
            itemView.findViewById(R.id.choose_recipe_button)
                    .setOnClickListener(v -> mealPlanClickListener.onChooseRecipeClicked(getAdapterPosition()));
            //set click listener for delete recipe icon
            itemView.findViewById(R.id.delete_icon)
                    .setOnClickListener(v -> mealPlanClickListener.onRemoveRecipeClicked(getAdapterPosition()));

            /* set notes and edit notes listeners */
            //set values of notes
            EditText notesView = (EditText) itemView.findViewById(R.id.meal_plan_notes);
            if (mealPlan.getNotes() != null) {
                notesView.setText(mealPlan.getNotes());
            }

            //set listener for notes clicked to enable save button
            View confirmNotes = itemView.findViewById(R.id.edit_notes_confirm);//button to click to save notes
            notesView.setOnTouchListener((v, event) -> {
                if (MotionEvent.ACTION_UP == event.getAction()) {
                    confirmNotes.setVisibility(View.VISIBLE);
                }
                return false;
            });

            confirmNotes.setOnClickListener((view) -> {
                mealPlanClickListener.onNotesConfirmClicked(getAdapterPosition(), notesView.getText().toString());
                confirmNotes.setVisibility(View.GONE);
                notesView.clearFocus();
            });

        }
    }

    public interface MealPlanClickListener {
        void onTitleConfirmClicked(int position, String newTitle);
        void onNotesConfirmClicked(int position, String newNotes);
        void onChooseRecipeClicked(int position);
        void onRecipeClicked(int position);
        void onRemoveRecipeClicked(int position);
    }
}
