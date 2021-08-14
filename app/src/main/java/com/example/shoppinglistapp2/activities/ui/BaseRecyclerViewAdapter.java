package com.example.shoppinglistapp2.activities.ui;

import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class BaseRecyclerViewAdapter<Item> extends RecyclerView.Adapter<BaseRecyclerViewAdapter<Item>.ViewHolder>{
    protected List<Item> items;
    protected Deque<List<Item>> pendingUpdates = new ArrayDeque<>();


    @Override
    /**
     * Calls holder.bind(items.get(position))
     */
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        if(null != items){
            return items.size();
        }
        return 0;
    }

    public Item getItem(int position) {
        if (null != items && items.size() > position) {
            return items.get(position);
        }
        return null;
    }

    public void updateList(final List<Item> newItems){
        pendingUpdates.push(newItems);
        //if there's already another update being run, then don't start a new one
        if(pendingUpdates.size() > 1){
            //do nothing
            return;
        }
        //otherwise start this update
        updateListInternal(newItems);
    }

    protected void updateListInternal(List<Item> newItems) {
        final List<Item> oldItems;
        if(null != items){
            oldItems = new ArrayList<>(items);
        }
        else{
            oldItems = null;
        }

        final Handler handler = new Handler();
        new Thread(() -> {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(createDiffCallback(newItems, oldItems));
            handler.post(() -> applyDiffResult(newItems,diffResult));
        });
    }

    protected void applyDiffResult(List<Item> newItems, DiffUtil.DiffResult diffResult){
        //take this update off the queue now that it is being executed
        pendingUpdates.remove(newItems);
        dispatchUpdates(newItems, diffResult);

        //if there are more updates queued now, then take the latest one and run it
        if(pendingUpdates.size() > 0) {
            List<Item> latest = pendingUpdates.pop();

            //remove older, outdated updates from queue
            pendingUpdates.clear();

            //run latest update
            updateListInternal(latest);
        }
    }

    private void dispatchUpdates(List<Item> newItems, DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(this);
        items.clear();
        items.addAll(newItems);
    }

    protected ItemDiff createDiffCallback(List<Item> newList, List<Item> oldList) {
        return new ItemDiff(newList, oldList);
    }

    public class ItemDiff extends DiffUtil.Callback {
        protected List<Item> newList;
        protected List<Item> oldList;

        public ItemDiff(List<Item> newList, List<Item> oldList) {
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
            if (newList == null){
                return 0;
            }
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            //compare Recipe ids
            return oldList.get(oldItemPosition) == newList.get(newItemPosition);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    public abstract class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(Item item);
    }
}