package com.example.shoppinglistapp2.activities.ui.recipes.editor;

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

public class IngredientListEditorAdapter extends ListAdapter<Ingredient, IngredientListEditorAdapter.ViewHolder> {
    private IngredientListEditorAdapter.ItemClickListener itemClickListener;

    public IngredientListEditorAdapter(@NonNull DiffUtil.ItemCallback<Ingredient> diffCallback, IngredientListEditorAdapter.ItemClickListener itemClickListener) {
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

    public static class IngredientDiff extends DiffUtil.ItemCallback<Ingredient> {

        @Override
        public boolean areItemsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            return oldItem == newItem;
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

            //attach click listener to delete icon
            itemView.findViewById(R.id.delete_icon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemClickListener.onDeleteClicked(getAdapterPosition());
                }
            });
        }

        public void bind (Ingredient ingredient){
            ingredientNameView.setText(ingredient.toString());
        }
    }
}
