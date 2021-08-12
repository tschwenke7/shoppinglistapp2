package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.IngListItem;

import java.util.ArrayList;
import java.util.List;


public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {
    private List<IngListItem> items;
    private final SlItemClickListener slItemClickListener;

    public ShoppingListAdapter(SlItemClickListener slItemClickListener){
        this.slItemClickListener = slItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shopping_list_recyclerview_item, parent, false);
        return new ShoppingListAdapter.ViewHolder(view, slItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngListItem current = getItem(position);
        holder.bind(current);
    }

    @Override
    public int getItemCount() {
        if(null != items){
            return items.size();
        }
        return 0;
    }

    public IngListItem getItem(int position){
        if(null != items){
            return items.get(position);
        }
        return null;
    }

    public void setItems(List<IngListItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SlItemDiff(newItems, items));
        diffResult.dispatchUpdatesTo(this);
        if(items != null){
            items.clear();
            if(newItems != null){
                items.addAll(newItems);
            }
        }
        else if(newItems != null){
            items = new ArrayList<>();
            items.addAll(newItems);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View itemView;
        private SlItemClickListener slItemClickListener;

        public ViewHolder(@NonNull View itemView, SlItemClickListener slItemClickListener) {
            super(itemView);
            this.itemView = itemView;
            this.slItemClickListener = slItemClickListener;
        }

        public void bind (IngListItem item){
            TextView textView = (TextView) itemView.findViewById(R.id.item_name);
            View editItemContainer = itemView.findViewById(R.id.edit_item_container);
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
            textView.setOnClickListener(v -> slItemClickListener.onSlItemClick(getAdapterPosition()));

            //set long click listener to enable editing
            textView.setOnLongClickListener(v -> {
                //hide textview, show edittext instead
                textView.setVisibility(View.GONE);
                editItemContainer.setVisibility(View.VISIBLE);
                editText.requestFocus();
                return true;
            });

            //set listener to edit item when edit confirm clicked
            confirmEditItemButton.setOnClickListener(v -> {
                //delegate updating the item to fragment
                slItemClickListener.onSlItemEditConfirm(item, editText.getText().toString());
                //swap editing view back to plain textview
                textView.setVisibility(View.VISIBLE);
                editItemContainer.setVisibility(View.GONE);
            });

        }
    }

    private class SlItemDiff extends DiffUtil.Callback {
        List<IngListItem> newList;
        List<IngListItem> oldList;

        public SlItemDiff(List<IngListItem> newList, List<IngListItem> oldList) {
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
            IngListItem newItem = newList.get(newItemPosition);
            IngListItem oldItem = oldList.get(oldItemPosition);

            boolean boo = newItem.equals(oldItem);

            return boo;
        }
    }

    public interface SlItemClickListener {
        void onSlItemClick(int position);
        void onSlItemEditConfirm(IngListItem oldItem, String newItemString);
    }
}
