package com.example.shoppinglistapp2.activities.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class BaseRecyclerViewAdapter<Item> extends RecyclerView.Adapter<BaseRecyclerViewAdapter<Item>.ViewHolder>{
    protected List<Item> items;
    protected Deque<List<Item>> pendingUpdates = new ArrayDeque<>();
    protected Executor updateListExecutor;
    protected View recyclerView;
    protected View progressBar;

    /** Using this constructor will mean there's no progress bar to show/hide while loading, and
     * all background operations will run on the uiThread. It is recommended to at least specify
     * a listUpdateExecutor using the single-argument constructor.
     */
    public BaseRecyclerViewAdapter() {
    }

    /** Use this constructor if you do not want to associate a progress bar
     * which displays instead of recyclerview while loading.
     * @param listUpdateExecutor the executor to perform diffUtil calculations on
     */
    public BaseRecyclerViewAdapter(Executor listUpdateExecutor) {
        this.updateListExecutor = listUpdateExecutor;
        this.recyclerView = null;
        this.progressBar = null;
    }

    /**
     * Constructor which provides both a background thread executor to run listUpdates on,
     * as well a progressBar. When the list update is complete, the progressBar will have its
     * visibility set to View.GONE, and the recyclerView will be set to View.VISIBLE.
     * @param listUpdateExecutor - the executor to perform diffUtil calculations on
     * @param recyclerView - the recyclerview managed by this adapter, to be shown when list is loaded
     * @param progressBar - the progress bar associated with this recyclerview, which will be hidden
     *                    once the list has been updated.
     */
    public BaseRecyclerViewAdapter(Executor listUpdateExecutor, View recyclerView, View progressBar) {
        this.updateListExecutor = listUpdateExecutor;
        this.recyclerView = recyclerView;
        this.progressBar = progressBar;
    }

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

    public void setItems(List<Item> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(createDiffCallback(newItems, items));
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

    protected void updateListInternal(List<Item> newItems) {
        final List<Item> oldItems;
        if(null != items){
            oldItems = new ArrayList<>(items);
        }
        else{
            oldItems = null;
        }

        final Handler handler = new Handler(Looper.getMainLooper());

        if(updateListExecutor != null) {
            updateListExecutor.execute(() -> {
                final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(createDiffCallback(newItems, oldItems));
                handler.post(() -> applyDiffResult(newItems,diffResult));
                handler.post(this::hideProgressBar);
            });
        }
        //run operations on ui thread if no other executor provided
        else {
            handler.post(() -> {
                final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(createDiffCallback(newItems, oldItems));
                handler.post(() -> applyDiffResult(newItems,diffResult));
                handler.post(this::hideProgressBar);
            });
        }
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

    protected void dispatchUpdates(List<Item> newItems, DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(this);
        if (items == null){
            items = new ArrayList<>();
        }
        else {
            items.clear();
        }

        if(items != null){
            items.addAll(newItems);
        }
    }

    protected void showProgressBar(){
        if (progressBar != null && recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }
    protected void hideProgressBar(){
        if (progressBar != null && recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
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
