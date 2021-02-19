package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.SlItem;

import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {
    private List<SlItem> items;
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
        SlItem current = getItem(position);
        holder.bind(current);
    }

    @Override
    public int getItemCount() {
        if(null != items){
            return items.size();
        }
        return 0;
    }

    public SlItem getItem(int position){
        if(null != items){
            return items.get(position);
        }
        return null;
    }

    public void setItems(List<SlItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SlItemDiff(newItems, items));
        diffResult.dispatchUpdatesTo(this);
        this.items = newItems;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private View itemView;
        private SlItemClickListener slItemClickListener;

        public ViewHolder(@NonNull View itemView, SlItemClickListener slItemClickListener) {
            super(itemView);
            this.itemView = itemView;
            this.slItemClickListener = slItemClickListener;
            itemView.setOnClickListener(this);
        }

        public void bind (SlItem item){
            TextView textView = (TextView) itemView.findViewById(R.id.item_name);
            //set text to contents of slItem
            textView.setText(item.toString());

            //if checked, then cross out and fade item
            if(item.isChecked()){
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textView.setTextColor(0xffd3d3d3);
            }
            else{
                textView.setPaintFlags(textView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                textView.setTextColor(0xff000000);//todo - use color resources instead
            }
        }

        @Override
        public void onClick(View view) {
            slItemClickListener.onSlItemClick(getAdapterPosition());
        }
    }

    private class SlItemDiff extends DiffUtil.Callback {
        List<SlItem> newList;
        List<SlItem> oldList;

        public SlItemDiff(List<SlItem> newList, List<SlItem> oldList) {
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
            SlItem newItem = newList.get(newItemPosition);
            SlItem oldItem = oldList.get(oldItemPosition);

            return
                newItem.isChecked() == oldItem.isChecked()
                && newItem.getQty1() == oldItem.getQty1()
                && newItem.getQty2() == (oldItem.getQty2())
                && newItem.getName().equals(oldItem.getName());
        }
    }

    public interface SlItemClickListener {
        void onSlItemClick(int position);
    }
}