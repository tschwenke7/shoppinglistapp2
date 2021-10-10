package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.mainContentFragments.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.mainContentFragments.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewShoppingListItemBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.Collections;
import java.util.List;


public class ShoppingListAdapter extends BaseRecyclerViewAdapter<IngListItem> {
    private final SlItemClickListener slItemClickListener;

    public ShoppingListAdapter(SlItemClickListener slItemClickListener, ListeningExecutorService backgroundExecutor){
        super(backgroundExecutor);
        this.slItemClickListener = slItemClickListener;
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

            //enable editing of item
//            textView.setVisibility(View.GONE);
//            editText.setVisibility(View.VISIBLE);
//            confirmEditItemButton.setVisibility(View.VISIBLE);
//            editText.requestFocus();

            //set long click listener to enable editing
//            textView.setOnLongClickListener(v -> {
//                //hide textview, show edittext instead
//                textView.setVisibility(View.GONE);
//                editItemContainer.setVisibility(View.VISIBLE);
//                editText.requestFocus();
//                return true;
//            });

            //set listener to edit item when edit confirm clicked
            confirmEditItemButton.setOnClickListener(v -> {
                //delegate updating the item to fragment
                slItemClickListener.onSlItemEditConfirm(item, editText.getText().toString());
                //swap editing view back to plain textview
                textView.setVisibility(View.VISIBLE);
                binding.itemMenuButton.setVisibility(View.VISIBLE);
                editText.setVisibility(View.GONE);
                confirmEditItemButton.setVisibility(View.GONE);

                editText.clearFocus();
                //hide keyboard
                InputMethodManager imm = (InputMethodManager)textView.getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            });

        }

        public void setSelected(boolean selected) {
            if(selected) {
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.recipe_item_background_light));
                binding.itemName.setAlpha(0.4f);
                binding.itemContainer.setAlpha(0.4f);
                itemView.setRotation(2f);
            }
            else {
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.recipe_item_background_light));
                binding.itemName.setAlpha(1f);
                binding.itemContainer.setAlpha(1f);
                itemView.setRotation(0f);
            }
        }
    }

    public interface SlItemClickListener {
        void onSlItemClick(int position);
        void onSlItemEditConfirm(IngListItem oldItem, String newItemString);
    }
}
