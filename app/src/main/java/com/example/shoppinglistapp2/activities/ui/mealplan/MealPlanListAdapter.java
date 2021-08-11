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

            View addNotesButton = itemView.findViewById(R.id.add_plan_notes_button);
            View chooseRecipeButton = itemView.findViewById(R.id.choose_recipe_button);
            View plusIcon = itemView.findViewById(R.id.plus_icon);
            CardView cardView = itemView.findViewById(R.id.recipe_cardview);
            EditText notesView = (EditText) itemView.findViewById(R.id.meal_plan_notes);

            /* set day name */
            TextView dayTitle = itemView.findViewById(R.id.day_title);
//            dayTitle.setText(mealPlan.getDayTitle());

            /* Listen for click on day name for editing */
            View confirmDayTitle = itemView.findViewById(R.id.edit_day_title_confirm);
            View deleteMealIcon = itemView.findViewById(R.id.delete_meal_icon);

            dayTitle.setOnTouchListener((v, event) -> {
                if(MotionEvent.ACTION_UP == event.getAction()){
                    confirmDayTitle.setVisibility(View.VISIBLE);
                    deleteMealIcon.setVisibility(View.VISIBLE);

                    //hide other buttons in title row
                    plusIcon.setVisibility(View.GONE);
                    chooseRecipeButton.setVisibility(View.GONE);
                    addNotesButton.setVisibility(View.GONE);
                }
                return false;
            });

            /* Listen for title change confirm "tick icon" clicked, and tell fragment to update */
            confirmDayTitle.setOnClickListener(view -> {
                mealPlanClickListener.onTitleConfirmClicked(getAdapterPosition(), dayTitle.getText().toString());
                dayTitle.clearFocus();
                confirmDayTitle.setVisibility(View.GONE);
                deleteMealIcon.setVisibility(View.GONE);

                //restore applicable buttons in title row
                if(cardView.getVisibility() == View.GONE){
                    chooseRecipeButton.setVisibility(View.VISIBLE);
                }
                if(notesView.getVisibility() == View.GONE){
                    addNotesButton.setVisibility(View.VISIBLE);
                }
                if(chooseRecipeButton.getVisibility() == View.VISIBLE || addNotesButton.getVisibility() == View.VISIBLE){
                    itemView.findViewById(R.id.plus_icon).setVisibility(View.VISIBLE);
                }
            });

            /* Listen for delete meal icon click */
            deleteMealIcon.setOnClickListener((view) -> {
                mealPlanClickListener.onDeleteMealClicked(getAdapterPosition());
                dayTitle.clearFocus();
            });

            /* set recipe details if provided - otherwise hide recipe cardview */
//            if (null != mealPlan.getRecipe()){
//                chooseRecipeButton.setVisibility(View.GONE);
//                cardView.setVisibility(View.VISIBLE);
//
//                Recipe recipe = mealPlan.getRecipe();
//                //recipe name
//                ((TextView) cardView.findViewById(R.id.recipe_title)).setText(recipe.getName());
//
//                //set prep and cook times
//                String timeUnit = itemView.getContext().getString(R.string.abbreviated_time_unit);
//                TextView prepTimeView = itemView.findViewById(R.id.edit_text_prep_time);
//                if(0 != recipe.getPrepTime()){
//                    prepTimeView.setText(String.format("%d %s",recipe.getPrepTime(), timeUnit));
//                }
//                else{
//                    prepTimeView.setText("-");
//                }
//                TextView cookTimeView = itemView.findViewById(R.id.cook_time);
//                if(0 != recipe.getCookTime()){
//                    cookTimeView.setText(String.format("%d %s",recipe.getCookTime(),timeUnit));
//                }
//                else{
//                    cookTimeView.setText("-");
//                }
//            }
//            else{
//                cardView.setVisibility(View.GONE);
//                chooseRecipeButton.setVisibility(View.VISIBLE);
//            }
//
//            // set click listener for recipe
//            cardView.setOnClickListener(v -> mealPlanClickListener.onRecipeClicked(getAdapterPosition()));
//            // set click listener for choose recipe button
//            chooseRecipeButton
//                    .setOnClickListener(v -> mealPlanClickListener.onChooseRecipeClicked(getAdapterPosition()));
//            //set click listener for delete recipe icon
//            itemView.findViewById(R.id.delete_icon)
//                    .setOnClickListener(v -> mealPlanClickListener.onRemoveRecipeClicked(getAdapterPosition()));
//
//            /* set notes if provided, and edit notes listeners */
//            //set values of notes
//            if (mealPlan.getNotes() != null) {
//                notesView.setText(mealPlan.getNotes());
//
//                notesView.setVisibility(View.VISIBLE);
//                addNotesButton.setVisibility(View.GONE);
//            }
//            else{
//                notesView.setVisibility(View.GONE);
//                addNotesButton.setVisibility(View.VISIBLE);
//            }
//
//            //set listener for notes clicked to enable save button
//            View confirmNotes = itemView.findViewById(R.id.edit_notes_confirm);//button to click to save notes
//            View deleteNotes = itemView.findViewById(R.id.delete_notes);//button to click to delete notes
//            notesView.setOnTouchListener((v, event) -> {
//                if (MotionEvent.ACTION_UP == event.getAction()) {
//                    confirmNotes.setVisibility(View.VISIBLE);
//                    deleteNotes.setVisibility(View.VISIBLE);
//                }
//                return false;
//            });
//
//            confirmNotes.setOnClickListener((view) -> {
//                mealPlanClickListener.onNotesConfirmClicked(getAdapterPosition(), notesView.getText().toString());
//                confirmNotes.setVisibility(View.GONE);
//                deleteNotes.setVisibility(View.GONE);
//                notesView.clearFocus();
//            });
//
//            deleteNotes.setOnClickListener((view) -> {
//                mealPlanClickListener.onDeleteNotesClicked(getAdapterPosition());
//                notesView.clearFocus();
//                notesView.setVisibility(View.GONE);
//                confirmNotes.setVisibility(View.GONE);
//                deleteNotes.setVisibility(View.GONE);
//                addNotesButton.setVisibility(View.VISIBLE);
//                plusIcon.setVisibility(View.VISIBLE);
//            });
//
//            //listen to add notes button
//            addNotesButton.setOnClickListener((view) -> {
//                addNotesButton.setVisibility(View.GONE);
//                notesView.setVisibility(View.VISIBLE);
//
//                //if recipe and notes are both provided, we can remove the plus icon too
//                if(chooseRecipeButton.getVisibility() == View.GONE && addNotesButton.getVisibility() == View.GONE){
//                    itemView.findViewById(R.id.plus_icon).setVisibility(View.GONE);
//                }
//            });
//
//            //if recipe and notes are both provided, we can remove the plus icon too
//            if(chooseRecipeButton.getVisibility() == View.GONE && addNotesButton.getVisibility() == View.GONE){
//                itemView.findViewById(R.id.plus_icon).setVisibility(View.GONE);
//            }
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
