package com.example.shoppinglistapp2.activities.ui.recipes.viewrecipe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.Ingredient;

public class IngredientListAdapter extends ListAdapter<Ingredient, IngredientListAdapter.ViewHolder> {
    private IngredientListAdapter.ItemClickListener itemClickListener;
    private boolean editMode = false;

    public IngredientListAdapter(@NonNull DiffUtil.ItemCallback<Ingredient> diffCallback, IngredientListAdapter.ItemClickListener itemClickListener) {
        super(diffCallback);
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ingredient_list_item_editor, parent, false);
        return new ViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ingredient current = getItem(position);
        holder.bind(current);
    }

    public void setEditMode(boolean isEditMode){
        this.editMode = isEditMode;
    }

    public static class IngredientDiff extends DiffUtil.ItemCallback<Ingredient> {

        @Override
        public boolean areItemsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            return oldItem.getId() == newItem.getId();
        }
    }

    public interface ItemClickListener {
        void onDeleteClicked(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView ingredientNameView;

        public ViewHolder(@NonNull View itemView, ItemClickListener itemClickListener) {
            super(itemView);
            ingredientNameView = itemView.findViewById(R.id.ingredient_name);

            //hide delete button if edit mode is disabled
            if(!editMode){
                itemView.findViewById(R.id.delete_icon).setVisibility(View.INVISIBLE);
            }
            else{
                itemView.findViewById(R.id.delete_icon).setVisibility(View.VISIBLE);
                //attach click listener to delete icon
                itemView.findViewById(R.id.delete_icon).setOnClickListener(view ->
                        itemClickListener.onDeleteClicked(getAdapterPosition()));
            }
        }

        public void bind (Ingredient ingredient){
            ingredientNameView.setText(ingredient.toString());
        }
    }
}
