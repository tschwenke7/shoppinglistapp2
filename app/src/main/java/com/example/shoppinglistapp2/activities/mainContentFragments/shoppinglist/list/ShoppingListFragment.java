package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.list;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.databinding.FragmentShoppingListBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.Animations;
import com.example.shoppinglistapp2.helpers.ErrorsUI;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Executor;

public class ShoppingListFragment extends Fragment implements ShoppingListAdapter.SlItemClickListener {
    private ShoppingListViewModel viewModel;
    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;
    private FragmentShoppingListBinding binding;

    private final String TAG = "T_DBG_SL_FRAG";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //get viewModel
        viewModel =
                new ViewModelProvider(requireActivity()).get(ShoppingListViewModel.class);

        //inflate fragment
        binding = FragmentShoppingListBinding.inflate(inflater, container, false);

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        //setup action bar
        this.setHasOptionsMenu(true);

        //setup shopping list recyclerview
        final ShoppingListAdapter adapter = new ShoppingListAdapter(this, backgroundExecutor);
        binding.shoppingListRecyclerview.setAdapter(adapter);
        binding.shoppingListRecyclerview.setLayoutManager(new LinearLayoutManager(this.getContext()));
        ShoppingListTouchCallback itemTouchCallback = new ShoppingListTouchCallback(adapter, requireContext());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
        itemTouchHelper.attachToRecyclerView(binding.shoppingListRecyclerview);

        //set observer to update shopping list when it changes
        viewModel.getSlItems().observe(getViewLifecycleOwner(), items -> {
            //if the list is empty, show placeholder text instead
            if(items == null || items.size() == 0){
                binding.textviewNoSlItems.setVisibility(View.VISIBLE);
                binding.shoppingListRecyclerview.setVisibility(View.GONE);
                binding.shoppingListProgressBar.setVisibility(View.GONE);
            }
            else{
                //hide placeholder text
                binding.textviewNoSlItems.setVisibility(View.GONE);


                Parcelable recyclerViewState = binding.shoppingListRecyclerview.getLayoutManager().onSaveInstanceState();
                adapter.submitList(items, () -> {
                    binding.shoppingListRecyclerview.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                    if (binding.shoppingListProgressBar.getVisibility() == View.VISIBLE) {
                        Animations.fadeSwap(binding.shoppingListProgressBar, binding.shoppingListRecyclerview);
                    }
                    else {
                        binding.shoppingListRecyclerview.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        //listen to add item button
        binding.buttonNewListItem.setOnClickListener(this::addItems);
    }

    private void addItems(View view){
        String inputText = binding.editTextNewListItem.getText().toString();

        if(inputText.isEmpty()){
            Toast.makeText(this.getContext(), R.string.error_no_list_item_entered, Toast.LENGTH_LONG).show();
        }
        else{
            Futures.addCallback(
                backgroundExecutor.submit(() -> viewModel.addItems(inputText)),
                new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(@Nullable Object result) {
                        //clear input box
                        binding.editTextNewListItem.setText("");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (t instanceof InterruptedException){
                            Log.e(TAG, "adding items to shoppping list: ", t);
                            Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                        }
                        else{
                            Log.e(TAG, "adding items to shoppping list: ", t);
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.error_title)
                                    .setMessage(R.string.error_could_not_add_items)
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }
                    }
                },
                uiExecutor
            );
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        super.onCreateOptionsMenu(menu,inflater);
//        menu.clear();
//        inflater.inflate(R.menu.shopping_list_action_bar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_clear_checked_list_items:
                viewModel.deleteCheckedSlItems();
                break;
            case R.id.action_clear_all_list_items:
                //prompt for confirmation first
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.clear_list_warning_title)
                        .setMessage(R.string.clear_list_warning_message)
                        .setPositiveButton(R.string.clear_list_positive_button, (dialogInterface, i) -> {
                            //delete all items from the shopping list
                            viewModel.clearShoppingList();
                        })
                        //otherwise don't do anything
                        .setNegativeButton(R.string.clear_list_negative_button, null)
                        .show();
                break;
            case R.id.action_copy_list_to_clipboard:
                //ask whether to include crossed off items
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.copy_to_clipboard_dialog_title)
                        .setMessage(R.string.copy_to_clipboard_dialog_message)
                        //include crossed off items
                        .setPositiveButton(R.string.copy_to_clipboard_dialog_positive_button, (dialogInterface, i) -> {
                            ClipboardManager clipboard = (ClipboardManager) requireActivity()
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("shopping_list",
                                    viewModel.getAllItemsAsString(true));
                            clipboard.setPrimaryClip(clip);
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.copy_to_clipboard_dialog_title)
                                    .setMessage(R.string.share_shopping_list_success)
                                    .setPositiveButton(R.string.ok, null).show();
                        })
                        //negative button corresponds to "don't include"
                        .setNegativeButton(R.string.copy_to_clipboard_dialog_negative_button, ((dialog, which) -> {
                            ClipboardManager clipboard = (ClipboardManager) requireActivity()
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("shopping_list",
                                    viewModel.getAllItemsAsString(false));
                            clipboard.setPrimaryClip(clip);
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.copy_to_clipboard_dialog_title)
                                    .setMessage(R.string.share_shopping_list_success)
                                    .setPositiveButton(R.string.ok, null).show();
                        }))
                        .show();
                break;
            case R.id.action_open_favourites:
                //reset checked status of list when the fragment is reentered
                viewModel.resetFavouritesAdded();
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_shoppingListFragment_to_favouritesFragment);
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onResume(){
        super.onResume();

        //hide back button
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.hideUpButton();

        //set title
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.title_shopping_list);
    }

    @Override
    public void onSlItemClick(int position) {
        Futures.addCallback(
            backgroundExecutor.submit(() -> viewModel.toggleChecked(position)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {
                    //do nothing
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "while crossing off item: ", t);
                    Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                }
            },
            uiExecutor
        );
    }

    @Override
    public void onSlItemEditConfirm(IngListItem oldItem, String newItemString) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.editItem(oldItem, newItemString)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {

                }

                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof InterruptedException){
                        Log.e(TAG, "adding items to shoppping list: ", t);
                        Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                    }
                    else{
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.error_title)
                                .setMessage(R.string.error_could_not_add_items)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                }
            },
            uiExecutor
        );
    }

    @Override
    public void onSlItemDeleteClicked(IngListItem item) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.deleteItem(item)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {

                }

                @Override
                public void onFailure(Throwable t) {
                    ErrorsUI.showDefaultToast(requireContext());
                }
            }, uiExecutor);
    }

    public class ShoppingListTouchCallback extends ItemTouchHelper.SimpleCallback {
        private final ShoppingListAdapter adapter;

        public ShoppingListTouchCallback(ShoppingListAdapter adapter, Context context) {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END ,0);
            this.adapter = adapter;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPos = viewHolder.getAdapterPosition();
            int toPos = target.getAdapterPosition();

            //prevent extra scrolling if moving top item
            LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int firstPos = manager.findFirstCompletelyVisibleItemPosition();
            int offsetTop = 0;
            if(firstPos >= 0) {
                View firstView = manager.findViewByPosition(firstPos);
                offsetTop = manager.getDecoratedTop(firstView) - manager.getTopDecorationHeight(firstView);
            }

            adapter.swap(fromPos,toPos);

            if (firstPos >= 0) {
                manager.scrollToPositionWithOffset(firstPos, offsetTop);
            }

            return false;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return super.getMovementFlags(recyclerView,viewHolder);
        }

        /**
         * Triggers when drag is stopped. Here we will update the db backing of this recyclerview
         * @param recyclerView
         * @param viewHolder
         */
        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            viewModel.updateListPositions(adapter.getCurrentList());
            ((ShoppingListAdapter.ViewHolder) viewHolder).setSelected(false);
            super.clearView(recyclerView, viewHolder);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            switch (actionState) {
                case ItemTouchHelper.ACTION_STATE_DRAG:
                    ((ShoppingListAdapter.ViewHolder) viewHolder).setSelected(true);
                    break;
                case ItemTouchHelper.ACTION_STATE_IDLE:
                    break;
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            final int direction = (int) Math.signum(viewSizeOutOfBounds);
            return 12 * direction;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
            return current.getItemViewType() == target.getItemViewType();
        }
    }
}