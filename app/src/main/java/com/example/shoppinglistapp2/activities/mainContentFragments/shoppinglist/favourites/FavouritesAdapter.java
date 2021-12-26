package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.favourites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.BaseDiffCallback;
import com.example.shoppinglistapp2.activities.BaseRecyclerViewAdapter;
import com.example.shoppinglistapp2.databinding.RecyclerviewFavouritesHeaderBinding;
import com.example.shoppinglistapp2.databinding.RecyclerviewFavouritesItemBinding;
import com.example.shoppinglistapp2.databinding.RecyclerviewShoppingListItemBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.KeyboardHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class FavouritesAdapter extends BaseRecyclerViewAdapter<IngListItem> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final ClickListener clickListener;

    public FavouritesAdapter(ClickListener clickListener, Executor listUpdateExecutor) {
        super(listUpdateExecutor);
        this.clickListener = clickListener;
    }

    @Override
    protected BaseDiffCallback<IngListItem> createDiffCallback(List<IngListItem> newList, List<IngListItem> oldList) {
        return new BaseDiffCallback<IngListItem>(newList, oldList) {
            @Override
            public boolean areItemsTheSame(IngListItem oldItem, IngListItem newItem) {
                return  (oldItem == newItem || oldItem.getId() == newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(IngListItem oldItem, IngListItem newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    @Override
    public void submitList(List<IngListItem> newItems, @Nullable Runnable callback) {
        //prepend an extra, empty item to the list to account for the header viewholder
        List<IngListItem> listWithHeader = new ArrayList<>();
        listWithHeader.add(new IngListItem());
        listWithHeader.addAll(newItems);

        super.submitList(listWithHeader, callback);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(RecyclerviewFavouritesHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false), clickListener);
            case TYPE_ITEM:
            default:
                return new ItemViewHolder(RecyclerviewFavouritesItemBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false), clickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            holder.bind(null);
        }
        else {
            holder.bind(getItem(position));
        }
    }

    public class HeaderViewHolder extends BaseRecyclerViewAdapter<IngListItem>.ViewHolder {
        private final RecyclerviewFavouritesHeaderBinding binding;
        private final ClickListener clickListener;
        private boolean editorOpened = false;

        public HeaderViewHolder(RecyclerviewFavouritesHeaderBinding binding, ClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
        }

        @Override
        public void bind(IngListItem item) {
            binding.buttonAddAll.setOnClickListener((v) -> clickListener.onAddAllClicked());

            binding.buttonNewListItem.setOnClickListener((v) -> {
                //open editor on first click
                if (!editorOpened) {
                    editorOpened = true;

                    //shrink button to make room for editText
                    binding.buttonNewListItem.setText(R.string.new_list_item_button);
                    ViewGroup.LayoutParams params = binding.buttonNewListItem.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    binding.buttonNewListItem.setLayoutParams(params);

                    //show editText
                    binding.editTextNewListItem.setVisibility(View.VISIBLE);
                }
                else {
                    clickListener.onAddNewItemClicked(binding.editTextNewListItem);
                }
            });

            if (getItemCount() == 1) {
                binding.textviewFavouritesHint.setVisibility(View.VISIBLE);
            }
            else {
                binding.textviewFavouritesHint.setVisibility(View.GONE);
            }
        }
    }

    public class ItemViewHolder extends BaseRecyclerViewAdapter<IngListItem>.ViewHolder {
        private final ClickListener clickListener;
        private final RecyclerviewFavouritesItemBinding binding;

        public ItemViewHolder(RecyclerviewFavouritesItemBinding binding, ClickListener clickListener) {
            super(binding.getRoot());
            this.clickListener = clickListener;
            this.binding = binding;
        }

        @Override
        public void bind(IngListItem item) {
            //if checked = true, the item has been added to the list and so should look different
            if (item.isChecked()) {
                binding.addItemButton.setVisibility(View.INVISIBLE);
                binding.itemAddedTick.setVisibility(View.VISIBLE);
                binding.listItem.itemName.setEnabled(false);
            }

            //listen for individual add to list button
            binding.addItemButton.setOnClickListener((v) -> {
                clickListener.onAddItemToListClicked(item, getAdapterPosition());
            });


            RecyclerviewShoppingListItemBinding itemBinding = binding.listItem;
            //set ingredient text
            itemBinding.itemName.setText(item.toString());
            itemBinding.editTextItemName.setText(item.toString());

            //listen for context menu
            binding.listItem.itemMenuButton.setOnClickListener((v) -> {
                PopupMenu popup = new PopupMenu(itemBinding.itemMenuButton.getContext(), itemBinding.itemMenuButton);
                popup.inflate(R.menu.shopping_list_item_context_menu);
                itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.card_background_default));

                popup.setOnMenuItemClickListener((menuItem) -> {
                    switch (menuItem.getItemId()) {
                        case R.id.edit_item_action:
                            //switch out default  views for editing views
                            itemBinding.itemName.setVisibility(View.GONE);
                            itemBinding.itemMenuButton.setVisibility(View.GONE);
                            itemBinding.editTextItemName.setVisibility(View.VISIBLE);
                            itemBinding.confirmEditItemButton.setVisibility(View.VISIBLE);
                            itemBinding.editTextItemName.requestFocus();

                            //open keyboard
                            KeyboardHelper.showKeyboard(itemBinding.editTextItemName);

                            return true;
                        case R.id.delete_item_action:
                            clickListener.onSlItemDeleteClicked(item);
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
            itemBinding.confirmEditItemButton.setOnClickListener(v -> {
                //delegate updating the item to fragment
                clickListener.onSlItemEditConfirm(item, itemBinding.editTextItemName.getText().toString());

                //swap editing view back to plain textview
                itemBinding.itemName.setText(itemBinding.editTextItemName.getText());
                itemBinding.itemName.setVisibility(View.VISIBLE);
                itemBinding.itemMenuButton.setVisibility(View.VISIBLE);
                itemBinding.editTextItemName.setVisibility(View.GONE);
                itemBinding.confirmEditItemButton.setVisibility(View.GONE);

                //hide keyboard
                itemBinding.editTextItemName.clearFocus();
                KeyboardHelper.hideKeyboard(itemBinding.editTextItemName);
            });
        }
    }

    public interface ClickListener {
        void onAddAllClicked();
        void onAddItemToListClicked(IngListItem item, int pos);
        void onAddNewItemClicked(EditText inputField);
        void onSlItemDeleteClicked(IngListItem item);
        void onSlItemEditConfirm(IngListItem item, String newText);
    }
}
