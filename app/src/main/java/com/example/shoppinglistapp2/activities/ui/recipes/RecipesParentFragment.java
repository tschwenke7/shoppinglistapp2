package com.example.shoppinglistapp2.activities.ui.recipes;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglistapp2.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecipesParentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecipesParentFragment extends Fragment {


    public RecipesParentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment RecipesParentFragment.
     */
    public static RecipesParentFragment newInstance() {
        RecipesParentFragment fragment = new RecipesParentFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recipes_parent, container, false);
    }
}