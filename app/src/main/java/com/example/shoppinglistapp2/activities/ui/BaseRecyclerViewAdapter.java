package com.example.shoppinglistapp2.activities.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class BaseRecyclerViewAdapter<T> extends RecyclerView.Adapter<BaseRecyclerViewAdapter<T>.ViewHolder>{
    protected List<T> items;
    protected Deque<ListAndCallback<T>> pendingUpdates = new ArrayDeque<>();
    protected Executor updateListExecutor;

    /** Using this constructor will mean
     * all background operations will run on the uiThread. It is recommended to specify
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

    public T getItem(int position) {
        if (null != items && items.size() > position) {
            return items.get(position);
        }
        return null;
    }

    public List<T> getCurrentList(){
        return items;
    }

    public void submitList(final List<T> newItems){
        //otherwise start this update
        submitList(newItems, null);
    }

    public void submitList(final List<T> newItems, @Nullable Runnable callback){
        ListAndCallback<T> current = new ListAndCallback<>(newItems, callback);
        pendingUpdates.push(current);
        //if there's already another update being run, then don't start a new one
        if(pendingUpdates.size() > 1){
            //do nothing
            return;
        }
        //otherwise start this update
        submitListInternal(current);
    }

    protected void submitListInternal(ListAndCallback<T> listAndCallback) {
        final List<T> oldItems;
        if(null != items){
            oldItems = new ArrayList<>(items);
        }
        else{
            oldItems = null;
        }

        final Handler handler = new Handler(Looper.getMainLooper());

        if(updateListExecutor != null) {
            updateListExecutor.execute(() -> {
                final DiffUtil.DiffResult diffResult =
                        DiffUtil.calculateDiff(createDiffCallback(listAndCallback.list, oldItems));
                handler.post(() -> applyDiffResult(listAndCallback, diffResult));
            });
        }
        //run operations on ui thread if no other executor provided
        else {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(createDiffCallback(listAndCallback.list, oldItems));
            handler.post(() -> applyDiffResult(listAndCallback, diffResult));
        }
    }

    protected void applyDiffResult(ListAndCallback<T> current, DiffUtil.DiffResult diffResult){
        //take this update off the queue now that it is being executed
        pendingUpdates.remove(current);
        dispatchUpdates(current.list, diffResult);
        if(current.callback != null) {
            current.callback.run();
        }

        //if there are more updates queued now, then take the latest one and run it
        if(pendingUpdates.size() > 0) {
            ListAndCallback<T> latest = pendingUpdates.pop();

            //remove older, outdated updates from queue
            pendingUpdates.clear();

            //run latest update
            submitListInternal(latest);
        }
    }

    protected void dispatchUpdates(List<T> newItems, DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(this);
        if (items == null){
            items = new ArrayList<>();
        }
        else {
            items.clear();
        }

        if(newItems != null){
            items.addAll(newItems);
        }
    }

    public void updateListSync(List<T> newItems) {
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

    protected abstract BaseDiffCallback<T> createDiffCallback(List<T> newList, List<T> oldList);

    public abstract class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(T item);
    }

    private static class ListAndCallback<T> {
        public List<T> list;
        public Runnable callback;

        public ListAndCallback(List<T> list, Runnable callback) {
            this.list = list;
            this.callback = callback;
        }
    }
}
