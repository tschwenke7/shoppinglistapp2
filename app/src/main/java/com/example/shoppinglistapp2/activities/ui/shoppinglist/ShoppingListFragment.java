package com.example.shoppinglistapp2.activities.ui.shoppinglist;

import android.content.DialogInterface;
import android.os.Bundle;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shoppinglistapp2.R;

public class ShoppingListFragment extends Fragment implements ShoppingListAdapter.SlItemClickListener {
    private ShoppingListViewModel shoppingListViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //get viewModel
        shoppingListViewModel =
                new ViewModelProvider(getActivity()).get(ShoppingListViewModel.class);

        //inflate fragment
        View root = inflater.inflate(R.layout.fragment_shopping_list, container, false);

        //setup action bar
        this.setHasOptionsMenu(true);

        //setup shopping list recyclerview
        RecyclerView shoppingListRecyclerView = root.findViewById(R.id.shopping_list_recyclerview);
        final ShoppingListAdapter adapter = new ShoppingListAdapter(this);
        shoppingListRecyclerView.setAdapter(adapter);
        shoppingListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        //set observer to update shopping list when it changes
        shoppingListViewModel.getAllItems().observe(getViewLifecycleOwner(), slItems -> {
            adapter.setItems(slItems);
        });

        //listen to add item button
        ((Button) root.findViewById(R.id.button_new_list_item)).setOnClickListener(view -> {
            addItems(view);
        });


        return root;
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
        inflater.inflate(R.menu.shopping_list_action_bar, menu);
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
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onSlItemClick(int position) {
        shoppingListViewModel.toggleChecked(position);
    }
}