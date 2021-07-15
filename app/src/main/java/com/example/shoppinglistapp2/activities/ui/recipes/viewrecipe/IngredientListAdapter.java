package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class IngredientListAdapter extends RecyclerView.Adapter<IngredientListAdapter.ViewHolder> {

    private List<Ingredient> ingredients;
    private IngredientClickListener ingredientClickListener;
    private boolean editMode = false;
    private List<Integer> deselectedPositions;

    public IngredientListAdapter(IngredientClickListener ingredientClickListener){
        this.ingredientClickListener = ingredientClickListener;
        deselectedPositions = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ingredient_list_item, parent, false);
        return new IngredientListAdapter.ViewHolder(view, ingredientClickListener);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(ingredients.get(position));
    }

    @Override
    public int getItemCount() {
        if(null == ingredients){
            return 0;
        }
        return ingredients.size();
    }

    public void setList(List<Ingredient> newList){
        //clear any selection/deselection info, as the list may have changed
        // and invalidated the recorded positions
        resetSelections();

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new IngredientDiff(newList, ingredients));
        diffResult.dispatchUpdatesTo(this);
        ingredients = newList;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void resetSelections(){
        if(null != ingredients)
            deselectedPositions.clear();
    }

    /**
     * Compiles and returns a list of all Ingredients which haven't been manually checked off the
     * recipe's ingredients list by the user.
     * @return all Ingredients which remain selected within this adapter
     */
    public List<Ingredient> getSelectedIngredients(){
        List<Ingredient> selected = new ArrayList<>();

        //add all ingredients which haven't been deselected by the user
        for (int i = 0; i < ingredients.size(); i++){
            if(!deselectedPositions.contains(i)){
                selected.add(ingredients.get(i));
            }
        }

        return selected;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private IngredientClickListener ingredientClickListener;
        private View itemView;

        public ViewHolder(@NonNull View itemView, IngredientClickListener ingredientClickListener) {
            super(itemView);
            this.itemView = itemView;
            this.ingredientClickListener = ingredientClickListener;
        }

        public void bind(Ingredient ingredient){
            //fill ingredient name
            CheckBox checkBoxView = itemView.findViewById(R.id.ingredient_name);
            checkBoxView.setText(ingredient.toString());
            checkBoxView.setChecked(true);

            //show or hide delete icon depending on whether edit mode is active
            View deleteIcon = itemView.findViewById(R.id.delete_icon);
            if(editMode){
                View confirmEditButton = itemView.findViewById(R.id.confirm_icon);
                EditText editText = itemView.findViewById(R.id.edit_text_modify_ingredient);

                deleteIcon.setVisibility(View.VISIBLE);
                //set listener for when delete icon clicked
                deleteIcon.setOnClickListener((view) -> {
                    ingredientClickListener.onDeleteClicked(getAdapterPosition());
                });

                //hide checkbox
                Drawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
                checkBoxView.setButtonDrawable(transparentDrawable);

                //set longclick listener for editing ingredients
                checkBoxView.setOnLongClickListener(v -> {
                    //hide textview and delete button
                    checkBoxView.setVisibility(View.GONE);
                    deleteIcon.setVisibility(View.GONE);

                    //show edittext and confirm button
                    editText.setText(checkBoxView.getText());
                    editText.setVisibility(View.VISIBLE);
                    editText.requestFocus();

                    confirmEditButton.setVisibility(View.VISIBLE);
                    return true;
                });

                //set listener for confirm edit button
                confirmEditButton.setOnClickListener(v -> {
                    //copy new text into checkbox
                    checkBoxView.setText(editText.getText());

                    //invert visible components back to default
                    editText.setVisibility(View.GONE);
                    confirmEditButton.setVisibility(View.GONE);
                    checkBoxView.setVisibility(View.VISIBLE);
                    deleteIcon.setVisibility(View.VISIBLE);

                    //notify parent fragment to update db
                    ingredientClickListener.onConfirmEditClicked(getAdapterPosition(),
                            editText.getText().toString());
                });
            }
            else{
                deleteIcon.setVisibility(View.GONE);

                //listen for select/deselect clicks
                checkBoxView.setOnClickListener((view -> {
                    //if deselected, reselect and remove from deselected list
                    if(deselectedPositions.contains(getAdapterPosition())){
                        deselectedPositions.remove(Integer.valueOf(getAdapterPosition()));

                        //remove strikethrough and restore text colour
                        checkBoxView.setPaintFlags(checkBoxView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                        checkBoxView.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_text_default));
                    }
                    //if selected, reselect and add to deselected list
                    else{
                        deselectedPositions.add(getAdapterPosition());

                        //add strikethrough and fade text
                        checkBoxView.setPaintFlags(checkBoxView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        checkBoxView.setTextColor(0xffd3d3d3);
                    }
                }));
            }
        }
    }

    private class IngredientDiff extends DiffUtil.Callback {
        List<Ingredient> newList;
        List<Ingredient> oldList;

        public IngredientDiff(List<Ingredient> newList, List<Ingredient> oldList) {
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

    public interface IngredientClickListener{
        void onDeleteClicked(int position);
        void onConfirmEditClicked(int position, String newIngredientText);
    }
}
