package com.example.shoppinglistapp2.activities;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.shoppinglistapp2.App;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Executor;

public class ContentFragment extends Fragment {
    protected Executor uiExecutor;
    protected ListeningExecutorService backgroundExecutor;
    private String pageTitle;
    private boolean hasMenu = true;
    private boolean showUpButton = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.backgroundExecutor = ((App) requireActivity().getApplication()).backgroundExecutorService;
        this.uiExecutor = ContextCompat.getMainExecutor(requireContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        //show or hide back button
        MainActivity mainActivity = (MainActivity) requireActivity();
        if(showUpButton) {
            mainActivity.showUpButton();
        }
        else {
            mainActivity.hideUpButton();
        }

        setHasOptionsMenu(hasMenu);
        mainActivity.invalidateOptionsMenu();

        //set title
        mainActivity.getSupportActionBar().setTitle(pageTitle);
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(pageTitle);
    }
    public void setPageTitleWithoutUpdating(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public boolean getHasMenu() {
        return hasMenu;
    }

    public void setHasMenu(boolean hasMenu) {
        this.hasMenu = hasMenu;
    }

    public boolean isHasMenu() {
        return hasMenu;
    }

    public boolean isShowUpButton() {
        return showUpButton;
    }

    public void setShowUpButton(boolean showUpButton) {
        this.showUpButton = showUpButton;
        MainActivity mainActivity = (MainActivity) requireActivity();
        if(showUpButton) {
            mainActivity.showUpButton();
        }
        else {
            mainActivity.hideUpButton();
        }
    }

    public void addOnBackPressedCallback(Runnable onBackPressedCallback) {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedCallback.run();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    public void addDefaultOnBackPressedCallback() {
        addOnBackPressedCallback(() -> {
            Fragment f1 = this;
            NavHostFragment.findNavController(f1).navigateUp();
        });
    }
}


