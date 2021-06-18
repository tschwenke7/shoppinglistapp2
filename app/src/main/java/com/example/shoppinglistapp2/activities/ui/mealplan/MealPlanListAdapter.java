package com.example.shoppinglistapp2.activities.ui.mealplan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.recipes.recipelist.RecipeListAdapter;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MealPlanListAdapter extends RecyclerView.Adapter<MealPlanListAdapter.ViewHolder> {

    private List<MealPlan> mealPlans;

    public MealPlanListAdapter(){

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.meal_plan_recyclerview_item, parent, false);
        return new MealPlanListAdapter.ViewHolder(view);
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
        mealPlans = newList;
        notifyDataSetChanged();
        //TODO - implement diffUtil if necessary
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View itemView;
        private MealPlan mealPlan;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        public void bind(MealPlan mealPlan) {
            this.mealPlan = mealPlan;

            /* set day name */
            ((TextView) itemView.findViewById(R.id.day_title)).setText(mealPlan.getDayTitle());

            /* set recipe details if provided - otherwise hide recipe cardview */
            CardView cardView = itemView.findViewById(R.id.recipe_cardview);

            if (null != mealPlan.getRecipe()){
                cardView.setVisibility(View.VISIBLE);
                Recipe recipe = mealPlan.getRecipe();

                //recipe name
                ((TextView) cardView.findViewById(R.id.recipe_title)).setText(recipe.getName());

                //prep time
                ((TextView) cardView.findViewById(R.id.edit_text_prep_time)).setText(recipe.getPrepTime());
                //cook time
                ((TextView) cardView.findViewById(R.id.edit_text_cook_time)).setText(recipe.getCookTime());
            }
            else{
                cardView.setVisibility(View.GONE);
                itemView.findViewById(R.id.choose_recipe_button).setVisibility(View.VISIBLE);
            }

            /* set notes */
        }
    }
}
