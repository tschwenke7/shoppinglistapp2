package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.db.tables.SlItem;

public class ShoppingListFragment extends Fragment implements ShoppingListAdapter.SlItemClickListener {
    private ShoppingListViewModel shoppingListViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //get viewModel
        shoppingListViewModel =
                new ViewModelProvider(getActivity()).get(ShoppingListViewModel.class);

        //inflate fragment
        View root = inflater.inflate(R.layout.fragment_shopping_list, container, false);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        //setup action bar
        this.setHasOptionsMenu(true);

        //setup shopping list recyclerview
        RecyclerView shoppingListRecyclerView = root.findViewById(R.id.shopping_list_recyclerview);
        final ShoppingListAdapter adapter = new ShoppingListAdapter(this);
        shoppingListRecyclerView.setAdapter(adapter);
        shoppingListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update shopping list when it changes
        shoppingListViewModel.getAllItems().observe(getViewLifecycleOwner(), slItems -> {
            Parcelable recyclerViewState = shoppingListRecyclerView.getLayoutManager().onSaveInstanceState();
            adapter.setItems(slItems);
            shoppingListRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        });

        //listen to add item button
        ((Button) root.findViewById(R.id.button_new_list_item)).setOnClickListener(view -> {
            addItems(view);
        });
    }

    private void addItems(View view){
        EditText input = view.getRootView().findViewById(R.id.edit_text_new_list_item);
        String inputText = input.getText().toString();

        if(!inputText.isEmpty()){
            shoppingListViewModel.addItems(inputText);
        }
        else{
            Toast.makeText(this.getContext(), getContext().getString(R.string.error_no_list_item_entered), Toast.LENGTH_LONG).show();
        }

        //clear input box
        input.setText("");
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
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.clear_list_warning_title)
                        .setMessage(R.string.clear_list_warning_message)
                        .setPositiveButton(R.string.clear_list_positive_button, (dialogInterface, i) -> {
                            //delete all items from the shopping list
                            shoppingListViewModel.deleteAllSlItems();
                        })
                        //otherwise don't do anything
                        .setNegativeButton(R.string.clear_list_negative_button, null)
                        .show();
                break;
            case R.id.action_copy_list_to_clipboard:
                ClipboardManager clipboard = (ClipboardManager) requireActivity()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("shopping_list",
                        shoppingListViewModel.getAllItemsAsString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), R.string.toast_copied_list_to_clipboard, Toast.LENGTH_LONG).show();
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_shopping_list);
    }

    @Override
    public void onSlItemClick(int position) {
        shoppingListViewModel.toggleChecked(position);
    }

    @Override
    public void onSlItemEditConfirm(SlItem oldItem, String newItemString) {
        shoppingListViewModel.editItem(oldItem, newItemString);
    }
}