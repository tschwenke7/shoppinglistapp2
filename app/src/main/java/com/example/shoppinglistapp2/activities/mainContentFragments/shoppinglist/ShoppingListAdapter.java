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
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.IngListItem;


public class ShoppingListAdapter extends ListAdapter<IngListItem, ShoppingListAdapter.ViewHolder> {
    private final SlItemClickListener slItemClickListener;

    public ShoppingListAdapter(SlItemClickListener slItemClickListener){
        super(new IngListItem.DiffCallback());

        this.slItemClickListener = slItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_shopping_list_item, parent, false);
        return new ShoppingListAdapter.ViewHolder(view, slItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngListItem current = getItem(position);
        holder.bind(current);
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

                editItemContainer.clearFocus();
                //hide keyboard
                InputMethodManager imm = (InputMethodManager)textView.getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            });

        }
    }

    public interface SlItemClickListener {
        void onSlItemClick(int position);
        void onSlItemEditConfirm(IngListItem oldItem, String newItemString);
    }
}
