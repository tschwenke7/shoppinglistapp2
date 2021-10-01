package com.example.shoppinglistapp2.activities.ui.shoppinglist;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.databinding.FragmentShoppingListBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.IngListItemUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class ShoppingListFragment extends Fragment implements ShoppingListAdapter.SlItemClickListener {
    private ShoppingListViewModel shoppingListViewModel;
    private ListeningExecutorService backgroundExecutor;
    private Executor uiExecutor;
    private FragmentShoppingListBinding binding;

    private final String TAG = "T_DBG_SL_FRAG";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //get viewModel
        shoppingListViewModel =
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
        final ShoppingListAdapter adapter = new ShoppingListAdapter(this);
        binding.shoppingListRecyclerview.setAdapter(adapter);
        binding.shoppingListRecyclerview.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update shopping list when it changes
        shoppingListViewModel.getSlItems().observe(getViewLifecycleOwner(), items -> {
            //if the list is empty, show placeholder text instead
            if(items == null || items.size() == 0){
                binding.textviewNoSlItems.setVisibility(View.VISIBLE);
                binding.shoppingListRecyclerview.setVisibility(View.GONE);
            }
            else{
                //hide placeholder text
                binding.textviewNoSlItems.setVisibility(View.GONE);
                binding.shoppingListRecyclerview.setVisibility(View.VISIBLE);

                Parcelable recyclerViewState = binding.shoppingListRecyclerview.getLayoutManager().onSaveInstanceState();
                adapter.submitList(items, () -> {
                    binding.shoppingListRecyclerview.getLayoutManager().onRestoreInstanceState(recyclerViewState);
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
                backgroundExecutor.submit(() -> shoppingListViewModel.addItems(inputText)),
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
                shoppingListViewModel.deleteCheckedSlItems();
                break;
            case R.id.action_clear_all_list_items:
                //prompt for confirmation first
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.clear_list_warning_title)
                        .setMessage(R.string.clear_list_warning_message)
                        .setPositiveButton(R.string.clear_list_positive_button, (dialogInterface, i) -> {
                            //delete all items from the shopping list
                            shoppingListViewModel.clearShoppingList();
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
                                    shoppingListViewModel.getAllItemsAsString(true));
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(requireContext(), R.string.export_ingredients_toast, Toast.LENGTH_LONG).show();
                        })
                        //negative button corresponds to "don't include"
                        .setNegativeButton(R.string.copy_to_clipboard_dialog_negative_button, ((dialog, which) -> {
                            ClipboardManager clipboard = (ClipboardManager) requireActivity()
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("shopping_list",
                                    shoppingListViewModel.getAllItemsAsString(false));
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(requireContext(), R.string.export_ingredients_toast, Toast.LENGTH_LONG).show();
                        }))
                        .show();
                break;
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
            backgroundExecutor.submit(() -> shoppingListViewModel.toggleChecked(position)),
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
        Futures.addCallback(backgroundExecutor.submit(() -> shoppingListViewModel.editItem(oldItem, newItemString)),
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
}