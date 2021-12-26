package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.list;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewShoppingListItemBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.KeyboardHelper;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.Collections;
import java.util.List;


public class ShoppingListAdapter extends BaseRecyclerViewAdapter<IngListItem> {
    private final SlItemClickListener slItemClickListener;
    private final boolean deleteEnabled;

    public ShoppingListAdapter(SlItemClickListener slItemClickListener, ListeningExecutorService backgroundExecutor, boolean deleteEnabled){
        super(backgroundExecutor);
        this.slItemClickListener = slItemClickListener;
        this.deleteEnabled = deleteEnabled;
    }

    public ShoppingListAdapter(SlItemClickListener slItemClickListener, ListeningExecutorService backgroundExecutor){
        super(backgroundExecutor);
        this.slItemClickListener = slItemClickListener;
        this.deleteEnabled = true;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).isChecked()) {
            return 2;
        }
        else {
            return 1;
        }
    }

    @NonNull
    @Override
    public BaseRecyclerViewAdapter<IngListItem>.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ShoppingListAdapter.ViewHolder(RecyclerviewShoppingListItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), slItemClickListener);
    }

    @Override
    protected BaseDiffCallback<IngListItem> createDiffCallback(List<IngListItem> newList, List<IngListItem> oldList) {
        return new BaseDiffCallback<IngListItem>(newList, oldList) {
            @Override
            public boolean areItemsTheSame(IngListItem oldItem, IngListItem newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(IngListItem oldItem, IngListItem newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    public void swap(int fromPos, int toPos) {
        Collections.swap(getCurrentList(), fromPos, toPos);
        notifyItemMoved(fromPos, toPos);
    }


    public class ViewHolder extends BaseRecyclerViewAdapter<IngListItem>.ViewHolder {
        private SlItemClickListener slItemClickListener;
        private RecyclerviewShoppingListItemBinding binding;

        public ViewHolder(RecyclerviewShoppingListItemBinding binding, SlItemClickListener slItemClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.slItemClickListener = slItemClickListener;
        }

        public void bind (IngListItem item){
            TextView textView = (TextView) itemView.findViewById(R.id.item_name);
            EditText editText = itemView.findViewById(R.id.edit_text_item_name);
            View confirmEditItemButton = itemView.findViewById(R.id.confirm_edit_item_button);
            //set text to contents of slItem
            textView.setText(item.toString());
            editText.setText(item.toString());

            //if checked, then cross out and fade item
            if(item.isChecked()){
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textView.setTextColor(0xffd3d3d3);
            }
            else{
                textView.setPaintFlags(textView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                textView.setTextColor(0xff000000);//todo - use color resources instead
            }

            //set click listener to toggle this item checked/unchecked from list
            textView.setOnClickListener(v ->
            {
                slItemClickListener.onSlItemClick(getAdapterPosition());
            });

            binding.itemMenuButton.setOnClickListener((v) -> {
                PopupMenu popup = new PopupMenu(binding.itemMenuButton.getContext(), binding.itemMenuButton);
                popup.inflate(R.menu.shopping_list_item_context_menu);
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.card_background_default));
                if(!deleteEnabled) {
                    popup.getMenu().removeItem(R.id.delete_item_action);
                }

                popup.setOnMenuItemClickListener((menuItem) -> {
                    switch (menuItem.getItemId()) {
                        case R.id.edit_item_action:
                            //switch out default  views for editing views
                            textView.setVisibility(View.GONE);
                            binding.itemMenuButton.setVisibility(View.GONE);
                            editText.setVisibility(View.VISIBLE);
                            confirmEditItemButton.setVisibility(View.VISIBLE);
                            editText.requestFocus();

                            //open keyboard
                            KeyboardHelper.showKeyboard(editText);

                            return true;
                        case R.id.delete_item_action:
                            slItemClickListener.onSlItemDeleteClicked(getItem(getAdapterPosition()));
                        default:
                            return false;
                    }
                });
                popup.setOnDismissListener((d) -> {
                    itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.recipe_item_background_light));
                });
                popup.show();
            });

            //set listener to edit item when edit confirm clicked
            confirmEditItemButton.setOnClickListener(v -> {
                //delegate updating the item to fragment
                slItemClickListener.onSlItemEditConfirm(item, editText.getText().toString());

                //swap editing view back to plain textview
                textView.setText(editText.getText());
                textView.setVisibility(View.VISIBLE);
                binding.itemMenuButton.setVisibility(View.VISIBLE);
                editText.setVisibility(View.GONE);
                confirmEditItemButton.setVisibility(View.GONE);

                //hide keyboard
                editText.clearFocus();
                KeyboardHelper.hideKeyboard(editText);
            });

            itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.recipe_item_background_light));

        }

        public void setSelected(boolean selected) {
            if(selected) {

                binding.itemName.setAlpha(0.4f);
                binding.itemContainer.setAlpha(0.4f);
                itemView.setRotation(2f);
            }
            else {
                binding.itemName.setAlpha(1f);
                binding.itemContainer.setAlpha(1f);
                itemView.setRotation(0f);
            }
        }
    }

    public interface SlItemClickListener {
        void onSlItemClick(int position);
        void onSlItemEditConfirm(IngListItem oldItem, String newItemString);
        void onSlItemDeleteClicked(IngListItem item);
    }
}
