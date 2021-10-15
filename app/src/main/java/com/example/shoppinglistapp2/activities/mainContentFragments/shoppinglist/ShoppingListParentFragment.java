package com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglistapp2.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShoppingListParentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShoppingListParentFragment extends Fragment {


    public ShoppingListParentFragment() {
        // Required empty public constructor
    }

    public static ShoppingListParentFragment newInstance() {
        ShoppingListParentFragment fragment = new ShoppingListParentFragment();
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
        return inflater.inflate(R.layout.fragment_shopping_list_parent, container, false);
    }
}