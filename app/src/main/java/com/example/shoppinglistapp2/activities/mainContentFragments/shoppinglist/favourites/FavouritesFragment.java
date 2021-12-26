package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.favourites;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.shoppinglistapp2.App;
import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ContentFragment;
import com.example.shoppinglistapp2.activities.MainActivity;
import com.example.shoppinglistapp2.databinding.FragmentFavouritesBinding;
import com.example.shoppinglistapp2.db.tables.IngListItem;
import com.example.shoppinglistapp2.helpers.ErrorsUI;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Executor;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FavouritesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FavouritesFragment extends ContentFragment implements FavouritesAdapter.ClickListener {
    private static final String TAG = "T_DBG_FAVFRAG";
    private FavouritesViewModel viewModel;
    private FragmentFavouritesBinding binding;

    private FavouritesAdapter adapter;

    public FavouritesFragment() {
        // Required empty public constructor
    }

    public static FavouritesFragment newInstance() {
        return new FavouritesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //get viewModel
        viewModel =
                new ViewModelProvider(requireActivity()).get(FavouritesViewModel.class);

        binding = FragmentFavouritesBinding.inflate(inflater, container, false);

        backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        uiExecutor = ContextCompat.getMainExecutor(requireContext());

        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        adapter = new FavouritesAdapter(this, backgroundExecutor);
        binding.favouritesRecyclerview.setAdapter(adapter);
        binding.favouritesRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        viewModel.getFavourites().observe(getViewLifecycleOwner(), (list) -> {
            if (list.isEmpty()) {
                adapter.submitList(list, () -> adapter.notifyItemChanged(0));
            }
            else{
                adapter.submitList(list);
            }
        });

        //setup action bar
        setHasMenu(true);
        setPageTitle(getString(R.string.title_favourites));
        setShowUpButton(true);

        //handle back pressed
        addDefaultOnBackPressedCallback();

    }

    @Override
    public void onAddAllClicked() {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.addAllUnchecked()),
                new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(@Nullable Object result) {
                        ErrorsUI.showToast(requireContext(), R.string.favourites_added_success);
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                },
                uiExecutor);
        Navigation.findNavController(requireView()).navigateUp();
    }

    @Override
    public void onAddItemToListClicked(IngListItem item, int pos) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.sendToShoppingList(item)),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {
                    adapter.notifyItemChanged(pos);
                }

                @Override
                public void onFailure(Throwable t) {
                    ErrorsUI.showDefaultToast(requireContext());
                }
            }, uiExecutor);
    }

    @Override
    public void onAddNewItemClicked(EditText inputField) {
        String inputText = inputField.getText().toString();
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
                            inputField.setText("");
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            if (t instanceof InterruptedException){
                                Log.e(TAG, "adding items to shoppping list: ", t);
                                Toast.makeText(requireContext(), R.string.error_could_not_access_database, Toast.LENGTH_LONG).show();
                            }
                            else{
                                ErrorsUI.showAlert(requireContext(), R.string.error_could_not_add_items);
                            }
                        }
                    },
                    uiExecutor
            );
        }
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

    @Override
    public void onSlItemEditConfirm(IngListItem item, String newText) {
        Futures.addCallback(backgroundExecutor.submit(() -> viewModel.editItem(item, newText)),
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Navigation.findNavController(requireView()).navigateUp();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}